package org.sade.analyzers

import org.junit.{Assert, Test}
import org.hamcrest.Matchers
import scala.math._

class FrequencyAnalyzerTest {
  val PointCount = 65536;

  @Test
  def Gutter() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => sin(Pi / 2 * i)), Matchers.closeTo(0.25, 1E-3));
  }

  @Test
  def WithConst() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => (cos(Pi / 2 * i) + 10000)), Matchers.closeTo(0.25, 1E-3));
  }

  @Test
  def SecondaryHarmonic() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => (cos(Pi / 2 * (0.5 + sin(Pi / 8 * i))) + 10000)), Matchers.closeTo(0.0625, 1E-3));
  }

  def EvaluateFrequencyFromFunc(func: Int => Double) = {
    EvaluateFrequency(CreateData(func));
  }

  def CreateData(func: Int => Double) = {
    (0 until PointCount).map(func).toArray
  }

  def EvaluateFrequency(doubles: Array[Double]) = {
    FrequencyEvaluator.evaluateFrequency(doubles)
  }
}
