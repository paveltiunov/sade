package org.sade.binding

import org.sade.lab.plot.PlotPanel

abstract class BindPlotPanel[T](bindField: BindField[T]) extends PlotPanel {
  bindField += {v =>
    removeAll()
    v.foreach(updatePlots)
  }

  def updatePlots(value: T)
}

abstract class BindTriggerPlotPanel(triggerFields: BindField[_]*) extends PlotPanel {
  val updatePlotFun = {
    v: Option[_] =>
      removeAll()
      v.foreach(x => updatePlots())
  }
  triggerFields.foreach(_ += updatePlotFun)

  def updatePlots()
}