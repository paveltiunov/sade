package org.sade.lab.ui

import java.io.File
import scala.concurrent.ops._
import org.sade.upload.PointUploader
import swing.{BorderPanel, Frame}
import org.sade.lab.{PointSource, VirtualDirectoryImpl, ExperimentStorageCrawler}
import swing.BorderPanel.Position
import org.sade.binding.{BindProgressBar, BindLabel, BindField}

class UploaderForm(experimentDirectory: File, serverUrl: String) extends Frame {
  private val pointUploader = new PointUploader(serverUrl)

  object Model {
    object currentPointSource extends BindField[PointSource]
    object iterationProgress extends BindField[Int]
  }

  spawn {
    while(true) {
      val crawlStream = ExperimentStorageCrawler.crawl(VirtualDirectoryImpl(experimentDirectory))
      val pointCount = crawlStream.size
      crawlStream.zipWithIndex.foreach { case (source, index) => {
        Model.currentPointSource.set(source)
        Model.iterationProgress.set(index * 100 / pointCount)
        try {
          pointUploader.uploadPoint(source)
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }}
      Model.currentPointSource.set(null)
      Model.iterationProgress.set(0)
      Thread.sleep(10000)
    }
  }

  contents = new BorderPanel {
    val currentPointLabel = new BindLabel[PointSource](Model.currentPointSource, _.toString)
    layout(currentPointLabel) = Position.Center
    val bar = new BindProgressBar[Int](Model.iterationProgress, p => p)
    layout(bar) = Position.South
  }
}