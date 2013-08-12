package org.sade.model

import org.squeryl.{PrimitiveTypeMode, KeyedEntity, Schema}
import org.sade.starcoords.{SkyMapPoint, MeasuredPointCoordinates, Directions}
import org.apache.commons.io.output.ByteArrayOutputStream
import java.sql.{Timestamp}
import annotation.tailrec
import java.util.zip.GZIPInputStream
import java.io.{DataInputStream, ByteArrayInputStream}
import org.squeryl.dsl.ast.{RightHandSideOfIn, ConstantExpressionNodeList}
import org.squeryl.dsl.CompositeKey2

object SadeDB extends Schema with PrimitiveTypeMode {
  val points = table[Point]()

  val pointContents = table[PointContent]()

  val analyzeResults = table[AnalyzeResult]()

  def contentByPoint(pointId: PointKeyed.Key) = pointContents.lookup(pointId).get

  def skyMapPoints(expName: String, channelFun: AnalyzeResult => Double = _.meanValue, channelId: String = "channel0"): Iterable[SkyMapPoint] = from(points, analyzeResults) ((content, result) => {  //TODO
    where((content.timestamp === result.timestamp and content.channelId === channelId and result.channelId === channelId) and (content.expName === expName)) select ((content, result))
  }).map {
    case (content, result) => SkyMapPoint(content.coordinates, 0, channelFun(result))
  }

  implicit def traversableOfTimestamp2ListTimestamp(l: Traversable[Timestamp]) =
    new RightHandSideOfIn[Timestamp](new ConstantExpressionNodeList[Timestamp](l))

  def inIdSet(ids: Set[PointKeyed.Key])(p: PointKeyed) =
    (p.timestamp in (ids.map(_.a1))) and (p.channelId in (ids.map(_.a2)))

  def dropPoints(ids: Set[PointKeyed.Key]) {
    inTransaction {
      dropAnalyze(ids)
      pointContents.deleteWhere(inIdSet(ids))
      points.deleteWhere(inIdSet(ids))
    }
  }

  def dropAnalyze(ids: Set[PointKeyed.Key]) {
    inTransaction {
      analyzeResults.deleteWhere(inIdSet(ids))
    }
  }

  def analyzeResultAndTokenStatus(expName: String): Iterable[(Point, Option[AnalyzeResult])] = join(points, analyzeResults.leftOuter) ((content, result) => {
    where(content.expName === expName) select ((content, result)) on(content.id === result.get.id)
  })

  def experiments = from(points) (p => groupBy(p.expName)).map(_.key)

  def pointCount(expName: String) = from(points) (p => where(p.expName === expName) compute(count())).head.measures

  def analyzeResultCount(expName: String) = from(points, analyzeResults) ((p,r) => where((p.id === r.id) and (p.expName === expName)) compute(count())).head.measures
}

case class Point(
                         timestamp: Timestamp,
                         channelId: String,
                         expName: String,
                         pointIndex: Int,
                         pointCount: Int,
                         dirIndex: Int,
                         direction: Directions.Direction
                         ) extends PointKeyed {
  def this() = this (null, null, null, 0, 0, 0, Directions.Forward)

  def coordinates = MeasuredPointCoordinates(
    timestamp,
    pointIndex,
    pointCount,
    dirIndex,
    direction
  )

  def content = {
    Point.unzippedContentBy(id)
  }
}

case class PointContent(timestamp: Timestamp, channelId: String, content: Array[Byte]) extends PointKeyed

case class AnalyzeResult(
                          timestamp: Timestamp,
                          channelId: String,
                          meanValue: Double,
                          absoluteError: Double,
                          meanFrequency: Double
                          ) extends PointKeyed {
  def this() = this(null, null, 0, 0, 0)
}

object AnalyzeResult {
  def apply(id: PointKeyed.Key,
            meanValue: Double,
            absoluteError: Double,
            meanFrequency: Double
             ): AnalyzeResult = AnalyzeResult(id.a1, id.a2, meanValue, absoluteError, meanFrequency)
}

object Point {
  def apply(coordinate: MeasuredPointCoordinates, expName: String, channelId: String): Point = {
    Point(new Timestamp(coordinate.time.getTime), channelId, expName, coordinate.pointIndex, coordinate.pointCount, coordinate.dirIndex, coordinate.direction)
  }

  def unzippedContentBy(pointId: PointKeyed.Key): Array[Byte] = {
    val content = SadeDB.contentByPoint(pointId).content
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

trait PointKeyed extends KeyedEntity[CompositeKey2[Timestamp, String]] {

  def timestamp: Timestamp
  def channelId: String

  def id = new CompositeKey2(timestamp, channelId)
}

object PointKeyed {
  type Key = CompositeKey2[Timestamp, String]
}