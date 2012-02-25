package org.sade.lab

import org.sade.analyzers.math.FFT
import plot.{Plot3DPanel}
import swing._
import swing.TabbedPane.Page
import org.sade.binding.{BindTriggerPlot3DPanel, BindTriggerPlot2DPanel, BindField, BindDecimalField}
import java.util.ArrayList
import scala.collection.JavaConversions._
import org.sade.analyzers._
import scala.math
import java.io.{FileInputStream, File}
import javax.swing.filechooser.FileNameExtensionFilter
import org.apache.commons.math.complex.Complex


object AnalyzerResearchMain extends SimpleSwingApplication with NimbusLookAndFeel {

  object RangeModel {

    object omegaFrom extends BindField[Double]

    object omegaTo extends BindField[Double]

    object deltaFrom extends BindField[Double]

    object deltaTo extends BindField[Double]

    object phiFrom extends BindField[Double]

    object phiTo extends BindField[Double]

    object sampleFile extends BindField[File]

  }

  val fromFields = Seq(RangeModel.omegaFrom, RangeModel.deltaFrom, RangeModel.phiFrom, RangeModel.sampleFile)

  def optimizerComparisonPlotPanel = {
    new BorderPanel {
      add (new BindTriggerPlot2DPanel(fromFields :_*) {
        setAxisLabels("Step", "S")
        def updatePlots() {
          val params = new MinimizeParameters(Omega.fromOrDefault, Delta.fromOrDefault, Phi.fromOrDefault)
          val evaluator = plotEvaluator
          val results = Seq(
            "Bisection" -> bisectionOptimizerTrackError(params),
            "Gradient" -> gradientDescentTrackError(params, new GradientDescentOptimizer(JacobiAngerAnalyzer.Precision, evaluator)),
            "Fastest gradient" -> gradientDescentTrackError(params, new FastestGradientDescentOptimizer(JacobiAngerAnalyzer.Precision, evaluator)))
          results.foreach {case (label, (track, _)) => addLinePlot(label, track)}
          resultsParamsPanel.updateResults(results)
        }
      }, BorderPanel.Position.Center)
      val resultsParamsPanel = new BoxPanel(Orientation.Vertical) {
        def updateResults(results: Seq[(String, (Array[Double], Array[Double]))]) {
          contents.clear()
          results.foreach {
            case (label, (_, params)) => contents += new Label(label + ": " + new MinimizeParameters(params(0), params(1), params(2)))
          }
        }
      }
      add(resultsParamsPanel, BorderPanel.Position.North)
    }
  }

  def top = new MainFrame {
    title = "Analyzer research"
    contents = new BorderPanel {

      val selectSampleFileButton: Button = new Button(new Action("Select sample file") {
        def apply() {
          val chooser = new FileChooser
          chooser.fileFilter = new FileNameExtensionFilter("Optical disk exp raw data", "txt", "bin", "sgl")
          chooser.showOpenDialog(selectSampleFileButton) match {
            case FileChooser.Result.Approve => {
              RangeModel.sampleFile.value = chooser.selectedFile
            }
            case _ =>
          }
        }
      })
      add(new GridPanel(4, 3) {
        contents += new Label("Omega")
        contents += new BindDecimalField(RangeModel.omegaFrom)
        contents += new BindDecimalField(RangeModel.omegaTo)

        contents += new Label("Delta")
        contents += new BindDecimalField(RangeModel.deltaFrom)
        contents += new BindDecimalField(RangeModel.deltaTo)

        contents += new Label("Phi")
        contents += new BindDecimalField(RangeModel.phiFrom)
        contents += new BindDecimalField(RangeModel.phiTo)
        contents += selectSampleFileButton
      }, BorderPanel.Position.North)

      add(new TabbedPane {
        vars.foreach {
          v =>
            pages += new Page(v.label, new BindTriggerPlot2DPanel(RangeModel.omegaFrom, RangeModel.omegaTo, RangeModel.deltaFrom, RangeModel.deltaTo, RangeModel.phiFrom, RangeModel.phiTo, RangeModel.sampleFile) {
              def updatePlots() {
                addLinePlot(v.label, errorValue2DPlot(v))
              }
            })
        }
        vars.flatMap(v1 => vars.map(v2 => Set(v1, v2))).filter(_.size == 2).map(_.toSeq).foreach {
          case Seq(v1, v2) => {
            val label = v1.label + " - " + v2.label
            pages += new Page(label, new BindTriggerPlot3DPanel(RangeModel.omegaFrom, RangeModel.omegaTo, RangeModel.deltaFrom, RangeModel.deltaTo, RangeModel.phiFrom, RangeModel.phiTo, RangeModel.sampleFile) {
              def updatePlots() {
                val pointCount3D = 50
                setAxisLabels(v1.label, v2.label, "S")
                addGridPlot(label, errorValue3DPlot(v1, v2, pointCount3D), pointCount3D)
                addScatterPlot("Scan params", scanParametersForErrorPlot(v1, v2))
              }
            })
          }
        }
        pages += new Page("Optimize comparison plot panel", optimizerComparisonPlotPanel)
        pages += new Page("Scan parameters", new Plot3DPanel {
          addScatterPlot("Scan params", scanParametersSpherical)
        })
        pages += new Page("Sample comparison", new BindTriggerPlot2DPanel(fromFields :_*) {
          def updatePlots() {
            addLinePlot("Original", prepareSample)
            addLinePlot("Analyzed", analyzeResultSample)
            addLinePlot("Filtered", filteredSample)
          }
        })
      }, BorderPanel.Position.Center)
    }
  }

