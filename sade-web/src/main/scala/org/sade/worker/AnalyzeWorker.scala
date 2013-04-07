package org.sade.worker

import org.sade.analyzers.{SignalAnalyzer, AnalyzerFactory}
import org.squeryl.PrimitiveTypeMode._
import java.io.{InputStream, ByteArrayInputStream}
import java.util.Date
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import org.sade.model.{Point, AnalyzeToken, AnalyzeResult, SadeDB}
import akka.actor.{Address, OneForOneStrategy, Props, Actor}
import akka.routing.{RemoteRouterConfig, SmallestMailboxRouter}

class AnalyzeWorker extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  def resultDoNotExists(c: Point) = {
    notExists(from(SadeDB.analyzeResults) {
      r => where(r.id === c.id) select (r)
    })
  }

  def findPointsWithoutResult: Seq[Point] = {
    from(SadeDB.points)(c =>
      where(resultDoNotExists(c)) select (c)
    ).toList
  }

  var timeouted = Set[Timestamp]()
  var analyzing = Seq[Timestamp]()
  var currentHosts = Set[String]()
  var analyzer = createAnalyzer
  var analyzeStarted = Map[Timestamp, Long]()

  private def createAnalyzer = {
    context.actorOf(
      Props[SinglePointAnalyzer]
        .withRouter(RemoteRouterConfig(SmallestMailboxRouter(nrOfInstances = workersNum), currentHosts.map(h => Address("akka", context.system.name, h, 2552)))))
  }

  private def sendAnalyzePoints() {
    analyzeStarted = analyzeStarted.filter(kv => (System.currentTimeMillis() - kv._2) > 1000 * 600)
    val toSend = analyzing.take(workersNum - analyzeStarted.size)
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
      self ! StartAllNotStarted
    }
    case StartAllNotStarted => {
      val withoutResult = inTransaction(findPointsWithoutResult)
      val toAnalyze = withoutResult.map(_.id).toSet -- analyzing -- timeouted
      logger.info("Starting all not started points. Point to be analyzed: " + toAnalyze.size)
      analyzing ++= toAnalyze
      logger.info("Current analyze size: " + analyzing.size)
      sendAnalyzePoints()
    }
    case CommitResult(analyzeResult) => {
      logger.info("Receive commit result: " + analyzeResult)
      inTransaction {
        SadeDB.analyzeResults.insert(analyzeResult)
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
}

case object StartAllNotStarted

case class UpdateHosts(hosts: Set[String])

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