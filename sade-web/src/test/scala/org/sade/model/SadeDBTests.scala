package org.sade.model

import org.squeryl.Session
import org.squeryl.adapters.H2Adapter
import org.scalatest.junit.MustMatchersForJUnit
import org.squeryl.PrimitiveTypeMode._
import java.sql.{Timestamp, DriverManager}
import org.sade.starcoords.{MeasuredPointCoordinates, SkyMapPoint, Directions}
import java.util.UUID
import org.junit.{After, Before, Test}

class SadeDBTests extends MustMatchersForJUnit with MemoryDBTest {
  val pointContent = PointContent("foo".getBytes, new Timestamp(123), 1, 2, 3, Directions.Backward)

  @Test
  def gutter() {
    SadeDB.pointContents.insert(pointContent)

    SadeDB.pointContents.lookup(pointContent.id).get must be (pointContent)
  }

  @Test
  def skyMapPoints() {
    SadeDB.pointContents.insert(pointContent)
    val result = AnalyzeResult(pointContent.id, 1, 2, 3)
    SadeDB.analyzeResults.insert(result)

    SadeDB.skyMapPoints.head must be (
      SkyMapPoint(
        MeasuredPointCoordinates(
          pointContent.id,
          pointContent.pointIndex,
          pointContent.pointCount,
          pointContent.dirIndex,
          pointContent.direction
        ),
        0,
        result.meanValue
      )
    )
  }
}

trait MemoryDBTest {
  val session = Session.create(DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID().toString), new H2Adapter)

  @Before
  def setupInMemoryDB() {
    session.bindToCurrentThread
    SadeDB.create
  }

  @After
  def tearDownDB() {
    session.unbindFromCurrentThread
  }
}