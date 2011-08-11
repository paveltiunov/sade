package org.sade.servlet

import org.sade.WebServerRunner
import org.sade.upload.PointUploader
import org.sade.lab.PointSource
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}
import org.sade.model.SadeDB
import org.scalatest.junit.MustMatchersForJUnit
import org.junit.{After, Before, Test}

class PointUploadServletTest extends WebServerRunner with MustMatchersForJUnit {
  val jetty = prepareJetty

  @Before
  def setup() {
    jetty.start()
  }

  def createPointSource(date: Date) = {
    PointSource(
      MeasuredPointCoordinates(date, 1, 2, 3, Directions.Backward), () => "foo".getBytes
    )
  }

  @Test
  def integration() {
    val uploader = new PointUploader("http://localhost:8080")
    val pointSource = createPointSource(new Date(5000))
    uploader.uploadPoint(pointSource) must be(true)
    uploader.uploadPoint(pointSource) must be(false)
    uploader.loadedIds must have size (1)

    uploader.loadedIds ++= Set(new Date(123))
    uploader.uploadPoint(createPointSource(new Date(123))) must be (false)

    inTransaction {
      from(SadeDB.pointContents) {
        p => select(p)
      }.iterator.size must be(1)
    }
  }

  @After
  def tearDown() {
    jetty.stop()
  }
}