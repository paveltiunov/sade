package org.sade.worker

import org.sade.analyzers.{AnalyzeTimeoutException, SignalAnalyzer, AnalyzerFactory}
import org.squeryl.PrimitiveTypeMode._
import java.io.{InputStream, ByteArrayInputStream}
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import org.sade.model.{PointKeyed, Point, AnalyzeResult, SadeDB}
import akka.actor.{Address, Props, Actor}
import akka.routing.{RoundRobinRouter, RemoteRouterConfig}

class AnalyzeWorker extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  def resultDoNotExists(c: Point) = {
    notExists(from(SadeDB.analyzeResults) {
      r => where(r.id === c.id) select (r)
    })
  }

  def findPointsWithoutResult(expName: Option[String] = None): Seq[Point] = inTransaction {
    from(SadeDB.points)(c =>
      where(resultDoNotExists(c) and expName.map(c.expName === _).getOrElse(1 === 1)) select (c)
    ).toList
  }

  var timeouted = Set[PointKeyed.Key]()
  var analyzing = Seq[PointKeyed.Key]()
  var currentHosts = Set[RegisterHost]()
  var analyzer = createAnalyzer
  var analyzeStarted = Map[PointKeyed.Key, Long]()
  var tries = Map[PointKeyed.Key, Int]().withDefault(_ => 0)

  private def createAnalyzer = {
    if (currentHosts.isEmpty) {
      context.actorOf(Props[SinglePointAnalyzer])
    } else context.actorOf(
      Props[SinglePointAnalyzer]
        .withRouter(RemoteRouterConfig(RoundRobinRouter(nrOfInstances = workersNum), currentHosts.map(h => Address("akka", context.system.name, h.hostName, h.port))))
    )
  }

  private def sendAnalyzePoints() {
    analyzeStarted = analyzeStarted.filterNot(kv => (System.currentTimeMillis() - kv._2) > 1000 * 600)
    val toSend = analyzing.take(workersNum - analyzeStarted.size)
    logger.info("Sending tasks: %s".format(toSend))
    toSend.foreach(id => analyzer ! AnalyzePoint(id))
    toSend.foreach(id => tries += id -> (tries(id) + 1))
    analyzing = analyzing.filterNot(toSend.contains)
    analyzeStarted ++= toSend.map(_ -> System.currentTimeMillis()).toMap
  }


  def workersNum: Int = {
    currentHosts.size * Runtime.getRuntime.availableProcessors()
  }

  protected def receive = {
    case UpdateHosts(hosts) => {
      currentHosts = hosts
      logger.info("Updating hosts: " + currentHosts)
      analyzer = createAnalyzer
    }
    case StartAllNotStarted => startAnalyzeForExp(None)
    case StartExp(expName) => startAnalyzeForExp(Some(expName))
    case StopExp(expName) => {
      val withoutResult = findPointsWithoutResult(Some(expName)).map(_.id).toSet
      analyzing = analyzing.filterNot(withoutResult.contains)
    }
    case CommitResult(analyzeResult) => {
      logger.info("Receive commit result: " + analyzeResult)
      inTransaction {
        SadeDB.analyzeResults.insertOrUpdate(analyzeResult)
      }
      analyzeStarted -= analyzeResult.id
      sendAnalyzePoints()
      logger.info("Current analyze size: " + analyzing.size)
    }
    case PointTimeout(id) => {
      logger.info("Receive timeout: " + id)
      analyzeStarted -= id
      timeouted += id
      sendAnalyzePoints()
    }
    case GetAnalyzeStatus => sender ! AnalyzeStatus(timeouted, analyzing.toSet, analyzeStarted, tries)
  }

  def startAnalyzeForExp(expName: Option[String]) {
    val withoutResult = findPointsWithoutResult(expName)
    val toAnalyze = withoutResult.map(_.id).toSet -- analyzing -- timeouted
    logger.info("Starting all not started points. Point to be analyzed: " + toAnalyze.size)
    analyzing ++= toAnalyze
    logger.info("Current analyze size: " + analyzing.size)
    sendAnalyzePoints()
  }
}

case object StartAllNotStarted

case class StartExp(expName: String)

case class StopExp(expName: String)

case class UpdateHosts(hosts: Set[RegisterHost])

case class AnalyzePoint(id: PointKeyed.Key)

case class CommitResult(result: AnalyzeResult)

case class PointTimeout(id: PointKeyed.Key)

case object GetAnalyzeStatus

case class AnalyzeStatus(timeouted: Set[PointKeyed.Key], analyzing: Set[PointKeyed.Key], analyzeStarted: Map[PointKeyed.Key, Long], tries: Map[PointKeyed.Key, Int])

class PointTimeoutException extends RuntimeException

class SinglePointAnalyzer extends Actor {
  val analyzerFactory = new AnalyzerFactory {
    def createAnalyzer(inputStream: InputStream) = new SignalAnalyzer(inputStream)
  }

  val logger = LoggerFactory.getLogger(getClass)

  protected def receive = {
    case AnalyzePoint(id: PointKeyed.Key) => {
      val timeMillis = System.currentTimeMillis()
      logger.info("Start work on point timed at: " + id)
      val content = inTransaction(Point.unzippedContentBy(id))
      try {
        val analyzer = new SignalAnalyzer(new ByteArrayInputStream(content), timeoutTime = Some(System.currentTimeMillis() + 1000 * 300))
        sender ! CommitResult(AnalyzeResult(id, analyzer.meanValue, analyzer.absoluteError, analyzer.meanFrequency))
        logger.info("Finished work on point timed at: " + id + ", elapsed time: " + (System.currentTimeMillis() - timeMillis) + "ms")
      } catch {
        case e: AnalyzeTimeoutException => {
          sender ! PointTimeout(id)
          throw new PointTimeoutException
        }
      }
    }
  }
}