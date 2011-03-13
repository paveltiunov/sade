package org.sade.lab

import plot.PlotPanel
import swing.{MainFrame, SimpleSwingApplication}
import org.sade.analyzers.FloatReader
import java.awt.Dimension

object SadeLabMain extends SimpleSwingApplication {
  def top = new MainFrame {
    size = new Dimension(800, 600)
    title = "SADE Lab"
    contents = new PlotPanel {
      addLinePlot("Hello",
        new FloatReader(getClass.getResource("/org/sade/analyzers/test_00000.txt").openStream)
          .chunkStream
          .take(10000)
          .toArray
          .map(_.toDouble)
      )
    }
  }

}