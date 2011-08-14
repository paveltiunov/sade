package org.sade.worker

import java.io.InputStream
import org.sade.analyzers.{SignalAnalyzer, AnalyzerFactory}
import scala.concurrent.ops._
import org.slf4j.LoggerFactory

class MainWorker {
  val logger = LoggerFactory.getLogger(getClass)
  val analyzerFactory = new AnalyzerFactory {
    def createAnalyzer(inputStream: InputStream) = new SignalAnalyzer(inputStream)
  }

  def startWorking() {
    (0 until Runtime.getRuntime.availableProcessors()).foreach(_ => startNextWorker())
  }

  def startNextWorker() {
    spawn {
      try {
        if (!new AnalyzeWorker(analyzerFactory).analyzeNextPoint()) Thread.sleep(10000)
      } catch {
        case e: Exception => logger.error("Error during analyze work", e)
      }
      startNextWorker()
    }
  }
}