package org.sade.starcoords

import scala.math._

object StarCoordsConverter {
  def radiansFrom(grad: Double) = {
    grad * Pi / 180
  }

  def gradFrom(radians: Double) = {
    radians * 180 / Pi
  }

  def toDeltaTime(coords: LabCoordinates) = {
    val thetaRad = radiansFrom(coords.theta)
    val phiRad = radiansFrom(coords.phi)
    val deltaRad = asin(cos(thetaRad) * cos(phiRad))
    val cosDelta = cos(deltaRad)
    val tRad = atan2(-sin(thetaRad) / cosDelta, -sin(phiRad) * cos(thetaRad) / cosDelta)
    DeltaTime(gradFrom(deltaRad), gradFrom(tRad))
  }
}

case class LabCoordinates(theta: Double, phi: Double)
case class DeltaTime(delta: Double, t: Double)