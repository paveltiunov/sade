package org.sade.lab

import plot.PlotPanel
import swing.{MainFrame, SimpleSwingApplication}
import java.awt.Dimension
import org.sade.analyzers.FloatReader
import org.sade.starcoords.{AnalyzerXMLImport, StarCoordsPlotter}
import xml.XML

object StarCoordsMain extends SimpleSwingApplication {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Galactic Coordinates"

    val name = "StepExp-11.03.10-1-window-40-freq-2"

    val points = AnalyzerXMLImport.parse(XML.load(getClass.getResource("/org/sade/starcoords/" + name + ".anx")))

    contents = new StarCoordsPlotter {
      skyMapPoints = points
    }
  }
}