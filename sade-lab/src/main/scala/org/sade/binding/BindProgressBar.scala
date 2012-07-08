package org.sade.binding

import swing.ProgressBar


class BindProgressBar[T](bindField: BindField[T], valueFun: T => Int) extends ProgressBar {
  updateProgress(bindField.valueOption)
  bindField += updateProgress

  def updateProgress(v: Option[T]) = value = v.map(valueFun).getOrElse(0)
}