package org.sade.lab

import plot.PlotPanel
import java.awt.Dimension
import org.sade.analyzers.FloatReader
import xml.XML
import com.sun.xml.internal.ws.wsdl.writer.document.http.Address
import swing._
import event.{MouseClicked, SelectionChanged}
import collection.mutable.Buffer
import swing.BorderPanel.Position
import swing.FlowPanel.Alignment
import org.sade.starcoords._
import java.io.{File, FileInputStream, InputStream}
import javax.swing.filechooser.{FileNameExtensionFilter, FileFilter}
import javax.swing.UIManager

object StarCoordsMain extends SimpleSwingApplication with NimbusLookAndFeel {
  def loadPoints(inputStream: InputStream) = {
    AnalyzerXMLImport.parse(XML.load(inputStream))
  }

  def top = new MainFrame {
    size = new Dimension(800, 600)
    val titlePrefix = "SADE Galactic Coordinates"
    title = titlePrefix

    val name = "StepExp-11.03.10-1-window-40-freq-2"

    val inputStream = getClass.getResource("/org/sade/starcoords/" + name + ".anx").openStream()
    val points = loadPoints(inputStream)

    val starCoordsPlotter = new StarCoordsPlotter {
      skyMapPoints = points
    }

    contents = new BorderPanel {
      add(new FlowPanel(Alignment.Left)(
        new ComboBox(Seq(Galactic, Plane)) {
          listenTo(selection)
          reactions += {
            case SelectionChanged(_) => starCoordsPlotter.changeViewMode(selection.item)
          }
        },
        new Button("Open") {
          listenTo(mouse.clicks)
          reactions += {
            case e: MouseClicked => {
              val chooser = new FileChooser
              chooser.fileFilter = new FileNameExtensionFilter("Optical disk exp analyzer result", "anx")
              chooser.showOpenDialog(starCoordsPlotter) match {
                case FileChooser.Result.Approve => {
                  title = titlePrefix + ": " + chooser.selectedFile.getName
                  starCoordsPlotter.changeSkyMapPoints(loadPoints(new FileInputStream(chooser.selectedFile)))
                }
                case _ =>
              }
            }
          }
        }
      ), Position.North)

      add(starCoordsPlotter, Position.Center)
    }

  }
}