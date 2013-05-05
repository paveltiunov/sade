
package org.sade.analyzers

import org.junit.{Assert, Test}
import org.hamcrest.Matchers
import scala.math._
import org.scalatest.junit.MustMatchersForJUnit
import scala.util.Random

class FrequencyAnalyzerTest extends MustMatchersForJUnit {
  val PointCount = 65536

  @Test
  def SecondaryHarmonic() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => (cos(Pi / 2 * (0.5 + sin(Pi / 800 * i))) + 10000)), Matchers.closeTo(6.256E-4, 1E-6))
  }

  @Test
  def SecondaryHarmonic2() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => (2.5 * cos(2.5 * (1.5 + cos(2 * Pi / 200 * i + 0.7))) + 2)), Matchers.closeTo(0.005, 1E-3))
  }

  @Test
  def highHarmonicsHPS() {
    evaluateFileFrequency("120hz.bin") must be < (130.0)
  }

  @Test
  def genericHPS() {
    evaluateFileFrequency("test_00004.bin") must (be > (247.0) and be < (249.0))
  }

  @Test
  def newExps() {
    evaluateFileFrequency("2010-03-27 12_00_50.0.bin") must (be > (199.0) and be < (215.0))
  }

  @Test
  def newExps2() {
    evaluateFileFrequency("2010-03-27 11_27_46.0.bin") must (be > (199.0) and be < (215.0))
  }

  @Test
  def newExps3() {
    evaluateFileFrequency("2010-03-27 11_48_12.0.bin") must (be > (199.0) and be < (215.0))
  }

  @Test
  def oldExps() {
    evaluateFileFrequency("2008-07-26 16_00_35.25.bin") must (be > (248.0) and be < (249.0))
  }


  def evaluateFileFrequency(fixtureFile: String): Double = {
    val clazz = getClass
    val reader = new FloatReader(clazz.getResource(fixtureFile).openStream())
    val sample = reader.chunkStream.take(65536).toArray.map(_.toDouble)
    val frequency = EvaluateFrequency(sample) * 100000
    frequency
  }

  def EvaluateFrequencyFromFunc(func: Int => Double) = {
    EvaluateFrequency(CreateData(func))
  }

  def CreateData(func: Int => Double) = {
    (0 until PointCount).map(func).toArray
  }

  def EvaluateFrequency(doubles: Array[Double]) = {
    FrequencyEvaluator.evaluateFrequency(doubles)
  }
}
