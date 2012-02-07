package org.sade.lab

import org.sade.analyzers.math.FFT
import org.sade.analyzers.{MinimizeParameters, JacobiAngerErrorFuncDiffEvaluator, TestSample}
import swing._
import org.sade.binding.{BindTriggerPlotPanel, BindField, BindDecimalField}


object AnalyzerResearchMain extends SimpleSwingApplication with NimbusLookAndFeel {
  object RangeModel {
    object omegaFrom extends BindField[Double]
    object omegaTo extends BindField[Double]
  }

  def top = new MainFrame {
    title = "Analyzer research"
    contents = new BorderPanel {
      add(new GridPanel(1,3) {
        contents += new Label("Omega")
        contents += new BindDecimalField(RangeModel.omegaFrom)
        contents += new BindDecimalField(RangeModel.omegaTo)
      }, BorderPanel.Position.North)
      
      add(new BindTriggerPlotPanel(RangeModel.omegaFrom, RangeModel.omegaTo) {
        def updatePlots() {
          addLinePlot("Omega", omegaError)
        }
      }, BorderPanel.Position.Center)
    }
  }
  
  def omegaError = {
    val phi = 0.7
    val delta = 0.4
    val omega = 3.0
    val sample = TestSample.prepareSample(1, omega, math.Pi * 2 / 512.0, phi, delta, 0, 512)
    val fourierCoefficients = FFT.transform(sample, 8)
    val evaluator = new JacobiAngerErrorFuncDiffEvaluator(fourierCoefficients)
    val (omegaFrom, omegaTo) = (RangeModel.omegaFrom.valueOption.getOrElse(omega), RangeModel.omegaTo.valueOption.getOrElse(omega))
    val pointCount = 500
    (0 to pointCount).map(i => {
      val omegaX = omegaFrom + i * (omegaTo - omegaFrom) / pointCount
      (omegaX, evaluator.Value(new MinimizeParameters(omegaX, delta, phi).Wrap()))
    }).toArray
  }
}
