package org.sade.analyzers

import java.io.InputStream


trait AnalyzerFactory {
  def createAnalyzer(inputStream: InputStream): Analyzer
}

trait Analyzer {
  def absoluteError: Double
  def meanValue: Double
  def meanFrequency: Double
}