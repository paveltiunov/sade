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
    analyzeResults.filterNot(JacobiAngerAnalyzer.isOverErrorThreshold).map(_.getParameters.getDelta).toArray
  }

  private def estimateParameters = {
    floatReader.chunkArrayStream.map(_.map(_.toDouble)).flatMap(analyzer.DivideAndAnalyze(_).toSeq)
  }

  def originalSignal = {
    floatReader.buffered.map(_.toDouble)
  }

  def estimatedSignal = {
    analyzeResults.flatMap(r => {
      (0 until r.getPeriod).map(i => {
        r.getAmplitude * cos(r.getParameters.getOmega * cos(2 * Pi * i / r.getPeriod + r.getParameters.getPhi) + r.getParameters.getDelta) + r.getCenter
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