package org.sade.analyzers

import org.apache.commons.math.transform.FastFourierTransformer
import scala.math._

object FrequencyEvaluator {
  def evaluateFrequency(truncData: Array[Double]) = {
    val transform = harmonicProductSpectrum(truncData)

    val amplitudes = transform.zipWithIndex.map(t => Amplitude(t._1, t._2 * 1.0 / truncData.length))
    firstHarmonicFrequency(truncData, amplitudes.drop(1))
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
    val ampToAutoCorrelation = sortedByFreq.map(a => a -> autoCorrelation(a.frequency)(truncData))
    ampToAutoCorrelation.maxBy(_._2)._1.frequency
  }

  private def autoCorrelation(frequency: Double)(truncData: Array[Double]) = {
    val window = truncData.size / 8
    val firstWindow = truncData.take(window)
    val secondWindow = truncData.drop(scala.math.round(1 / frequency.toFloat)).take(window)
    firstWindow.zip(secondWindow).map(v => v._1 * v._2).sum
  }

  case class Amplitude(amp: Double, frequency: Double)

}