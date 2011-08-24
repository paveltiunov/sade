package org.sade.starcoords

import org.junit.Test
import java.sql.Date
import org.scalatest.junit.MustMatchersForJUnit


class StarCoordsPainterTest extends MustMatchersForJUnit {
  val painter = new StarCoordsPainter(Seq(
    SkyMapPoint(MeasuredPointCoordinates(new Date(1), 1, 2, 3, Directions.Forward), 0, 0),
    SkyMapPoint(MeasuredPointCoordinates(new Date(2), 2, 2, 3, Directions.Forward), 0, 1),
    SkyMapPoint(MeasuredPointCoordinates(new Date(3), 3, 2, 3, Directions.Forward), 0, 2)
  ), Plane, 1, 1)

  @Test
  def planeNormalization() {
    painter.planeNormalizedColorPoints.map(_.color) must be (Seq(0, 0.5, 1))
  }

  @Test
  def galacticNormalization() {
    painter.galacticProjectedAndNormalized(painter.galacticPointHolders).map(_.color) must be (Seq(0, 0.5, 1))
  }
}