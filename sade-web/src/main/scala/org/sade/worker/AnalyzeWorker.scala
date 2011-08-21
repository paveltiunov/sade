package org.sade.worker

import org.sade.analyzers.AnalyzerFactory
import org.squeryl.PrimitiveTypeMode
import java.io.ByteArrayInputStream
import java.util.Date
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import org.sade.model.{Point, AnalyzeToken, AnalyzeResult, SadeDB}


class AnalyzeWorker(analyzerFactory: AnalyzerFactory) extends PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def resultDoNotExists(c: Point) = {
    notExists(from(SadeDB.analyzeResults) {
      r => where(r.id === c.id) select (r)
    })
  }

  def findNeverAnalyzedPointAndInsertToken(now: Timestamp): Option[Point] = {
    from(SadeDB.points)(c =>
      where(
        resultDoNotExists(c) and
          notExists(from(SadeDB.analyzeTokens) {
            t => where(t.id === c.id) select (t)
          })
      )
        select (c)
    ).forUpdate.headOption.map(p => {
      SadeDB.analyzeTokens.insert(AnalyzeToken(p.id, now))
      p
    })
  }

  def findFailedAnalyzePointAndUpdateToken(now: Timestamp): Option[Point] = {
    from(SadeDB.points, SadeDB.analyzeTokens)((content, token) => {
      where((content.id === token.id) and resultDoNotExists(content) and (token.analyzeStarted lt new Timestamp(new Date().getTime - 5 * 60 * 1000))) select ((token, content))
    }).forUpdate.headOption.map {
      case (token, content) => {
        SadeDB.analyzeTokens.update(token.copy(analyzeStarted = now))
        content
      }
    }
  }

  def analyzeNextPoint() = {
    val now = new Timestamp(new Date().getTime)
    val notAnalyzedPoint = inTransaction {
      findFailedAnalyzePointAndUpdateToken(now).orElse(findNeverAnalyzedPointAndInsertToken(now))
    }
    notAnalyzedPoint.foreach(point => {
      val timeMillis = System.currentTimeMillis()
      logger.info("Start work on point timed at: " + point.id)
      val content = inTransaction(point.content)
      val analyzer = analyzerFactory.createAnalyzer(new ByteArrayInputStream(content))
      inTransaction {
        SadeDB.analyzeResults.insert(AnalyzeResult(point.id, analyzer.meanValue, analyzer.absoluteError, analyzer.meanFrequency))
      }
      logger.info("Finished work on point timed at: " + point.id + ", elapsed time: " + (System.currentTimeMillis() - timeMillis) + "ms")
    })
    notAnalyzedPoint.isDefined
  }
}