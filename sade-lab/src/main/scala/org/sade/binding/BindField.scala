package org.sade.binding

import javax.swing.SwingUtilities


trait BindField[T] {
  private var listeners: Seq[Option[T] => Unit] = Nil
  private var fieldValue: Option[T] = None

  def set(value: T) {
    val newValue = Option(value)
    if (fieldValue != newValue) {
      fieldValue = newValue
      SwingUtilities.invokeLater(new Runnable {
        def run() {
          listeners.foreach(_(fieldValue))
        }
      })
    }
  }

  def value:T = fieldValue.getOrElse(null.asInstanceOf[T])
  def valueOption: Option[T] = fieldValue

  def value_=(v: T) { set(v) }

  def +=(listener: Option[T] => Unit) {
    listeners = listeners :+ listener
  }
}