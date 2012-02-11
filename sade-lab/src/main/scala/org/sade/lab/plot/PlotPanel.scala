package org.sade.lab.plot

import swing.Component
import org.math.plot.Plot2DPanel
import org.math.plot. {PlotPanel => MPlotPanel}

class PlotPanel extends Component with PlotPanelMethods {
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
}

trait PlotPanelMethods {
  def peer: MPlotPanel

  def removeAll() { peer.removeAllPlots() }

  def setAxisLabels(labels: String*) {
    peer.setAxisLabels(labels :_*)
  }
}