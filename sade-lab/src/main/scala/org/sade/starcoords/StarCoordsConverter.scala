package org.sade.starcoords

import scala.math._
import java.util.{TimeZone, GregorianCalendar, Date, Calendar}

object StarCoordsConverter {
  def radiansFrom(grad: Double) = {
    grad * Pi / 180
  }

  def gradFrom(radians: Double) = {
    radians * 180 / Pi
  }

  def toHourAngle(radians: Double) = radians * 12 / Pi

  def fromHourAngle(angle: Double) = angle * Pi / 12


  def sideralTime(coords: FullStandCoordinate) = {
    val time = coords.stand.time
    val j2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    j2000.set(2000, 0, 1, 12, 0)
    val d = new Date(time.getTime - j2000.getTime.getTime).getTime / 1000.0 / 3600.0 / 24.0
    fromHourAngle(18.697374558 + 24.06570982441908 * d)
  }

  def toDeltaTime(coords: FullStandCoordinate) = {
    val thetaRad = radiansFrom(coords.stand.theta)
    val phiRad = radiansFrom(coords.labCoord.latitude)
    val deltaRad = asin(cos(thetaRad) * cos(phiRad))
    val cosDelta = cos(deltaRad)
    val tRad = atan2(-sin(thetaRad) / cosDelta, -sin(phiRad) * cos(thetaRad) / cosDelta)
    DeltaTime(gradFrom(deltaRad), gradFrom(tRad))
  }
}

case class FullStandCoordinate(stand: StandCoordinate, labCoord: LabCoordinates)

case class StandCoordinate(theta: Double, time: Date)

case class LabCoordinates(longitude: Double, latitude: Double)

case class DeltaTime(delta: Double, t: Double)