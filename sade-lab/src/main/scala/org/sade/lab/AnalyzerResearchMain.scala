package org.sade.lab

import org.sade.analyzers.math.FFT
import plot.{Plot3DPanel, PlotPanel}
import swing._
import swing.TabbedPane.Page
import org.sade.binding.{BindTriggerPlot3DPanel, BindTriggerPlot2DPanel, BindField, BindDecimalField}
import java.util.ArrayList
import scala.collection.JavaConversions._
import org.sade.analyzers._
import scala.math


object AnalyzerResearchMain extends SimpleSwingApplication with NimbusLookAndFeel {

  object RangeModel {

    object omegaFrom extends BindField[Double]

    object omegaTo extends BindField[Double]

    object deltaFrom extends BindField[Double]

    object deltaTo extends BindField[Double]

    object phiFrom extends BindField[Double]

    object phiTo extends BindField[Double]

  }

  def optimizerComparisonPlotPanel(params: MinimizeParameters) = {
    val results = Seq(
      "Bisection" -> bisectionOptimizerTrackError(params),
      "Gradient" -> gradientDescentTrackError(params, new GradientDescentOptimizer(JacobiAngerAnalyzer.Precision, evaluator)),
      "Fastest gradient" -> gradientDescentTrackError(params, new FastestGradientDescentOptimizer(JacobiAngerAnalyzer.Precision, evaluator)))
    new BorderPanel {
      add (new PlotPanel {
        setAxisLabels("Step", "S")
        results.foreach {case (label, (track, _)) => addLinePlot(label, track)}
      }, BorderPanel.Position.Center)
      add(new BoxPanel(Orientation.Vertical) {
        results.foreach {
          case (label, (_, params)) => contents += new Label(label + ": " + params.mkString(", "))
        }
      }, BorderPanel.Position.North)
    }
  }

  def top = new MainFrame {
    title = "Analyzer research"
    contents = new BorderPanel {
      add(new GridPanel(3, 3) {
        contents += new Label("Omega")
        contents += new BindDecimalField(RangeModel.omegaFrom)
        contents += new BindDecimalField(RangeModel.omegaTo)

        contents += new Label("Delta")
        contents += new BindDecimalField(RangeModel.deltaFrom)
        contents += new BindDecimalField(RangeModel.deltaTo)

        contents += new Label("Phi")
        contents += new BindDecimalField(RangeModel.phiFrom)
        contents += new BindDecimalField(RangeModel.phiTo)
      }, BorderPanel.Position.North)

      add(new TabbedPane {
        vars.foreach {
          v =>
            pages += new Page(v.label, new BindTriggerPlot2DPanel(RangeModel.omegaFrom, RangeModel.omegaTo, RangeModel.deltaFrom, RangeModel.deltaTo, RangeModel.phiFrom, RangeModel.phiTo) {
              def updatePlots() {
                addLinePlot(v.label, errorValue2DPlot(v))
              }
            })
        }
        vars.flatMap(v1 => vars.map(v2 => Set(v1, v2))).filter(_.size == 2).map(_.toSeq).foreach {
          case Seq(v1, v2) => {
            val label = v1.label + " - " + v2.label
            pages += new Page(label, new BindTriggerPlot3DPanel(RangeModel.omegaFrom, RangeModel.omegaTo, RangeModel.deltaFrom, RangeModel.deltaTo, RangeModel.phiFrom, RangeModel.phiTo) {
              def updatePlots() {
                val pointCount3D = 50
                setAxisLabels(v1.label, v2.label, "S")
                addGridPlot(label, errorValue3DPlot(v1, v2, pointCount3D), pointCount3D)
                addScatterPlot("Scan params", scanParametersForErrorPlot(v1, v2))
              }
            })
          }
        }
        pages += new Page("Easy optimize", optimizerComparisonPlotPanel(new MinimizeParameters(2.9, 0.3, 0.5)))
        pages += new Page("Hard optimize", optimizerComparisonPlotPanel(new MinimizeParameters(2.5, 1.5, 1.5)))
        pages += new Page("Scan parameters", new Plot3DPanel {
          addScatterPlot("Scan params", scanParametersSpherical)
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


  def plotEvaluator = {
    val sample = TestSample.prepareSample(1, omegaDefault, math.Pi * 2 / 512.0, phiDefault, deltaDefault, 0, 512)
    val fourierCoefficients = FFT.transform(sample, 8)
    new JacobiAngerErrorFuncDiffEvaluator(fourierCoefficients)
  }

  def fromOrDefaultValueMap = {
    vars.map(v => v -> v.fromOrDefault).toMap
  }

  val evaluator = plotEvaluator

  def errorValue2DPlot(trackVar: Var) = {
    val pointCount = 500
    (0 to pointCount).map(i => {
      val valueMap = fromOrDefaultValueMap
      val value = trackVar.value(i, pointCount)
      val resultValueMap = valueMap + (trackVar -> value)
      (value, evaluator.Value(new MinimizeParameters(resultValueMap(Omega), resultValueMap(Delta), resultValueMap(Phi)).Wrap()))
    }).toArray
  }

  def errorValue3DPlot(trackVarX: Var, trackVarY: Var, pointCount: Int): (Int, Int) => (Double, Double, Double) = {
    (i, j) =>
      val valueMap = fromOrDefaultValueMap
      val valueX = trackVarX.value(i, pointCount)
      val valueY = trackVarY.value(j, pointCount)
      val resultValueMap = valueMap + (trackVarX -> valueX) + (trackVarY -> valueY)
      (valueX, valueY, evaluator.Value(new MinimizeParameters(resultValueMap(Omega), resultValueMap(Delta), resultValueMap(Phi)).Wrap()))
  }
  
  def scanParametersForErrorPlot(trackVarX: Var, trackVarY: Var) = {
    JacobiAngerAnalyzer.scanParameters().map(p =>
      (trackVarX.valueIn(p), trackVarY.valueIn(p), evaluator.Value(p.Wrap()))
    )  
  }
  
  def bisectionOptimizerTrackError(initialParams: MinimizeParameters) = {
    val trackCollectionValue = new ArrayList[Array[Double]]()
    val optimizer = new BisectionGradientOptimizer(JacobiAngerAnalyzer.Precision, evaluator, new MinimizeParameters(1.5, math.Pi / 2, math.Pi / 2).Wrap, trackCollectionValue)
    val result = optimizer.Optimize(initialParams.Wrap())
    (trackCollectionValue.map(evaluator.Value).toArray, result)
  }

  def gradientDescentTrackError(initialParams: MinimizeParameters, optimizer: DescentOptimizer) = {
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
