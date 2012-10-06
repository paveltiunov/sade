package org.sade.analyzers.math

import scala.math._
import org.apache.commons.math.complex.Complex

object FFT extends ComplexHelpers {
  def transform(values: Array[Double], seriesNum:Int, periodFactor: Double = 1.0) = transformComplex(values.map(v => new Complex(v, 0)), seriesNum, periodFactor)
  def transformComplex(values: Array[Complex], seriesNum:Int, periodFactor: Double = 1.0) = (0 until seriesNum).map(evaluateKHarmonic(values, periodFactor)).toArray

  private def evaluateKHarmonic(values: Array[Complex], periodFactor: Double)(k: Int) =
    divideAndEvaluate(k, Array(DividedArray(values, 1, 0)), Array(new Complex(1,0)), periodFactor)(0) / (values.length / periodFactor)

  private def divideAndEvaluate(k: Int, values:Array[DividedArray], exps: Array[Complex], periodFactor: Double): Array[Complex] = {
    val nearestDivider = exps.length
    if (values(0).length == 1) values.map(v => v(0))
    else {
      val nextExps = getNextExps(k, values(0).length/nearestDivider, periodFactor)
      val nextValues = values.flatMap(v => (0 until nearestDivider).map(dividedArray(nearestDivider, _, v)))
      val divided = divideAndEvaluate(k, nextValues, nextExps, periodFactor);
      (0 until values.length)
        .map(j => (0 until nearestDivider).map(i => divided(i + j*nearestDivider)*exps(i)).reduceLeft(_ + _))
        .toArray
    }
  }

  private def getNextExps(k: Int, length: Int, periodFactor: Double) = {
    val nearestDivider = findNearestDividerOf(length)
    (0 until nearestDivider).map(i => new Complex(0, -Pi*2*i*k/length * periodFactor).exp).toArray
  }

  private def dividedArray(divider:Int, i: Int, values:DividedArray) = {
    values.divide(divider, i)
  }

  case class DividedArray(src: Array[Complex], divider: Int, i: Int) {
    def apply(j: Int) = src(j*divider + i)

    def length = src.length / divider

    def divide(nextDivider: Int, nextI: Int) = {
      copy(divider = nextDivider * divider, i = nextI * divider + i)
    }
  }

  private def findNearestDividerOf(n:Int) = {
    if (n < 4) n
    else (2 to n).find(n % _ == 0).get
  }
}