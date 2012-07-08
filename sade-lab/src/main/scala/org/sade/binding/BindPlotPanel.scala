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
  private var currentFields: Seq[BindField[_]] = Seq()

  def triggerFields: Seq[BindField[_]]

  def updatePlotFun(v: Option[_]) {
      removeAll()
      v.foreach(x => updatePlots())
  }

  def refreshTriggerFields() {
    currentFields.foreach(_ -= updatePlotFun)
    currentFields = triggerFields
    triggerFields.foreach(_ += updatePlotFun)
  }

  refreshTriggerFields()

  def updatePlots()
}

trait BindTriggerPlotPanelWithBindFields[T] extends BindTriggerPlotPanel {
  self : PlotPanelMethods =>

  def triggerBindField: BindField[T]
  def triggerBindFieldMapFun: T => Seq[BindField[_]]

  triggerBindField += (_ => refreshTriggerFields())

  def triggerFields = triggerBindField.valueOption.map(triggerBindFieldMapFun).getOrElse(Seq())
}

abstract class BindTriggerPlot2DPanel(val triggerFields: BindField[_]*) extends PlotPanel with BindTriggerPlotPanel
abstract class BindFieldsTriggerPlot2DPanel[T](val triggerBindField: BindField[T], val triggerBindFieldMapFun: T => Seq[BindField[_]]) extends PlotPanel with BindTriggerPlotPanelWithBindFields[T]
abstract class BindTriggerPlot3DPanel(val triggerFields: BindField[_]*) extends Plot3DPanel with BindTriggerPlotPanel