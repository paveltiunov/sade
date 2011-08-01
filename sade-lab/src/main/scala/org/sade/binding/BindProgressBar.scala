package org.sade.binding

import swing.ProgressBar


class BindProgressBar[T](bindField: BindField[T], valueFun: T => Int) extends ProgressBar {
  bindField += { v =>
    value = v.map(valueFun).getOrElse(0)
  }
}