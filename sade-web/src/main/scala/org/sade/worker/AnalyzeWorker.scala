package org.sade.worker

import org.sade.analyzers.{SignalAnalyzer, AnalyzerFactory}
import org.squeryl.PrimitiveTypeMode._
import java.io.{InputStream, ByteArrayInputStream}
import java.util.Date
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import org.sade.model.{Point, AnalyzeToken, AnalyzeResult, SadeDB}
import akka.actor.{Address, OneForOneStrategy, Props, Actor}
import akka.routing.{RoundRobinRouter, RemoteRouterConfig, SmallestMailboxRouter}

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

  var timeouted = Set[Timestamp]()
  var analyzing = Seq[Timestamp]()
  var currentHosts = Set[RegisterHost]()
  var analyzer = createAnalyzer
  var analyzeStarted = Map[Timestamp, Long]()

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

case class AnalyzePoint(id: Timestamp)

case class CommitResult(result: AnalyzeResult)

case class PointTimeout(id: Timestamp)

class PointTimeoutException extends RuntimeException

class SinglePointAnalyzer extends Actor {
  val analyzerFactory = new AnalyzerFactory {
    def createAnalyzer(inputStream: InputStream) = new SignalAnalyzer(inputStream)
  }

  val logger = LoggerFactory.getLogger(getClass)

  protected def receive = {
    case AnalyzePoint(id: Timestamp) => {
      val timeMillis = System.currentTimeMillis()
      logger.info("Start work on point timed at: " + id)
      val content = inTransaction(Point.unzippedContentBy(id))
      val analyzer = new SignalAnalyzer(new ByteArrayInputStream(content), Some(f => {
        if (System.currentTimeMillis() - timeMillis > 1000 * 300) {
          sender ! PointTimeout(id)
          throw new PointTimeoutException
        }
      }))
      sender ! CommitResult(AnalyzeResult(id, analyzer.meanValue, analyzer.absoluteError, analyzer.meanFrequency))
      logger.info("Finished work on point timed at: " + id + ", elapsed time: " + (System.currentTimeMillis() - timeMillis) + "ms")
    }
  }
}