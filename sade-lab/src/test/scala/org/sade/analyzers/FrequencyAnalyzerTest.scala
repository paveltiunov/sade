
package org.sade.analyzers

import org.junit.{Assert, Test}
import org.hamcrest.Matchers
import scala.math._
import org.scalatest.junit.MustMatchersForJUnit

class FrequencyAnalyzerTest extends MustMatchersForJUnit {
  val PointCount = 65536

  //@Test //TODO
  def SecondaryHarmonic() {
    Assert.assertThat[java.lang.Double](EvaluateFrequencyFromFunc(i => (cos(Pi / 2 * (0.5 + sin(Pi / 8 * i))) + 10000)), Matchers.closeTo(0.0625, 1E-3));
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
