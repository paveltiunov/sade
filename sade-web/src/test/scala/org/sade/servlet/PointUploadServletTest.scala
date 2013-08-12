package org.sade.servlet

import org.sade.StubWebServerRunner
import org.sade.upload.PointUploader
import org.sade.lab.PointSource
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}
import org.scalatest.junit.MustMatchersForJUnit
import org.junit.{Assert, Test}
import org.sade.model.{SadeDB}

class PointUploadServletTest extends StubWebServerRunner with MustMatchersForJUnit {
  val bytes = (0 until 540000).map(_.toString).mkString(" ").getBytes

  def createPointSource(date: Date) = {
    PointSource(
      MeasuredPointCoordinates(date, 1, 2, 3, Directions.Backward), "foo", "channel0", () => {
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

    uploader.loadedIds ++= Set(new Date(123) -> "channel0")
    uploader.uploadPoint(createPointSource(new Date(123))) must be (false)

    val actualBytes = inTransaction {
      from(SadeDB.points) {
        p => select(p)
      }.single.content
    }

    actualBytes.size must be (bytes.size)

    bytes.zip(actualBytes).zipWithIndex.foreach{case ((b1, b2), i) => {
      Assert.assertEquals("at index " + i, b1, b2)
    }}
  }
}