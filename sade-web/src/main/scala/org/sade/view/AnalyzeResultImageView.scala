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
    case Req(Seq("analyze-result-image", expName), _, _) if SadeDB.experiments.exists(_ == expName) => () => Full(drawImage(expName))
  }

  def filterByFlag(flagParam: String, filterFun: (scala.Seq[SkyMapPoint]) => Seq[SkyMapPoint], points: Seq[SkyMapPoint]): Seq[SkyMapPoint] = {
    S.param(flagParam).map(_ => filterFun(points)).openOr(points)
  }

  def drawImage(expName: String) = {
    val width = 640
    val height = 480
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val deltaChannel = S.param(AnalyzeResultImageView.deltaChannel).openOr("channel0")
    var points = S.param(channel).map {
      case "value" => SadeDB.skyMapPoints(expName, channelId = deltaChannel).toSeq
      case "frequency" => SadeDB.skyMapPoints(expName, _.meanFrequency, channelId = deltaChannel).toSeq
    }.openOr(SadeDB.skyMapPoints(expName, channelId = deltaChannel).toSeq)
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
  val channel = "channel"
  val deltaChannel = "delta-channel"
}