  private def interpolateValue(from: BindField[Double], to: BindField[Double], default: Double, i: Int, pointCount: Int) = {
    from.valueOption.getOrElse(default) + i * (to.valueOption.getOrElse(default) - from.valueOption.getOrElse(default)) / pointCount
  }

  val phiDefault = 0.7
  val deltaDefault = 0.4
  val omegaDefault = 3.0

  val vars = Set(Omega, Delta, Phi)

  trait Var {
    def from: BindField[Double]

    def to: BindField[Double]

    def default: Double

    def value(i: Int, pointCount: Int): Double = interpolateValue(from, to, default, i, pointCount)

    def fromOrDefault = from.valueOption.getOrElse(default)

    def label: String
    
    def valueIn(params: MinimizeParameters): Double
  }

  object Omega extends Var {
    def from = RangeModel.omegaFrom

    def to = RangeModel.omegaTo

    def default = omegaDefault

    def label = "Omega"

    def valueIn(params: MinimizeParameters) = params.getOmega
  }

  object Phi extends Var {
    def from = RangeModel.phiFrom

    def to = RangeModel.phiTo

    def default = phiDefault

    def label = "Phi"

    def valueIn(params: MinimizeParameters) = params.getPhi
  }

  object Delta extends Var {
    def from = RangeModel.deltaFrom

    def to = RangeModel.deltaTo

    def default = deltaDefault

    def label = "Delta"

    def valueIn(params: MinimizeParameters) = params.getDelta
  }

  def prepareSample: Array[Double] = {
    RangeModel.sampleFile.valueOption.map(f => {
      val stream = new FileInputStream(f)
      val reader = new FloatReader(stream)
      val sample = reader.chunkArrayStream.head.map(_.toDouble)
      val period = JacobiAngerAnalyzer.ScanPeriod(sample)
      stream.close()
      JacobiAngerAnalyzer.ReScale(sample.take(period))
    }).getOrElse(TestSample.prepareSample(1, omegaDefault, math.Pi * 2 / 512.0, phiDefault, deltaDefault, 0, 512))
  }

  def analyzeResultSample: Array[Double] = {
    val period = prepareSample.length
    TestSample.prepareSample(1, Omega.fromOrDefault, math.Pi * 2 / period, Phi.fromOrDefault, Delta.fromOrDefault, 0, period)
  }
  
  def filteredSample: Array[Double] = {
    val sample = prepareSample
    val coeffNum = 8
    val fourierCoefficients = FFT.transform(sample, coeffNum)
    val paddedWithZeros = fourierCoefficients ++ (0 until (sample.length - coeffNum)).map(i => new Complex(0,0))
    FFT.transformComplex(paddedWithZeros.map(_.conjugate()), paddedWithZeros.length).map(_.getReal * sample.length)
  }

  def plotEvaluator = {
    val sample = prepareSample
    val fourierCoefficients = FFT.transform(sample, 8)
    new JacobiAngerErrorFuncDiffEvaluator(fourierCoefficients)
  }

  def fromOrDefaultValueMap = {
    vars.map(v => v -> v.fromOrDefault).toMap
  }

  def errorValue2DPlot(trackVar: Var) = {
    val evaluator = plotEvaluator
    val pointCount = 500
    (0 to pointCount).map(i => {
      val valueMap = fromOrDefaultValueMap
      val value = trackVar.value(i, pointCount)
      val resultValueMap = valueMap + (trackVar -> value)
      (value, evaluator.Value(new MinimizeParameters(resultValueMap(Omega), resultValueMap(Delta), resultValueMap(Phi)).Wrap()))
    }).toArray
  }

  def errorValue3DPlot(trackVarX: Var, trackVarY: Var, pointCount: Int): (Int, Int) => (Double, Double, Double) = {
    val evaluator = plotEvaluator
      (i, j) =>
        val valueMap = fromOrDefaultValueMap
        val valueX = trackVarX.value(i, pointCount)
        val valueY = trackVarY.value(j, pointCount)
        val resultValueMap = valueMap + (trackVarX -> valueX) + (trackVarY -> valueY)
        (valueX, valueY, evaluator.Value(new MinimizeParameters(resultValueMap(Omega), resultValueMap(Delta), resultValueMap(Phi)).Wrap()))
  }
  
  def scanParametersForErrorPlot(trackVarX: Var, trackVarY: Var) = {
    val evaluator = plotEvaluator
    JacobiAngerAnalyzer.scanParameters().map(p =>
      (trackVarX.valueIn(p), trackVarY.valueIn(p), evaluator.Value(p.Wrap()))
    )  
  }
  
  def bisectionOptimizerTrackError(initialParams: MinimizeParameters) = {
    val evaluator = plotEvaluator
    val trackCollectionValue = new ArrayList[Array[Double]]()
    val optimizer = new BisectionGradientOptimizer(JacobiAngerAnalyzer.Precision, evaluator, new MinimizeParameters(1.5, math.Pi / 2, math.Pi / 2).Wrap, trackCollectionValue)
    val result = optimizer.Optimize(initialParams.Wrap())
    (trackCollectionValue.map(evaluator.Value).toArray, result)
  }

  def gradientDescentTrackError(initialParams: MinimizeParameters, optimizer: DescentOptimizer) = {
    val evaluator = plotEvaluator
    val result = optimizer.Optimize(initialParams.Wrap())
    (optimizer.trackValueBuffer.map(evaluator.Value).toArray, result)
  }

  def scanParametersSpherical = {
    JacobiAngerAnalyzer.scanParameters().map(p => (
      p.getOmega * math.sin(p.getDelta) * math.cos(p.getPhi),
      p.getOmega * math.sin(p.getDelta) * math.sin(p.getPhi),
      p.getOmega * math.cos(p.getDelta)
    ))
  }
}
