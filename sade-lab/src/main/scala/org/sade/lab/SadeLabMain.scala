package org.sade.lab

import plot.PlotPanel
import java.awt.Dimension
import org.sade.analyzers.{SignalAnalyzer}
import swing.TabbedPane.Page
import swing._
import swing.BorderPanel.Position
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.{FileInputStream, InputStream}

object SadeLabMain extends SimpleSwingApplication with NimbusLookAndFeel {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Lab"
    val frame = this
    val button: Button = new Button(new Action("Open") {
      def apply() {
        val chooser = new FileChooser
        chooser.fileFilter = new FileNameExtensionFilter("Optical disk exp raw data", "txt", "bin", "sgl")
        chooser.showOpenDialog(button) match {
          case FileChooser.Result.Approve => {
            val inputStream = new FileInputStream(chooser.selectedFile)
            new AnalyzeForm(inputStream).open()
          }
          case _ =>
        }
      }
    })
    contents = button
  }
}

class AnalyzeForm(inputStream: InputStream) extends Frame {
  size = new Dimension(800, 600)
  title = "SADE Lab"
  val signalAnalyzer = new SignalAnalyzer(inputStream)
  contents = new TabbedPane {
    pages += new Page("Amalyzed signal", new PlotPanel {
      addLinePlot("Analyzed signal",
        signalAnalyzer.foundDeltaStream
      )
    })
    pages += new Page("Signal", new PlotPanel {
      addLinePlot("Signal", signalAnalyzer.originalSignal.toArray)
      addLinePlot("Estimated signal", signalAnalyzer.estimatedSignal.toArray)
    })
    pages += new Page("Statistics", new BorderPanel {
      val statistics = new GridPanel(4, 2) {
        contents += new Label("Mean value")
        contents += new Label(signalAnalyzer.meanValue.toString)
        contents += new Label("Absolute error")
        contents += new Label(signalAnalyzer.absoluteError.toString)
        contents += new Label("Period count")
        contents += new Label(signalAnalyzer.foundDeltaStream.size.toString)
        contents += new Label("Mean frequency")
        contents += new Label(signalAnalyzer.meanFrequency.toString)
      }
      layout(statistics) = Position.North
    })
  }
}