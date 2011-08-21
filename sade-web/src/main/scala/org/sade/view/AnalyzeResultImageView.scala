package org.sade.view

import net.liftweb.http.{InMemoryResponse, LiftResponse, Req}
import java.awt.image.BufferedImage
import org.sade.model.SadeDB
import org.squeryl.PrimitiveTypeMode
import javax.imageio.{ImageIO}
import org.apache.commons.io.output.ByteArrayOutputStream
import net.liftweb.common.{Box, Full}
import net.liftweb.http.{S}
import org.sade.starcoords._
import AnalyzeResultImageView._

class AnalyzeResultImageView extends PrimitiveTypeMode {
  def dispatch: PartialFunction[Req, () => Box[LiftResponse]] = {
    case Req("analyze-result-image" :: Nil, _, _) => () => Full(drawImage())
  }

  def filterByFlag(flagParam: String, filterFun: (scala.Seq[SkyMapPoint]) => Seq[SkyMapPoint], points: Seq[SkyMapPoint]): Seq[SkyMapPoint] = {
    S.param(flagParam).map(_ => filterFun(points)).openOr(points)
  }

  def drawImage() = {
    val width = 640
    val height = 480
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    var points = SadeDB.skyMapPoints.toSeq
    points = filterByFlag(meanFilterParam, SkyMapFilter.averageFilter, points)
    points = filterByFlag(logarithmParam, SkyMapFilter.logarithmFilter, points)
    StarCoordsPainter(points, S.param(modeParam).map {
      case "plane" => Plane
      case "galactic" => Galactic
    }.openOr(Plane), width, height).paint(bufferedImage.createGraphics())
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", outputStream)
    InMemoryResponse(outputStream.toByteArray, ("Content-type" -> "image/png") :: Nil, Nil, 200)
  }
}

object AnalyzeResultImageView {
  val meanFilterParam = "mean-filter"
  val logarithmParam = "logarithm"
  val modeParam = "mode"
}