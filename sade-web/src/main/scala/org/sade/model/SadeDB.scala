package org.sade.model

import org.squeryl.{KeyedEntity, Schema}
import org.sade.starcoords.{MeasuredPointCoordinates, Directions}
import java.sql.{Timestamp}


object SadeDB extends Schema {
  val pointContents = table[PointContent]()

  val analyzeResults = table[AnalyzeResult]()

  val analyzeTokens = table[AnalyzeToken]()
}

case class PointContent(
                         content: Array[Byte],
                         id: Timestamp,
                         pointIndex: Int,
                         pointCount: Int,
                         dirIndex: Int,
                         direction: Directions.Direction
                         ) extends KeyedEntity[Timestamp] {
  def this() = this (null, null, 0, 0, 0, Directions.Forward)
}

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

object PointContent {
  def apply(content: Array[Byte], coordinate: MeasuredPointCoordinates): PointContent = {
    PointContent(content, new Timestamp(coordinate.time.getTime), coordinate.pointIndex, coordinate.pointCount, coordinate.dirIndex, coordinate.direction)
  }
}