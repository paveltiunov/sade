package org.sade.lab.plot

import swing.Component
import org.math.plot.Plot2DPanel


class PlotPanel extends Component {
  override lazy val peer = new Plot2DPanel with SuperMixin

  def addLinePlot(name: String, points: Array[Double]) {
    peer.addLinePlot(name, points)
  }
}