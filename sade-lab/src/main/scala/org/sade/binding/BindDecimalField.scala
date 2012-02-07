package org.sade.binding

import swing.TextField
import swing.event.EditDone

class BindDecimalField(bindField: BindField[Double]) extends TextField {
  listenTo(this)
  reactions += {
    case EditDone(_) => try {bindField.value = text.toDouble} catch {case e: NumberFormatException => }
  }
  
  bindField += {v => text = v.map(_.toString).getOrElse("")}
}
