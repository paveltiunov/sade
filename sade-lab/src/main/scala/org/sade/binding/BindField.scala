package org.sade.binding

import javax.swing.SwingUtilities


trait BindField[T] {
  private var listeners: Seq[Option[T] => Unit] = Nil
  private var fieldValue: Option[T] = None

  def set(value: T) {
    fieldValue = Option(value)
    SwingUtilities.invokeLater(new Runnable {
      def run() {
        listeners.foreach(_(fieldValue))
      }
    })
  }
  def +=(listener: Option[T] => Unit) {
    listeners = listeners :+ listener
  }
}