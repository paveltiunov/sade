package org.sade.view

import org.sade.upload.PointUploader
import org.scalatest.junit.MustMatchersForJUnit
import org.squeryl.PrimitiveTypeMode
import org.junit.{Ignore, Test}
import org.sade.{Fixtures, StubWebServerRunner}

class AllPointIdViewTest extends StubWebServerRunner with MustMatchersForJUnit with PrimitiveTypeMode with Fixtures {
  @Test
  @Ignore("support multiple jetty stub tests")
  def gutter() {
    inTransaction {
      setupPointContentFixture()
    }
    val uploader = new PointUploader("http://localhost:8080")
    uploader.updateLoadedIds()
    uploader.loadedIds must have size (1)
  }
}