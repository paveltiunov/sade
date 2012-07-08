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
import actors.threadpool.AtomicInteger
import concurrent.JavaConversions
import java.util.concurrent.Executors
import swing.FileChooser.SelectionMode
import ui.{AnalyzeDirectoryForm, UploaderForm, AnalyzeForm}

object SadeLabMain extends SimpleSwingApplication with NimbusLookAndFeel {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Lab"
    val frame = this
    val analyzeSinglePointButton: Button = new Button(new Action("Analyze single point") {
      def apply() {
        val chooser = new FileChooser
        chooser.fileFilter = new FileNameExtensionFilter("Optical disk exp raw data", "txt", "bin", "sgl")
        chooser.showOpenDialog(analyzeSinglePointButton) match {
          case FileChooser.Result.Approve => {
            new AnalyzeForm(chooser.selectedFile).open()
          }
          case _ =>
        }
      }
    })
    val analyzeDirectoryButton: Button = new Button(new Action("Analyze directory") {
      def apply() {
        val chooser = new FileChooser
        chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
        chooser.showOpenDialog(analyzeDirectoryButton) match {
          case FileChooser.Result.Approve => {
            new AnalyzeDirectoryForm(chooser.selectedFile).open()
          }
          case _ =>
        }
      }
    })
    val uploadExperimentDataButton: Button = new Button(new Action("Upload experiment data") {
      def apply() {
        val chooser = new FileChooser
        chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
        chooser.showOpenDialog(uploadExperimentDataButton) match {
          case FileChooser.Result.Approve => {
            new UploaderForm(chooser.selectedFile, "http://localhost:8080").open()
          }
          case _ =>
        }
      }
    })
    contents = new BoxPanel(Orientation.Vertical) {
      contents ++= Seq(analyzeSinglePointButton, analyzeDirectoryButton, uploadExperimentDataButton)
    }
  }
}

