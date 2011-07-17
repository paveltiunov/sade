package org.sade.lab

import plot.PlotPanel
import swing.{MainFrame, SimpleSwingApplication}
import java.awt.Dimension
import org.sade.analyzers.{SignalAnalyzer, FloatReader}

object SadeLabMain extends SimpleSwingApplication {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Lab"
    val inputStream = getClass.getResource("/org/sade/analyzers/test_00000.bin").openStream
    val signalAnalyzer = new SignalAnalyzer(inputStream)
    contents = new PlotPanel {
      addLinePlot("Hello",
        signalAnalyzer.foundDeltaStream.take(500)
          .toArray
      )
    }
  }

}