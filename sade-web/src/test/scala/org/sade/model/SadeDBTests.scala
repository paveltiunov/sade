package org.sade.model

import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.H2Adapter
import org.junit.{Before, Test}
import java.sql.{Date, DriverManager}
import org.sade.starcoords.Directions
import java.util.UUID
import org.scalatest.junit.MustMatchersForJUnit
import org.squeryl.PrimitiveTypeMode._

class SadeDBTests extends MustMatchersForJUnit {
  @Before
  def setupInMemoryDB() {
    val session = Session.create(DriverManager.getConnection("jdbc:h2:mem:"), new H2Adapter)
    session.bindToCurrentThread
    SadeDB.create
  }

  @Test
  def gutter() {
    val pointContent = PointContent("foo".getBytes, new Date(123), 1, 2, 3, Directions.Backward)
    SadeDB.pointContents.insert(pointContent)

    SadeDB.pointContents.lookup(pointContent.id).get must be (pointContent)
  }
}