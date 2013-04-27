package org.sade.worker

import java.io.InputStream
import org.sade.analyzers.{SignalAnalyzer, AnalyzerFactory}
import scala.concurrent.ops._
import org.slf4j.LoggerFactory
import akka.actor.{Cancellable, Props, Actor}
import akka.util.duration._

class MainWorker extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  val analyzeWorker = context.actorOf(Props[AnalyzeWorker], "worker")
  var registeredHosts = Set[RegisterHost]()
  var expToCancellable = Map[String, Cancellable]()

  protected def receive = {
    case s @ StartExp(expName) => {
      expToCancellable += expName -> context.system.scheduler.schedule(0 minutes, 10 minutes, analyzeWorker, s)
    }
    case s @ StopExp(expName) => {
      expToCancellable.get(expName).foreach(_.cancel())
      expToCancellable -= expName
      analyzeWorker ! s
    }
    case StartAnalyze => context.system.scheduler.schedule(0 minutes, 10 minutes, analyzeWorker, StartAllNotStarted)
    case name: RegisterHost => {
      logger.info("Register host: %s".format(name))
      if (registeredHosts + name != registeredHosts) {
        registeredHosts += name
        analyzeWorker ! UpdateHosts(registeredHosts)
      }
    }
    case GetAnalyzeState(expName) => sender ! AnalyzeState(expToCancellable.contains(expName))
    case m @ GetAnalyzeStatus => analyzeWorker forward(m)
  }
}

case object StartAnalyze

case class RegisterHost(hostName: String, port: Int)

case class GetAnalyzeState(expName: String)

case class AnalyzeState(analyzing: Boolean)