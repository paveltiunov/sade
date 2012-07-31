package org.sade.model

import org.squeryl.{PrimitiveTypeMode, KeyedEntity, Schema}
import org.sade.starcoords.{SkyMapPoint, MeasuredPointCoordinates, Directions}
import org.apache.commons.io.output.ByteArrayOutputStream
import java.sql.{Timestamp}
import annotation.tailrec
import java.util.zip.GZIPInputStream
import java.io.{DataInputStream, ByteArrayInputStream}

object SadeDB extends Schema with PrimitiveTypeMode {
  val points = table[Point]()

  val pointContents = table[PointContent]()

  val analyzeResults = table[AnalyzeResult]()

  val analyzeTokens = table[AnalyzeToken]()

  def contentByPoint(point: Point) = pointContents.lookup(point.id).get

  def skyMapPoints(expName: String): Iterable[SkyMapPoint] = from(points, analyzeResults) ((content, result) => {
    where((content.id === result.id) and (content.expName === expName)) select ((content, result))
  }).map {
    case (content, result) => SkyMapPoint(content.coordinates, 0, result.meanValue)
  }

  def dropPoint(id: Timestamp) {
    inTransaction {
      analyzeTokens.deleteWhere(_.id === id)
      analyzeResults.deleteWhere(_.id === id)
      pointContents.deleteWhere(_.id === id)
      points.deleteWhere(_.id === id)
    }
  }

  def analyzeResultAndTokenStatus(expName: String): Iterable[(Point, Option[AnalyzeResult], Option[AnalyzeToken])] = join(points, analyzeResults.leftOuter, analyzeTokens.leftOuter) ((content, result, token) => {
    where(content.expName === expName) select ((content, result, token)) on(content.id === result.get.id, token.get.id === content.id)
  })

  def experiments = from(points) (p => groupBy(p.expName)).map(_.key)

  def pointCount(expName: String) = from(points) (p => where(p.expName === expName) compute(count(p.id))).head.measures

  def analyzeResultCount(expName: String) = from(points, analyzeResults) ((p,r) => where((p.id === r.id) and (p.expName === expName)) compute(count(r.id))).head.measures
}

case class Point(
                         id: Timestamp,
                         expName: String,
                         pointIndex: Int,
                         pointCount: Int,
                         dirIndex: Int,
                         direction: Directions.Direction
                         ) extends KeyedEntity[Timestamp] {
  def this() = this (null, null, 0, 0, 0, Directions.Forward)

  def coordinates = MeasuredPointCoordinates(
    id,
    pointIndex,
    pointCount,
    dirIndex,
    direction
  )

  def content = {
    val content = SadeDB.contentByPoint(this).content
    val inputStream = new ByteArrayInputStream(content)
    if ((inputStream.read() | inputStream.read() << 8) == GZIPInputStream.GZIP_MAGIC)
      ungzipContent(content)
    else content
  }

  private def ungzipContent(content: Array[Byte]) = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(content))
    val outputStream = new ByteArrayOutputStream()
    val buffer = Array.ofDim[Byte](8192)
    @tailrec def readStream() {
      val read = inputStream.read(buffer)
      if (read > 0) {
        outputStream.write(buffer, 0, read)
        readStream()
      }
    }
    readStream()
    outputStream.toByteArray
  }
}

case class PointContent(id: Timestamp, content: Array[Byte]) extends KeyedEntity[Timestamp]

case class AnalyzeResult(
                          id: Timestamp,
                          meanValue: Double,
                          absoluteError: Double,
                          meanFrequency: Double
                          ) extends KeyedEntity[Timestamp] {
  def this() = this(null, 0, 0, 0)
}

case class AnalyzeToken(
                          id: Timestamp,
                          analyzeStarted: Timestamp
                         ) extends KeyedEntity[Timestamp] {
  def this() = this(null, null)
}

object Point {
  def apply(coordinate: MeasuredPointCoordinates, expName: String): Point = {
    Point(new Timestamp(coordinate.time.getTime), expName, coordinate.pointIndex, coordinate.pointCount, coordinate.dirIndex, coordinate.direction)
  }
}