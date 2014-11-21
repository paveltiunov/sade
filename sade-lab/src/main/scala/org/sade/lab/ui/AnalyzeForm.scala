package org.sade.lab.ui

import java.awt.Dimension
import swing.TabbedPane.Page
import swing._
import swing.BorderPanel.Position
import scala.concurrent.ops._
import java.io.{File, FileInputStream}
import org.sade.binding.{BindProgressBar, BindPlotPanel, BindLabel, BindField}
import actors.threadpool.AtomicInteger
import concurrent.{FutureTaskRunner, TaskRunners, JavaConversions}
import org.sade.analyzers.{AnalyzeResult, SignalAnalyzer}
import java.util.concurrent.Executors

class AnalyzeForm(val file: File, killOnClose: Boolean = true, defaultRunner: Option[FutureTaskRunner] = None) extends Frame {
  implicit val runner = defaultRunner.getOrElse(JavaConversions.asTaskRunner(Executors.newSingleThreadExecutor()))
  size = new Dimension(800, 600)
  title = file.getAbsolutePath
  private var killed = false

  def kill() {
    killed = true
  }

  override def closeOperation() {
    super.closeOperation()
    if (killOnClose) kill()
  }

  object AnalyzerModel {

    object signalAnalyzer extends BindField[SignalAnalyzer]

    object analyzeProgress extends BindField[Int]

    val lastProcessingToken = new AtomicInteger()
  }

  initFileMonitorAndStartAnalyze()

  private var lastModified: Long = file.lastModified()

  private def checkFileChanged() {
    if (!killed) {
      spawn {
        if (file.lastModified() > lastModified) {
          lastModified = file.lastModified()
          startAnalyze()
          checkFileChanged()
        } else {
          Thread.sleep(1000)
          checkFileChanged()
        }
      }
    }
  }

  private def initFileMonitorAndStartAnalyze() {
    startAnalyze()
    checkFileChanged()
  }

  def isProcessingTokenIsCurrent(processingToken: Int): Boolean = {
    AnalyzerModel.lastProcessingToken.get() == processingToken
  }

  private def startAnalyze() {
    if (file.exists() && !killed) {
      spawn {
        val fileInputStream = new FileInputStream(file)
        try {
          val processingToken = AnalyzerModel.lastProcessingToken.incrementAndGet()
          val signalAnalyzer = new SignalAnalyzer(
            fileInputStream,
            Some(i => {
              if (isProcessingTokenIsCurrent(processingToken))
                AnalyzerModel.analyzeProgress.set(math.round(i * 100))
            })
          )
          if (isProcessingTokenIsCurrent(processingToken))
            AnalyzerModel.signalAnalyzer.set(signalAnalyzer)
        } finally {
          fileInputStream.close()
        }
      }
    }
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
      def analyzeResultPlotPage(label: String, analayzeResultFun: AnalyzeResult => Double) = {
        new Page(label, new BindPlotPanel(AnalyzerModel.signalAnalyzer) {
          def updatePlots(signalAnalyzer: SignalAnalyzer) {
            addLinePlot(label, signalAnalyzer.analyzeResults.map(analayzeResultFun).toArray)
          }
        })
      }
      pages += analyzeResultPlotPage ("Period", _.getRealPeriod.toDouble)
      pages += analyzeResultPlotPage ("Minimize error", _.getMinimizeError)
      pages += new Page("Statistics", new BorderPanel {
        val statistics = new GridPanel(5, 2) {
          contents += new Label("Mean value")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.meanValue.toString)
          contents += new Label("Absolute error")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.absoluteError.toString)
          contents += new Label("Period count")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.foundDeltaStream.size.toString)
          contents += new Label("Mean frequency")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.meanFrequency.toString)
          contents += new Label("Omega Mean value")
          contents += new BindLabel[SignalAnalyzer](AnalyzerModel.signalAnalyzer, _.omegaMean.toString)
        }
        layout(statistics) = Position.North
      })
    }
    layout(tabbedPane) = Position.Center
    val bar = new BindProgressBar[Int](AnalyzerModel.analyzeProgress, v => v)
    layout(bar) = Position.South
  }
}

