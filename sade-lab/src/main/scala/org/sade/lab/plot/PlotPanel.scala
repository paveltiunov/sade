package org.sade.lab.plot

import swing.Component
import org.math.plot.Plot2DPanel

class PlotPanel extends Component {
  override lazy val peer = new Plot2DPanel with SuperMixin

  def addLinePlot(name: String, points: Array[Double]) {
    peer.addLinePlot(name, points)
  }

  def addLinePlot(name: String, points: Seq[(Double, Double)]) {
    peer.addLinePlot(name, points.map(p => Array(p._1, p._2)).toArray)
  }

  def addScatterPlot(name: String, points: Seq[(Double, Double)]) {
    peer.addScatterPlot(name, points.map(p => Array(p._1, p._2)).toArray)
  }

  def removeAll() { peer.removeAllPlots() }
}