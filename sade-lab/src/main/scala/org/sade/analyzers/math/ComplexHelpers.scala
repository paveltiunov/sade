package org.sade.analyzers.math

import org.apache.commons.math.complex.Complex

trait ComplexHelpers {
  implicit def toComplexWrapper(complex: Complex) = ComplexWrapper(complex)
}

case class ComplexWrapper(complex: Complex) {
  def /(v: Double) = complex.multiply(1/v)
  def *(c: Complex) = complex.multiply(c)
  def +(c: Complex) = complex.add(c)
  def -(c: Complex) = complex.subtract(c)
}