package org.sade.lab

import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}


object ExperimentStorageCrawler {
  def crawl(directory: VirtualDirectory): Stream[PointSource] = {
    directory.directories.toStream.flatMap(indexDir => {
      indexDir.directories.toStream.flatMap(directionDir => {
        val pointFiles = directionDir.files.toStream.filter(f => f.name.startsWith("test_") && f.name.endsWith(".txt"))
        pointFiles.map(f => {
          PointSource(
            MeasuredPointCoordinates(
              f.time,
              f.name.replace("test_", "").replace(".txt", "").toInt,
              pointFiles.size,
              indexDir.name.toInt,
              directionDir.name.toLowerCase match {
                case "forward" => Directions.Forward
                case "backward" => Directions.Backward
              }),
            f.content _
          )
        })
      })
    })
  }

}

case class PointSource(coordinate: MeasuredPointCoordinates, content: () => Array[Byte])

trait VirtualFile {
  def content: Array[Byte]

  def name: String

  def time: Date
}

trait VirtualDirectory {
  def name: String

  def files: Seq[VirtualFile]

  def directories: Seq[VirtualDirectory]
}