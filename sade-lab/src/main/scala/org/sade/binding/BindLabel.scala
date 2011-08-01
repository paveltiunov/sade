package org.sade.binding

import swing.Label

class BindLabel[T](bindField: BindField[T], labelFun: T => String) extends Label {
  bindField += (v => {
    text = v.map(labelFun).getOrElse("")
  })
}