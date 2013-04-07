package org.sade.worker

import java.io.InputStream
import org.sade.analyzers.{SignalAnalyzer, AnalyzerFactory}
import scala.concurrent.ops._
import org.slf4j.LoggerFactory
import akka.actor.{Props, Actor}
import akka.util.duration._

class MainWorker extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  val analyzeWorker = context.actorOf(Props[AnalyzeWorker])
  var registeredHosts = Set[String]()

  protected def receive = {
    case StartAnalyze => context.system.scheduler.schedule(0 minutes, 10 minutes, analyzeWorker, StartAllNotStarted)
    case RegisterHost(name) => {
      if (registeredHosts + name != registeredHosts) {
        registeredHosts += name
        analyzeWorker ! UpdateHosts(registeredHosts)
      }
    }
  }
}

case object StartAnalyze

case class RegisterHost(hostName: String)