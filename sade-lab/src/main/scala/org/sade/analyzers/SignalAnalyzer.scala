package org.sade.analyzers

import java.io.InputStream
import scala.collection.JavaConversions._


class SignalAnalyzer(inputStream: InputStream) {
  val analyzer = new JacobiAngerAnalyzer
  val floatReader = new FloatReader(inputStream)

  def foundDeltaStream: Stream[Double] = {
    floatReader.chunkArrayStream.map(_.map(_.toDouble)).flatMap(analyzer.DivideAndAnalyze(_).toSeq).map(_.getParameters.getDelta)
  }
}