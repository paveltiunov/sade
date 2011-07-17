package org.sade.lab

import plot.PlotPanel
import java.awt.Dimension
import org.sade.analyzers.{SignalAnalyzer}
import swing.TabbedPane.Page
import swing.{Panel, TabbedPane, MainFrame, SimpleSwingApplication}

object SadeLabMain extends SimpleSwingApplication with NimbusLookAndFeel {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Lab"
    val inputStream = getClass.getResource("/org/sade/analyzers/test_00000.bin").openStream
    val signalAnalyzer = new SignalAnalyzer(inputStream)
    contents = new TabbedPane {
      pages += new Page("Amalyzed signal", new PlotPanel {
        addLinePlot("Analyzed signal",
          signalAnalyzer.foundDeltaStream
            .toArray
        )
      })
      pages += new Page("Signal", new PlotPanel {
        addLinePlot("Signal", signalAnalyzer.originalSignal.toArray)
        addLinePlot("Estimated signal", signalAnalyzer.estimatedSignal.toArray)
      })

    }
  }
}