package org.sade.lab.plot

import swing.Component
import org.math.plot.{Plot3DPanel => MPlot3DPanel}

class Plot3DPanel extends Component with PlotPanelMethods {
  override lazy val peer = new MPlot3DPanel with SuperMixin

  def pointTripleArray(points: Seq[(Double, Double, Double)]): Array[Array[Double]] = {
    points.map(p => Array(p._1, p._2, p._3)).toArray
  }

  def addLinePlot(name: String, points: Seq[(Double, Double, Double)]) {
    peer.addLinePlot(name, pointTripleArray(points))
  }

  def addScatterPlot(name: String, points: Seq[(Double, Double, Double)]) {
    peer.addScatterPlot(name, pointTripleArray(points))
  }

  def addGridPlot(name: String, pointsFun: (Int, Int) => (Double, Double, Double), pointsCount: Int) {
    val valueMap = (0 to pointsCount).flatMap(i => (0 to pointsCount).map(j => (i, j) -> pointsFun(i, j))).toMap
    val xArray = valueMap.map {
      case ((i, _), (x, _, _)) => i -> x
    }.toSeq.sortBy(_._1).map(_._2).toArray

    val yArray = valueMap.map {
      case ((_, j), (_, y, _)) => j -> y
    }.toSeq.sortBy(_._1).map(_._2).toArray

    val zArray = (0 to pointsCount).map(i => (0 to pointsCount).map(j => valueMap(j, i)._3).toArray).toArray
    peer.addGridPlot(name, xArray, yArray, zArray)
  }
}
