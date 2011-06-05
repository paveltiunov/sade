package org.sade.starcoords

import swing.Component
import java.awt.{Paint, RenderingHints, Color, Graphics2D}

class StarCoordsPlotter extends Component {
  var skyMapPoints: Seq[SkyMapPoint] = Nil
  def filteredPoints: Seq[SkyMapPoint] = SkyMapFilter.averageFilter(skyMapPoints)
  private var viewMode: ViewMode = Galactic

  def changeViewMode(viewMode: ViewMode) {
    this.viewMode = viewMode
    repaint()
  }

  def changeSkyMapPoints(points: Seq[SkyMapPoint]) {
    skyMapPoints = points
    repaint()
  }

  def drawPoint(g: Graphics2D)(p: ((Double, Double), Double)) {
    val hue = p._2.toFloat * 5.0f/6.0f
    val color = new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f))
    g.setColor(new Color(color.getRed, color.getGreen, color.getBlue, 200))
    g.fillOval((p._1._1 * size.getWidth).toInt - 2, (p._1._2 * size.getHeight).toInt - 2, 4, 4)
  }

  private def normalize(values: Seq[Double]) = {
    val min = values.min
    val max = values.max
    values.map(v => (v - min) / (max - min))
  }

  def plotNormalized(normalized: Seq[((Double, Double), Double)], g: Graphics2D) {
    normalized.foreach(drawPoint(g))
  }

  private def planePlot(g: Graphics2D) {
    val normalized = normalize(filteredPoints.map(_.rotationAngle)) zip normalize(filteredPoints.map(_.time.getTime.toDouble)) zip normalize(filteredPoints.map(_.value))
    plotNormalized(normalized, g)
  }

  def galacticPlot(g: Graphics2D) {
    val galacticCoords = filteredPoints.map(_.standCoordinate).map(c => StarCoordsConverter.toGalacticCoordinates(FullStandCoordinate(c, LabCoordinates(37.686, 55.765))))
    val projected = galacticCoords.map(c => MollweideProjection.project(c.l, c.b))
    val normalized = normalize(projected.map(_._1)) zip normalize(projected.map(_._2)) zip normalize(filteredPoints.map(_.value))
    plotNormalized(normalized, g)
  }

  override protected def paintComponent(g: Graphics2D) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    g.clearRect(0, 0, size.getWidth.toInt, size.getHeight.toInt)
    val border = 10
    g.scale((size.getWidth - border*2) / size.getWidth, (size.getHeight - border*2) / size.getHeight)
    g.translate(border, border)

    viewMode match {
      case Plane => planePlot(g)
      case Galactic => galacticPlot(g)
    }
  }
}

trait ViewMode
case object Plane extends ViewMode
case object Galactic extends ViewMode