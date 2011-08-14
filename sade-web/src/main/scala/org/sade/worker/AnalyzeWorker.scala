package org.sade.worker

import org.sade.analyzers.AnalyzerFactory
import org.squeryl.PrimitiveTypeMode
import java.io.ByteArrayInputStream
import java.util.Date
import java.sql.Timestamp
import org.sade.model.{AnalyzeToken, AnalyzeResult, SadeDB}


class AnalyzeWorker(analyzerFactory: AnalyzerFactory) extends PrimitiveTypeMode {
  def analyzeNextPoint() = {
    val fiveMinutesAgo = new Timestamp(new Date().getTime - 5 * 60 * 1000)
    val now = new Timestamp(new Date().getTime)
    val notAnalyzedPoint = inTransaction {
      val notAnalyzedPoint = from(SadeDB.pointContents) ( c =>
        where(
          notExists(from(SadeDB.analyzeResults) {r => where(r.id === c.id) select(r)}) and
          notExists(from(SadeDB.analyzeTokens) {t => where(t.id === c.id) select(t)})
        )
          select(c)
      ).headOption

      notAnalyzedPoint.foreach(p => SadeDB.analyzeTokens.insert(AnalyzeToken(p.id, now)))

      notAnalyzedPoint.orElse(from(SadeDB.pointContents, SadeDB.analyzeTokens)((content, token) => {
        where((content.id === token.id) and (token.analyzeStarted lt fiveMinutesAgo)) select ((token,content))
      }).headOption.map {
        case (token, content) => {
          SadeDB.analyzeTokens.update(token.copy(analyzeStarted = now))
          content
        }
      })
    }
    notAnalyzedPoint.foreach(point => {
      val analyzer = analyzerFactory.createAnalyzer(new ByteArrayInputStream(point.content))
      inTransaction {
        SadeDB.analyzeResults.insert(AnalyzeResult(point.id, analyzer.meanValue, analyzer.absoluteError, analyzer.meanFrequency))
      }
    })
    notAnalyzedPoint.isDefined
  }
}