package org.sade.lab

import java.util.Date
import org.sade.starcoords.{Directions, MeasuredPointCoordinates}
import java.io.{FileInputStream, File}

object ExperimentStorageCrawler {
  val fileNameRegExp = "test_(\\d+)\\.txt\\.(.+?)\\.bin".r
  def crawl(directory: VirtualDirectory): Stream[PointSource] = {
    directory.directories.toStream.flatMap(indexDir => {
      indexDir.directories.toStream.flatMap(directionDir => {
        val pointFiles = directionDir.files.toStream.filter(f => fileNameRegExp.pattern.matcher(f.name).matches())
        pointFiles.map(f => {
          val (pointIndex, channelId) = pointIndexAndChannel(f)
          PointSource(
            MeasuredPointCoordinates(
              f.time,
              pointIndex.toInt,
              pointFiles.count(f => pointIndexAndChannel(f)._2 == channelId),
              indexDir.name.toInt,
              directionDir.name.toLowerCase match {
                case "forward" => Directions.Forward
                case "backward" => Directions.Backward
              }),
            directory.name,
            channelId,
            f.content _
          )
        })
      })
    })
  }


  def pointIndexAndChannel(f: VirtualFile): (String, String) = {
    f.name match {
      case fileNameRegExp(pointIndex, channelId) => pointIndex -> channelId
    }
  }
}

case class PointSource(coordinate: MeasuredPointCoordinates, expName: String, channelId: String, content: () => Array[Byte])

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

case class VirtualDirectoryImpl(dir: File) extends VirtualDirectory {
  def name = dir.getName

  def files = dir.listFiles().filter(!_.isDirectory).map(VirtualFileImpl)

  def directories = dir.listFiles().filter(_.isDirectory).map(VirtualDirectoryImpl)
}

case class VirtualFileImpl(file: File) extends VirtualFile {
  def content = {
    val fileInputStream = new FileInputStream(file)
    try {
      val buffer = Array.ofDim[Byte](fileInputStream.available())
      fileInputStream.read(buffer)
      buffer
    } finally {
      fileInputStream.close()
    }
  }

  def name = file.getName

  def time = new Date(file.lastModified())
}