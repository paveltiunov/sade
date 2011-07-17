package org.sade.analyzers

import java.io.InputStream
import scala.collection.JavaConversions._
import scala.math._
import javax.management.remote.rmi._RMIConnection_Stub

class SignalAnalyzer(inputStream: InputStream) {
  val analyzer = new JacobiAngerAnalyzer
  val floatReader = new FloatReader(inputStream)
  val analyzeResults = estimateParameters.toArray

  def foundDeltaStream: Seq[Double] = {
    analyzeResults.map(_.getParameters.getDelta)
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
}