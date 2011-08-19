package org.sade.view

import org.sade.StubWebServerRunner
import org.sade.upload.PointUploader
import org.scalatest.junit.MustMatchersForJUnit
import org.squeryl.PrimitiveTypeMode
import org.sade.model.{PointContent, SadeDB}
import java.sql.Timestamp
import org.sade.starcoords.Directions
import org.junit.{Ignore, Test}

class AllPointIdViewTest extends StubWebServerRunner with MustMatchersForJUnit with PrimitiveTypeMode {
  @Test
  @Ignore("support multiple jetty stub tests")
  def gutter() {
    inTransaction {
      SadeDB.pointContents.insert(PointContent("foo".getBytes, new Timestamp(123), 1, 1, 1, Directions.Forward))
    }
    val uploader = new PointUploader("http://localhost:8080")
    uploader.updateLoadedIds()
    uploader.loadedIds must have size (1)
  }
}