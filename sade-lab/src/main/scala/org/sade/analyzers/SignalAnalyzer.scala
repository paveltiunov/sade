package org.sade.analyzers

import java.io.InputStream
import scala.collection.JavaConversions._
import scala.math._
import org.apache.commons.math.stat.StatUtils

class SignalAnalyzer(inputStream: InputStream, statusListener: Option[Float => Unit] = None) extends Analyzer {
  val analyzer = new JacobiAngerAnalyzer
  val floatReader = new FloatReader(inputStream, statusListener)
  val analyzeResults = estimateParameters.toArray

  def foundDeltaStream: Array[Double] = {
    analyzeResults.map(_.getParameters.getDelta).toArray
  }

  private def estimateParameters = {
    floatReader.chunkArrayStream.map(_.map(_.toDouble)).flatMap(analyzer.DivideAndAnalyze(_).toSeq)
  }

  def originalSignal = {
    floatReader.buffered.map(_.toDouble)
  }

  def estimatedSignal = {
    val max = originalSignal.max
    val min = originalSignal.min
    val amplitude = (max - min) / 2
    val center = (max + min) / 2
    analyzeResults.flatMap(r => {
      (0 until r.getPeriod).map(i => {
        amplitude * cos(r.getParameters.getOmega * cos(2 * Pi * i / r.getPeriod + r.getParameters.getPhi) + r.getParameters.getDelta) + center
      })
    }).take(originalSignal.size)
  }

  def absoluteError = {
    sqrt(StatUtils.variance(foundDeltaStream) / foundDeltaStream.length)
  }

  def meanValue = {
    StatUtils.mean(foundDeltaStream)
  }

  def meanFrequency = {
    StatUtils.mean(analyzeResults.map(1.0 / _.getPeriod)) * 100000
  }
}