package org.sade.binding

import org.sade.lab.plot.PlotPanel

abstract class BindPlotPanel[T](bindField: BindField[T]) extends PlotPanel {
  bindField += {v =>
    removeAll()
    v.foreach(updatePlots)
  }

  def updatePlots(value: T)
}