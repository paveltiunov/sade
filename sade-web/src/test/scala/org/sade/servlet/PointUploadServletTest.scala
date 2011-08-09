package org.sade.servlet

import org.junit.Test
import org.sade.WebServerRunner
import org.sade.upload.PointUploader
import org.sade.lab.PointSource
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}


class PointUploadServletTest extends WebServerRunner {
  @Test
  def integration() {
    val jetty = prepareJetty
    jetty.start()

    val uploader = new PointUploader
    uploader.uploadPoint(PointSource(
      MeasuredPointCoordinates(new Date(123), 1, 2, 3, Directions.Backward), () => "foo".getBytes
    ))

    jetty.stop()
  }
}