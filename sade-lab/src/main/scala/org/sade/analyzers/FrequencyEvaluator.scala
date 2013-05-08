package org.sade.analyzers

import org.apache.commons.math.transform.FastFourierTransformer
import scala.math._
import scala.annotation.tailrec

object FrequencyEvaluator {
  def evaluateFrequency(truncData: Array[Double]) = {
    val transform = harmonicProductSpectrum(truncData)

    val amplitudes = transform.zipWithIndex.map(t => Amplitude(t._1, t._2 * 1.0 / truncData.length))
    firstHarmonicFrequency(truncData, amplitudes.drop(10))
  }

  def harmonicProductSpectrum(truncData: Array[Double]) = {
    val spectrum = powerDensity(truncData)
    val range = 1 until 6
    val spectralDensities = range.map(factor => downSample(spectrum, factor))
    harmonicProduct(spectralDensities)
  }

  def powerDensity(truncData: Array[Double]) = {
    val transformer = new FastFourierTransformer
    transformer.transform(truncData).map(_.abs())
  }

  private def downSample(sample: Array[Double], factor: Int): Array[Double] = {
    val size = sample.length / factor
    (0 until size).map(i => sample(i * factor)).toArray
  }

  private def harmonicProduct(spectralDensities: Traversable[Array[Double]]): Array[Double] = {
    val minLength = spectralDensities.map(_.length).min
    (0 until minLength).map(i => spectralDensities.map(_(i)).sum).toArray
  }

  private def firstHarmonicFrequency(truncData: Array[Double], amplitudes: Array[FrequencyEvaluator.Amplitude]) = {
    val maxAmplitude = amplitudes.map(_.amp).max
    val sortedByAmp = amplitudes.sortBy(_.amp).reverse
    val sortedByFreq = sortedByAmp.takeWhile(a => log(a.amp / maxAmplitude) > -1).sortBy(_.frequency)
    val ampToFrequency = sortedByFreq.map(a =>
      a -> refineFrequency(truncData, a.frequency)
    )
    ampToFrequency.minBy(v => scala.math.abs(v._2 - v._1.frequency))._2
  }

  @tailrec
  private def refineFrequency(sample: Array[Double], frequency: Double, iteration: Int = 0): Double = {
    val period = (1 / frequency).toFloat
    if (iteration >= 1000) {
      frequency
    } else {
      refineFrequency(sample, 1.0 / JacobiAngerAnalyzer.refinePeriod(sample, period), iteration +1)
    }
  }

  case class Amplitude(amp: Double, frequency: Double)

}