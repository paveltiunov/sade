package org.sade.servlet

import org.junit.Test
import org.sade.WebServerRunner
import org.sade.upload.PointUploader
import org.sade.lab.PointSource
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}
import org.sade.model.SadeDB
import org.scalatest.junit.MustMatchersForJUnit


class PointUploadServletTest extends WebServerRunner with MustMatchersForJUnit {
  @Test
  def integration() {
    val jetty = prepareJetty
    jetty.start()

    val uploader = new PointUploader("http://localhost:8080")
    val pointSource = PointSource(
      MeasuredPointCoordinates(new Date(5000), 1, 2, 3, Directions.Backward), () => "foo".getBytes
    )
    uploader.uploadPoint(pointSource) must be (true)
    uploader.uploadPoint(pointSource) must be (false)

    inTransaction {
      from(SadeDB.pointContents) {
        p => select(p)
      }.iterator.size must be(1)
    }

    jetty.stop()
  }
}