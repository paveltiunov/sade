package org.sade.model

import java.sql.{Timestamp}
import org.squeryl.{PrimitiveTypeMode, KeyedEntity, Schema}
import org.sade.starcoords.{SkyMapPoint, MeasuredPointCoordinates, Directions}


object SadeDB extends Schema with PrimitiveTypeMode {
  val points = table[Point]()

  val pointContents = table[PointContent]()

  val analyzeResults = table[AnalyzeResult]()

  val analyzeTokens = table[AnalyzeToken]()

  def contentByPoint(point: Point) = pointContents.lookup(point.id).get

  def skyMapPoints: Iterable[SkyMapPoint] = from(points, analyzeResults) ((content, result) => {
    where(content.id === result.id) select ((content, result))
  }).map {
    case (content, result) => SkyMapPoint(content.coordinates, 0, result.meanValue)
  }
}

case class Point(
                         id: Timestamp,
                         pointIndex: Int,
                         pointCount: Int,
                         dirIndex: Int,
                         direction: Directions.Direction
                         ) extends KeyedEntity[Timestamp] {
  def this() = this (null, 0, 0, 0, Directions.Forward)

  def coordinates = MeasuredPointCoordinates(
    id,
    pointIndex,
    pointCount,
    dirIndex,
    direction
  )

  def content = SadeDB.contentByPoint(this).content
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
  def apply(coordinate: MeasuredPointCoordinates): Point = {
    Point(new Timestamp(coordinate.time.getTime), coordinate.pointIndex, coordinate.pointCount, coordinate.dirIndex, coordinate.direction)
  }
}