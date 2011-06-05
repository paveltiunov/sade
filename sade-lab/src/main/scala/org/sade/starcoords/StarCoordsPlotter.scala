package org.sade.starcoords

import swing.Component
import java.awt.{Paint, RenderingHints, Color, Graphics2D}

class StarCoordsPlotter extends Component {
  var skyMapPoints: Seq[SkyMapPoint] = Nil

  def drawPoint(g: Graphics2D)(p: ((Double, Double), Double)) {
    val hue = p._2.toFloat
    g.setColor(new Color(Color.HSBtoRGB(hue, 0.8f, 1.0f)))
    g.fillOval((p._1._1 * size.getWidth).toInt - 2, (p._1._2 * size.getHeight).toInt - 2, 4, 4)
  }

  private def normalize(values: Seq[Double]) = {
    val min = values.min
    val max = values.max
    values.map(v => (v - min) / (max - min))
  }

  override protected def paintComponent(g: Graphics2D) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.clearRect(0, 0, size.getWidth.toInt, size.getHeight.toInt)

    val normalized = normalize(skyMapPoints.map(_.rotationAngle)) zip normalize(skyMapPoints.map(_.time.getTime.toDouble)) zip normalize(skyMapPoints.map(_.value))
    normalized.foreach(drawPoint(g))
  }
}