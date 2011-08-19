package org.sade.worker

import org.mockito.Mockito
import org.sade.analyzers.AnalyzerFactory
import org.squeryl.PrimitiveTypeMode
import org.scalatest.junit.MustMatchersForJUnit
import java.sql.Timestamp
import org.sade.starcoords.Directions
import org.junit.{Before, Test}
import org.sade.model.{AnalyzeToken, PointContent, SadeDB, MemoryDBTest}
import java.util.Date


class AnalyzeWorkerTest extends MemoryDBTest with PrimitiveTypeMode with MustMatchersForJUnit {
  val analyzerFactory = Mockito.mock(classOf[AnalyzerFactory], Mockito.RETURNS_DEEP_STUBS)

  val worker = new AnalyzeWorker(analyzerFactory)

  def checkResultCount(c: Int = 1) {
    inTransaction {
      from(SadeDB.analyzeResults) {
        r => compute(count())
      }.head.measures must be(c)
    }
  }


  @Before
  def setup() {
    inTransaction {
      SadeDB.pointContents.insert(PointContent("foo".getBytes, new Timestamp(123), 1, 2, 3, Directions.Forward))
    }
  }

  @Test
  def gutter() {
    worker.analyzeNextPoint() must be (true)
    checkResultCount()
    worker.analyzeNextPoint() must be (false)
    checkResultCount()
  }

  @Test
  def alreadyAnalyzing() {
    inTransaction {
      SadeDB.analyzeTokens.insert(AnalyzeToken(new Timestamp(123), new Timestamp(new Date().getTime)))
    }
    worker.analyzeNextPoint() must be (false)
    checkResultCount(0)
  }

  @Test
  def analyzedButFailed() {
    inTransaction {
      SadeDB.analyzeTokens.insert(AnalyzeToken(new Timestamp(123), new Timestamp(new Date().getTime - 6 * 60 * 1000)))
    }
    worker.analyzeNextPoint() must be (true)
    inTransaction {
      SadeDB.analyzeTokens.where(_.analyzeStarted > new Timestamp(new Date().getTime - 2000)).iterator.size must be (1)
    }
    checkResultCount(1)
  }
}