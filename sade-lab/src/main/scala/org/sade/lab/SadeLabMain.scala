package org.sade.lab

import java.awt.Dimension
import org.sade.analyzers.SignalAnalyzer
import swing.TabbedPane.Page
import swing._
import swing.BorderPanel.Position
import javax.swing.filechooser.FileNameExtensionFilter
import scala.concurrent.ops._
import java.io.{File, FileInputStream}
import org.sade.binding.{BindProgressBar, BindPlotPanel, BindLabel, BindField}

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
            new AnalyzeForm(chooser.selectedFile).open()
          }
          case _ =>
        }
      }
    })
    contents = button
  }
}

class AnalyzeForm(file: File) extends Frame {
  size = new Dimension(800, 600)
  title = file.getAbsolutePath

  object AnalyzerModel {

    object signalAnalyzer extends BindField[SignalAnalyzer]

    object analyzeProgress extends BindField[Int]

  }

  spawn {
    val signalAnalyzer = new SignalAnalyzer(
      new FileInputStream(file),
      Some(i => AnalyzerModel.analyzeProgress.set(math.round(i*100)))
    )
    AnalyzerModel.signalAnalyzer.set(signalAnalyzer)
  }

  contents = new BorderPanel {
    val tabbedPane = new TabbedPane {
      pages += new Page("Analyzed signal", new BindPlotPanel(AnalyzerModel.signalAnalyzer) {
        def updatePlots(signalAnalyzer: SignalAnalyzer) {
          addLinePlot("Analyzed signal",
            signalAnalyzer.foundDeltaStream
          )
        }
      })
      pages += new Page("Signal", new BindPlotPanel(AnalyzerModel.signalAnalyzer) {
        def updatePlots(signalAnalyzer: SignalAnalyzer) {
          addLinePlot("Signal", signalAnalyzer.originalSignal.toArray)
          addLinePlot("Estimated signal", signalAnalyzer.estimatedSignal.toArray)
        }
      })
      pages += new Page("Statistics", new BorderPanel {
        val statistics = new GridPanel(4, 2) {
          contents += new Label("Mean value")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.meanValue.toString)
          contents += new Label("Absolute error")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.absoluteError.toString)
          contents += new Label("Period count")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.foundDeltaStream.size.toString)
          contents += new Label("Mean frequency")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.meanFrequency.toString)
        }
        layout(statistics) = Position.North
      })
    }
    layout(tabbedPane) = Position.Center
    val bar = new BindProgressBar[Int](AnalyzerModel.analyzeProgress, v => v)
    layout(bar) = Position.South
  }
}