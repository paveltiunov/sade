package org.sade.servlet

import org.sade.WebServerRunner
import org.sade.upload.PointUploader
import org.sade.lab.PointSource
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}
import org.sade.model.SadeDB
import org.scalatest.junit.MustMatchersForJUnit
import org.junit.{Assert, After, Before, Test}

class PointUploadServletTest extends WebServerRunner with MustMatchersForJUnit {
  val jetty = prepareJetty()
  val bytes = (0 until 540000).map(_.toString).mkString(" ").getBytes

  @Before
  def setup() {
    WorkerInitServlet.inTest = true
    jetty.start()
  }

  def createPointSource(date: Date) = {
    PointSource(
      MeasuredPointCoordinates(date, 1, 2, 3, Directions.Backward), () => {
        bytes
      }
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

    val actualBytes = inTransaction {
      from(SadeDB.pointContents) {
        p => select(p)
      }.single.content
    }

    actualBytes.size must be (bytes.size)

    bytes.zip(actualBytes).zipWithIndex.foreach{case ((b1, b2), i) => {
      Assert.assertEquals("at index " + i, b1, b2)
    }}
  }

  @After
  def tearDown() {
    jetty.stop()
  }
}