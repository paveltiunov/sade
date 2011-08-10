package org.sade.model

import org.squeryl.{KeyedEntity, Schema}
import org.sade.starcoords.{MeasuredPointCoordinates, Directions}
import java.sql.{Timestamp}


object SadeDB extends Schema {
  val pointContents = table[PointContent]()
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

object PointContent {
  def apply(content: Array[Byte], coordinate: MeasuredPointCoordinates): PointContent = {
    PointContent(content, new Timestamp(coordinate.time.getTime), coordinate.pointIndex, coordinate.pointCount, coordinate.dirIndex, coordinate.direction)
  }
}