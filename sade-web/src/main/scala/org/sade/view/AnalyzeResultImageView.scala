package org.sade.view

import net.liftweb.http.{InMemoryResponse, LiftResponse, Req}
import java.awt.image.BufferedImage
import org.sade.model.SadeDB
import org.squeryl.PrimitiveTypeMode
import javax.imageio.{ImageIO}
import org.apache.commons.io.output.ByteArrayOutputStream
import net.liftweb.common.{Box, Full}
import net.liftweb.http.{S}
import org.sade.starcoords.{Galactic, Plane, StarCoordsPainter}

class AnalyzeResultImageView extends PrimitiveTypeMode {
  def dispatch: PartialFunction[Req, () => Box[LiftResponse]] = {
    case Req("analyze-result-image" :: Nil, _, _) => () => Full(drawImage())
  }

  def drawImage() = {
    val width = 640
    val height = 480
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val points = SadeDB.skyMapPoints
    StarCoordsPainter(points.toSeq, S.param("mode").map {
      case "plane" => Plane
      case "galactic" => Galactic
    }.openOr(Plane), width, height).paint(bufferedImage.createGraphics())
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", outputStream)
    InMemoryResponse(outputStream.toByteArray, ("Content-type" -> "image/png") :: Nil, Nil, 200)
  }
}