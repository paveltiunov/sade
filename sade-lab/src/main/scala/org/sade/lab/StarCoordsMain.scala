package org.sade.lab

import plot.PlotPanel
import java.awt.Dimension
import org.sade.analyzers.FloatReader
import xml.XML
import com.sun.xml.internal.ws.wsdl.writer.document.http.Address
import swing._
import event.SelectionChanged
import org.sade.starcoords.{Plane, Galactic, AnalyzerXMLImport, StarCoordsPlotter}
import collection.mutable.Buffer
import swing.BorderPanel.Position

object StarCoordsMain extends SimpleSwingApplication {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Galactic Coordinates"

    val name = "StepExp-11.03.10-1-window-40-freq-2"

    val points = AnalyzerXMLImport.parse(XML.load(getClass.getResource("/org/sade/starcoords/" + name + ".anx")))

    val starCoordsPlotter = new StarCoordsPlotter {
      skyMapPoints = points
    }

    contents = new BorderPanel {
      add(new ComboBox(Seq(Galactic, Plane)) {
        listenTo(selection)
        reactions += {
          case SelectionChanged(_) => starCoordsPlotter.changeViewMode(selection.item)
        }
      }, Position.North)

      add(starCoordsPlotter, Position.Center)
    }

  }
}