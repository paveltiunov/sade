package org.sade.model

import java.sql.Date
import org.sade.starcoords.Directions
import org.squeryl.{KeyedEntity, Schema}
import java.util.UUID


object SadeDB extends Schema {
  val pointContents = table[PointContent]()
}

case class PointContent(
                         content: Array[Byte],
                         time: Date,
                         pointIndex: Int,
                         pointCount: Int,
                         dirIndex: Int,
                         direction: Directions.Direction,
                         id: UUID = UUID.randomUUID()
                         ) extends KeyedEntity[UUID] {
  def this() = this (null, null, 0, 0, 0, Directions.Forward)
}