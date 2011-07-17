package org.sade.analyzers

import org.apache.commons.math.transform.FastFourierTransformer
import scala.math._

object FrequencyEvaluator {
  def evaluateFrequency(truncData: Array[Double]) = {
    val transformation: FastFourierTransformer = new FastFourierTransformer
    val transform = transformation.transform(truncData)

    val amplitudes = transform.zipWithIndex.map(t => Amplitude(t._1.abs, 1.0 - (t._2 * 1.0 / transform.length)))
    firstHarmonicFrequency(amplitudes.drop(1))
  }

  private def firstHarmonicFrequency(amplitudes: Array[FrequencyEvaluator.Amplitude]) = {
    val maxAmplitude = amplitudes.map(_.amp).max
    val sortedByAmp = amplitudes.sortBy(_.amp).reverse
    val sortedByFreq = sortedByAmp.takeWhile(a => log(a.amp / maxAmplitude) > -1).sortBy(_.frequency)
    sortedByFreq.head.frequency
  }

  case class Amplitude(amp: Double, frequency: Double)

}