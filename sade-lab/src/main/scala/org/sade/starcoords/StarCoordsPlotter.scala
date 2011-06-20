package org.sade.starcoords

import swing.Component
import java.awt.{RenderingHints, Color, Graphics2D}

class StarCoordsPlotter extends Component {
  var skyMapPoints: Seq[SkyMapPoint] = Nil
  def filteredPoints: Seq[SkyMapPoint] = SkyMapFilter.averageFilter(skyMapPoints)
  private var viewMode: ViewMode = Galactic
  val cmbrPoints: Seq[GalacticCoordinates] = Seq(GalacticCoordinates(48, 264))

  def changeViewMode(viewMode: ViewMode) {
    this.viewMode = viewMode
    repaint()
  }

  def changeSkyMapPoints(points: Seq[SkyMapPoint]) {
    skyMapPoints = points
    repaint()
  }

  def drawColorPoint(g: Graphics2D)(p: NormalizedColorPoint) {
    val hue = p.color.toFloat * 5.0f/6.0f
    val color = new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f))
    g.setColor(new Color(color.getRed, color.getGreen, color.getBlue, 200))
    g.fillOval((p.point.x * size.getWidth).toInt - 2, (p.point.y * size.getHeight).toInt - 2, 4, 4)
  }

  def drawCMBRPoint(g: Graphics2D)(p: NormalizedCMBR) {
    g.setColor(Color.black)
    val centerX = (p.point.x * size.getWidth).toInt
    val centerY = (p.point.y * size.getHeight).toInt
    g.drawRect(centerX - 5, centerY - 5, 10, 10)
    g.drawLine(centerX - 5, centerY - 5, centerX + 5, centerY + 5)
    g.drawLine(centerX + 5, centerY - 5, centerX - 5, centerY + 5)
  }

  private def normalize(values: Seq[Double]) = {
    val min = values.min
    val max = values.max
    values.map(v => (v - min) / (max - min))
  }

  def plotNormalized(normalized: Seq[((Double, Double), Double)], g: Graphics2D) {
    normalized.map(p => NormalizedColorPoint(NormalizedPoint(p._1._1, p._1._2), p._2)).foreach(drawColorPoint(g))
  }

  private def planePlot(g: Graphics2D) {
    val normalized = normalize(filteredPoints.map(_.rotationAngle)) zip normalize(filteredPoints.map(_.time.getTime.toDouble)) zip normalize(filteredPoints.map(_.value))
    plotNormalized(normalized, g)
  }

  def galacticPlot(g: Graphics2D) {
    val galacticCoords = filteredPoints.map(p => (p.standCoordinate, p.value)).map(c => (StarCoordsConverter.toGalacticCoordinates(FullStandCoordinate(c._1, LabCoordinates(37.686, 55.765, 135))), c._2))
    val projected = galacticCoords.map(c => (MollweideProjection.project(c._1.l, c._1.b), c._2)).map(p => NormalizedColorPoint(NormalizedPoint(p._1._1, p._1._2), p._2))
    val projectedCmbr = cmbrPoints.map(MollweideProjection.project).map(p => NormalizedCMBR(NormalizedPoint(p._1, p._2)))
    val normalizedPointHolders = normalizePointHolders(projected ++ projectedCmbr)
    val projectedAndNormalized = normalizeNormalizables(normalizedPointHolders.collect {case cp: NormalizedColorPoint => cp})
    projectedAndNormalized.foreach(drawColorPoint(g))
    normalizedPointHolders.collect {case p: NormalizedCMBR => p}.foreach(drawCMBRPoint(g))
  }

  private def normalizePointHolders(values: Seq[PointHolder[_]]) = {
    val normalizedPoints = normalizeNormalizables(values.map(_.point))
    values.zip(normalizedPoints).map(t => t._1.normalizePoint(t._2))
  }

  private def normalizeNormalizables[T <: Normalizable[T]](values: Seq[T]) = {
    val minPoint = values.reduceLeft(_ min _)
    val maxPoint = values.reduceLeft(_ max _)
    values.map(_.normalize(minPoint, maxPoint))
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

case class NormalizedPoint(x: Double, y: Double) extends Normalizable[NormalizedPoint] {
  def min(point: NormalizedPoint) = NormalizedPoint(math.min(x, point.x), math.min(y, point.y))
  def max(point: NormalizedPoint) = NormalizedPoint(math.max(x, point.x), math.max(y, point.y))
  def normalize(min: NormalizedPoint, max: NormalizedPoint) = NormalizedPoint(normalizeDouble(min.x, max.x, x), normalizeDouble(min.y, max.y, y))
}
case class NormalizedColorPoint(point: NormalizedPoint, color: Double) extends PointHolder[NormalizedColorPoint] with Normalizable[NormalizedColorPoint]{
  def normalize(min: NormalizedColorPoint, max: NormalizedColorPoint) = copy(color = normalizeDouble(min.color, max.color, color))

  def max(point: NormalizedColorPoint) = copy(color = math.min(color, point.color))

  def min(point: NormalizedColorPoint) = copy(color = math.max(color, point.color))

  def normalizePoint(point: NormalizedPoint) = copy(point = point)
}
case class NormalizedCMBR(point: NormalizedPoint) extends PointHolder[NormalizedCMBR] {
  def normalizePoint(point: NormalizedPoint) = NormalizedCMBR(point)
}

trait PointHolder[T] {
  def point: NormalizedPoint
  def normalizePoint(point: NormalizedPoint): T
}

trait Normalizable[T <: Normalizable[T]] {
  def normalizeDouble(min: Double, max: Double, value: Double) = (value-min)/(max - min)
  def min(point: T): T
  def max(point: T): T
  def normalize(min: T, max: T): T
}

trait ViewMode
case object Plane extends ViewMode
case object Galactic extends ViewMode