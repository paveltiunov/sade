package org.sade.lab

import plot.PlotPanel
import swing.{MainFrame, SimpleSwingApplication}
import org.sade.analyzers.math.FFT
import org.sade.analyzers.{MinimizeParameters, JacobiAngerErrorFuncDiffEvaluator, TestSample}


object AnalyzerResearchMain extends SimpleSwingApplication with NimbusLookAndFeel {
  def top = new MainFrame {
    title = "Analyzer research"
    contents = new PlotPanel {
      addLinePlot("Omega", omegaError)
    }
  }
  
  def omegaError = {
    val phi = 0.7
    val delta = 0.4
    val omega = 3
    val sample = TestSample.prepareSample(1, omega, math.Pi * 2 / 512.0, phi, delta, 0, 512)
    val fourierCoefficients = FFT.transform(sample, 8)
    val evaluator = new JacobiAngerErrorFuncDiffEvaluator(fourierCoefficients)
    (-250 to 250).map(i => {
      val omegaX = omega + i * 0.1
      (omegaX, evaluator.Value(new MinimizeParameters(omegaX, delta, phi).Wrap()))
    }).toArray
  }
}
