package org.sade.worker

import org.mockito.Mockito
import org.sade.analyzers.AnalyzerFactory
import org.squeryl.PrimitiveTypeMode
import org.scalatest.junit.MustMatchersForJUnit
import java.sql.Timestamp
import org.junit.Test
import org.sade.model.{AnalyzeToken, SadeDB, MemoryDBTest}
import java.util.Date
import org.sade.Fixtures


class AnalyzeWorkerTest extends MemoryDBTest with PrimitiveTypeMode with MustMatchersForJUnit with Fixtures {
 /* val analyzerFactory = Mockito.mock(classOf[AnalyzerFactory], Mockito.RETURNS_DEEP_STUBS)

  val worker = new AnalyzeWorker()

  def checkResultCount(c: Int = 1) {
    inTransaction {
      from(SadeDB.analyzeResults) {
        r => compute(count())
      }.head.measures must be(c)
    }
  }


  def setup() {
    setupPointContentFixture()
  }

  @Test
  def gutter() {
    setup()
    worker.analyzeNextPoint() must be (true)
    checkResultCount()
    worker.analyzeNextPoint() must be (false)
    checkResultCount()
  }

  @Test
  def alreadyAnalyzing() {
    setup()
    SadeDB.analyzeTokens.insert(AnalyzeToken(new Timestamp(123), new Timestamp(new Date().getTime)))
    worker.analyzeNextPoint() must be (false)
    checkResultCount(0)
  }

  @Test
  def analyzedButFailed() {
    setup()
    SadeDB.analyzeTokens.insert(AnalyzeToken(new Timestamp(123), new Timestamp(new Date().getTime - 6 * 60 * 1000)))
    worker.analyzeNextPoint() must be (true)
    inTransaction {
      SadeDB.analyzeTokens.where(_.analyzeStarted > new Timestamp(new Date().getTime - 2000)).iterator.size must be (1)
    }
    checkResultCount(1)
  }*/
}