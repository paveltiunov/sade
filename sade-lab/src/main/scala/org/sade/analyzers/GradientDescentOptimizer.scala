package org.sade.analyzers

import collection.mutable.Buffer
import annotation.tailrec

class GradientDescentOptimizer(val precision: Double, val function: GradientFunction) extends DescentOptimizer {
  def findNextParams(params: Array[Double], gradient: Array[Double]): Array[Double] = {
    params.zip(gradient).map(t => {
      t._1 - t._2 * stepSize
    })
  }
}

class FastestGradientDescentOptimizer(val precision: Double, val function: GradientFunction) extends DescentOptimizer {
  def findNextParams(params: Array[Double], gradient: Array[Double]): Array[Double] = {
    @tailrec def nextParam(prev: Array[Double], step: Double, errorValue: Double): Array[Double] = {
      val next = params.zip(gradient).map(t => t._1 - t._2 * step)
      val nextError = function.Value(next)
      if (nextError < errorValue) nextParam(next, step * 2, nextError)
      else prev
    }
    nextParam(params, stepSize, function.Value(params))
  }
}

trait DescentOptimizer extends GradientOptimizer {
  def precision: Double
  def function: GradientFunction
  val stepSize = 0.05

  val trackValueBuffer = Buffer[Array[Double]]()

  def findNextParams(params: Array[Double], gradient: Array[Double]): Array[Double]

  def Optimize(initial: Array[Double]) = {
    @tailrec def optimizeRecursive(params: Array[Double], optimizeCount: Int = 0): Array[Double] = {
      trackValueBuffer += params
      val gradient = function.Gradient(params)
      if (gradient.exists(_.isNaN)) {
        optimizeRecursive(params.map(_ + stepSize), optimizeCount + 1)
      } else {
        if (BisectionGradientOptimizer.hasConverged(gradient, precision) || optimizeCount > 48) params
        else {
          optimizeRecursive(findNextParams(params, gradient), optimizeCount + 1)
        }
      }
    }
    optimizeRecursive(initial)
  }
}