package org.sade.view

import net.liftweb.http.{InMemoryResponse, LiftResponse, Req}
import java.awt.image.BufferedImage
import org.sade.model.{Point, SadeDB}
import org.squeryl.PrimitiveTypeMode
import javax.imageio.{ImageIO}
import org.apache.commons.io.output.ByteArrayOutputStream
import net.liftweb.common.{Box, Full}
import net.liftweb.http.{S}
import org.sade.starcoords._
import AnalyzeResultImageView._
import org.apache.poi.hssf.usermodel.{HSSFCell, HSSFWorkbook}

class AnalyzeResultImageView extends PrimitiveTypeMode {
  def dispatch: PartialFunction[Req, () => Box[LiftResponse]] = {
    case Req(Seq("analyze-result-image", expName), _, _) if SadeDB.experiments.exists(_ == expName) => () => Full(drawImage(expName))
    case Req(Seq("export-time-table", expName), _, _) if SadeDB.experiments.exists(_ == expName) => () => Full(exportTimeTable(expName))
  }

  def filterByFlag(flagParam: String, filterFun: (scala.Seq[SkyMapPoint]) => Seq[SkyMapPoint], points: Seq[SkyMapPoint]): Seq[SkyMapPoint] = {
    S.param(flagParam).map(_ => filterFun(points)).openOr(points)
  }

  def drawImage(expName: String) = {
    val width = 640
    val height = 480
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val points = pointsBy(expName).sortBy(_.time.getTime)
    StarCoordsPainter(points, S.param(modeParam).map {
      case "plane" => Plane
      case "galactic" => Galactic
    }.openOr(Plane), width, height).paint(bufferedImage.createGraphics())
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", outputStream)
    InMemoryResponse(outputStream.toByteArray, ("Content-type" -> "image/png") :: Nil, Nil, 200)
  }

  val exportTable: Seq[(String, (HSSFCell, SkyMapPoint)=> Unit)] = Seq(
    "Time" -> { (c, p) => c.setCellValue(p.coordinates.time) },
    "Point index" -> { (c, p) => c.setCellValue(p.pointIndex) },
    "Dir index" -> { (c, p) => c.setCellValue(p.dirIndex) },
    "Direction" -> { (c, p) => c.setCellValue(p.direction.toString) },
    "Value" -> { (c, p) => c.setCellValue(p.value) }
  )

  def exportTimeTable(expName: String) = {
    val workbook = new HSSFWorkbook()
    val sheet = workbook.createSheet()
    val points = pointsBy(expName)
    val header = sheet.createRow(0)
    exportTable.map(_._1).zipWithIndex.map {case (head, index) => header.createCell(index).setCellValue(head) }
    points.zipWithIndex.foreach {
      case (point, rowIndex) => {
        val row = sheet.createRow(rowIndex + 1)
        exportTable.map(_._2).zipWithIndex.foreach {
          case (fun, cellIndex) => fun(row.createCell(cellIndex), point)
        }
      }
    }
    val stream = new ByteArrayOutputStream()
    workbook.write(stream)
    InMemoryResponse(stream.toByteArray, ("Content-type" -> "application/octet-stream") :: ("Content-Disposition" -> "attachment; filename=\"%s.xls\"".format(expName)) :: Nil, Nil, 200)
  }

  def pointsBy(expName: String) = {
    val deltaChannel = S.param(AnalyzeResultImageView.deltaChannel).openOr("channel0")
    var points = S.param(channel).map {
      case "value" => SadeDB.skyMapPoints(expName, channelId = deltaChannel).toSeq
      case "frequency" => SadeDB.skyMapPoints(expName, _.meanFrequency, channelId = deltaChannel).toSeq
    }.openOr(SadeDB.skyMapPoints(expName, channelId = deltaChannel).toSeq)
    points = filterByFlag(meanFilterParam, SkyMapFilter.averageFilter, points)
    filterByFlag(logarithmParam, SkyMapFilter.logarithmFilter, points)
  }
}

object AnalyzeResultImageView {
  val meanFilterParam = "mean-filter"
  val logarithmParam = "logarithm"
  val modeParam = "mode"
  val channel = "channel"
  val deltaChannel = "delta-channel"
}