package org.sade.starcoords

import xml.{Node, NodeSeq}
import java.util.Date
import java.text.{ParseException, SimpleDateFormat, DateFormat}

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
    SkyMapPoint(
      nodeValue(node, "StandardDeviation").toDouble,
      parseDate(nodeValue(node, "Time").replace("+", "GMT+").replaceFirst("\\.\\d+GMT\\+", "GMT+")),
      intNodeValue(node, "PointIndex"),
      intNodeValue(node, "PointCount"),
      intNodeValue(node, "DirIndex"),
      nodeValue(node, "Direction") match {
        case "FORWARD" => Forward
        case "BACKWARD" => Backward
      },
      nodeValue(node, "Value").toDouble
    )
  }

  def parse(anx: NodeSeq) = {
    val seq = anx \ "SkyMapPoint"
    seq.map(parseNode)
  }
}

case class SkyMapPoint(
                        standardDeviation: Double,
                        time: Date,
                        pointIndex: Int,
                        pointCount: Int,
                        dirIndex: Int,
                        direction: Direction,
                        value: Double
                        ) {
  def rotationAngle = (direction match {
    case Forward => pointIndex
    case Backward => pointCount - pointIndex
  }) * 360.0 / pointCount

  def standCoordinate = StandCoordinate(rotationAngle, time)
}

trait Direction
case object Forward extends Direction
case object Backward extends Direction