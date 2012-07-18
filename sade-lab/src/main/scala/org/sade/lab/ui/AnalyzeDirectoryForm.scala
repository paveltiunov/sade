package org.sade.lab.ui

import java.io.File
import swing._
import swing.TabbedPane.Page
import org.sade.binding.{BindFieldsTriggerPlot2DPanel, BindPlotPanel, BindProgressBar, BindField}
import scala.concurrent.ops._
import concurrent.TaskRunners

class AnalyzeDirectoryForm(directory: File) extends Frame {
  implicit val runner = TaskRunners.threadPoolRunner
  title = directory.getAbsolutePath
  private var killed = false

  object filesInDir extends BindField[Map[File, AnalyzeForm]]

  updateFilesInDir()


  override def closeOperation() {
    super.closeOperation()
    killed = true
    updateFilesInDir(Set())
  }

  def updateFilesInDir() {
    if (!killed) {
      spawn {
        val files = directory
          .listFiles()
          .filter(f => f.getName.endsWith(".txt") || f.getName.endsWith(".bin")).toSet
        updateFilesInDir(if (!killed) files else Set())
        Thread.sleep(1000)
        updateFilesInDir()
      }
    }
  }

  def updateFilesInDir(files: Set[File]) {
    val currentFiles = filesInDir.valueOption.getOrElse[Map[File, AnalyzeForm]](Map())
    val newFiles = files -- currentFiles.keySet
    val toRemove = currentFiles.keySet -- files
    toRemove.map(currentFiles).foreach(_.kill())
    val newFileMap = currentFiles ++ newFiles.map(f => f -> new AnalyzeForm(f, killOnClose = false, defaultRunner = Some(TaskRunners.threadPoolRunner))).toMap -- toRemove
    filesInDir.set(newFileMap)
  }

  contents = new TabbedPane {
    pages += new Page("Analyze progress", new ScrollPane(new BoxPanel(Orientation.Vertical) {
      filesInDir.+=(_.foreach(fs => updateContents(fs.toSeq.sortBy(_._1.getName).map(_._2))))

      def updateContents(analyzeForms: Seq[AnalyzeForm]) {
        contents.clear()
        contents ++= analyzeForms.map(form => new BoxPanel(Orientation.Horizontal) {
          contents ++= Seq(
            new Button(Action(form.file.getName) {
              form.open()
            }),
            new BindProgressBar[Int](form.AnalyzerModel.analyzeProgress, v => v)
          )
        })
        pack()
      }
    }))
    val tablePane = new ScrollPane()
    pages += new Page("Analyze results", new BindFieldsTriggerPlot2DPanel[Map[File, AnalyzeForm]](filesInDir, _.values.map(_.AnalyzerModel.signalAnalyzer).toSeq) {
      def updatePlots() {
        filesInDir.valueOption.foreach(files => {
          val points = files.values.flatMap(_.AnalyzerModel.signalAnalyzer.valueOption.map(a => a.meanFrequency -> a.meanValue)).toSeq.sortBy(_._1)
          addLinePlot("Mean value by frequency", points)
          tablePane.contents = new Table(
            files.toSeq.sortBy(_._1.getName).map(_._2).flatMap(f => f.AnalyzerModel.signalAnalyzer.valueOption.map(a => List(f.file.getName, a.meanFrequency, a.meanValue, a.absoluteError).toArray)).toArray,
            Seq("File", "Mean frequency", "Mean value", "Absolute error")
          )
        }
        )
      }
    })
    pages += new Page("Full statistics", tablePane)
  }
}
