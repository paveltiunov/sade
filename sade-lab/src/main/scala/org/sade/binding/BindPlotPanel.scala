package org.sade.binding

import org.sade.lab.plot.{Plot3DPanel, PlotPanelMethods, PlotPanel}

abstract class BindPlotPanel[T](bindField: BindField[T]) extends PlotPanel {
  bindField += {v =>
    removeAll()
    v.foreach(updatePlots)
  }

  def updatePlots(value: T)
}

trait BindTriggerPlotPanel {
  self : PlotPanelMethods =>

  def triggerFields: Seq[BindField[_]]

  def updatePlotFun(v: Option[_]) {
      removeAll()
      v.foreach(x => updatePlots())
  }

  triggerFields.foreach(_ += updatePlotFun)

  def updatePlots()
}

abstract class BindTriggerPlot2DPanel(val triggerFields: BindField[_]*) extends PlotPanel with BindTriggerPlotPanel
abstract class BindTriggerPlot3DPanel(val triggerFields: BindField[_]*) extends Plot3DPanel with BindTriggerPlotPanel