package org.sade.binding

import swing.TextField
import swing.event.EditDone

class BindTextField(bindField: BindField[String]) extends TextField {
  listenTo(this)
  reactions += {
    case EditDone(_) => bindField.value = text
  }
  
  bindField += {v => text = v.getOrElse("")}
}
