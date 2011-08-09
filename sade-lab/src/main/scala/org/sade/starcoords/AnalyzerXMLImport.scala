package org.sade.starcoords

import xml.{Node, NodeSeq}
import java.text.SimpleDateFormat
import java.util.Date
import scala.Enumeration

object AnalyzerXMLImport {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz")

  private def nodeValue(node: Node, nodeName: String) = {
    (node \ nodeName).text
  }

  private def intNodeValue(node: Node, nodeName: String): Int = {
    nodeValue(node, nodeName).toInt
  }

  private def parseDate(date: String) = {
    dateFormat.parse(date)
  }

  private def parseNode(node: Node) = {
    import Directions._
    SkyMapPoint(MeasuredPointCoordinates(
      parseDate(nodeValue(node, "Time").replace("+", "GMT+").replaceFirst("\\.\\d+GMT\\+", "GMT+")),
      intNodeValue(node, "PointIndex"),
      intNodeValue(node, "PointCount"),
      intNodeValue(node, "DirIndex"),
      nodeValue(node, "Direction") match {
        case "FORWARD" => Forward
        case "BACKWARD" => Backward
      }),
      nodeValue(node, "StandardDeviation").toDouble,
      nodeValue(node, "Value").toDouble
    )
  }

  def parse(anx: NodeSeq) = {
    val seq = anx \ "SkyMapPoint"
    seq.map(parseNode)
  }
}

case class MeasuredPointCoordinates(
                                     time: Date,
                                     pointIndex: Int,
                                     pointCount: Int,
                                     dirIndex: Int,
                                     direction: Directions.Direction
                                     )

object MeasuredPointCoordinates {
  def toMap(coords: MeasuredPointCoordinates): Map[String, String] = {
    Map(
      "time" -> coords.time.getTime.toString,
      "pointIndex" -> coords.pointIndex.toString,
      "pointCount" -> coords.pointCount.toString,
      "dirIndex" -> coords.dirIndex.toString,
      "direction" -> coords.direction.id.toString
    )
  }

  def fromMap(map: Map[String, String]): MeasuredPointCoordinates = {
    MeasuredPointCoordinates(
      new Date(map("time").toLong),
      map("pointIndex").toInt,
      map("pointCount").toInt,
      map("dirIndex").toInt,
      Directions(map("direction").toInt)
    )
  }
}

case class SkyMapPoint(
                        coordinates: MeasuredPointCoordinates,
                        standardDeviation: Double,
                        value: Double
                        ) {
  def rotationAngle = (direction match {
    case Directions.Forward => pointIndex
    case Directions.Backward => pointCount - pointIndex
  }) * 360.0 / pointCount

  def standCoordinate = StandCoordinate(rotationAngle, time)

  def time = coordinates.time
  def pointIndex = coordinates.pointIndex
  def pointCount = coordinates.pointCount
  def dirIndex = coordinates.dirIndex
  def direction = coordinates.direction
}

object Directions extends Enumeration {
  type Direction = Value
  val Forward = Value(1, "Forward")
  val Backward = Value(2, "Backward")
}