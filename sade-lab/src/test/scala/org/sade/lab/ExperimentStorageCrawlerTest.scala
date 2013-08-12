package org.sade.lab

import org.junit.Test
import java.io.{ByteArrayInputStream, InputStream}
import org.scalatest.junit.MustMatchersForJUnit
import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}

class ExperimentStorageCrawlerTest extends MustMatchersForJUnit {
  @Test
  def gutter() {
    val pointSource: PointSource = ExperimentStorageCrawler.crawl(
      StubDir("exp", Seq(
        StubDir(
          "00000",
          Seq(
            StubDir(
              "backward", Nil,
              Seq(StubFile("test_00001.txt.channel0.bin", "foo".getBytes, new Date(123)))
            )
          )
        )
      ))).head
    pointSource.coordinate must be(MeasuredPointCoordinates(new Date(123), 1, 1, 0, Directions.Backward))
    pointSource.channelId must be("channel0")
  }

  case class StubDir(name: String, directories: Seq[StubDir], files: Seq[StubFile] = Nil) extends VirtualDirectory

  case class StubFile(name: String, content: Array[Byte], time: Date) extends VirtualFile

}