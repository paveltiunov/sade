package org.sade.starcoords

import scala.math._
import java.util.{TimeZone, GregorianCalendar, Date, Calendar}
import org.apache.commons.math.linear.MatrixUtils

object StarCoordsConverter {
  val galacticMatrix = MatrixUtils.createRealMatrix(Array(
    Array(-0.05487556, -0.87343709, -0.48383501),
    Array(0.49410942, -0.44482969, 0.74698224),
    Array(-0.86766614, -0.19807637, 0.45598377)
  ))

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
    gradFrom(fromHourAngle(18.697374558 + 24.06570982441908 * d)) + coords.labCoord.longitude
  }

  def toGalacticCoordinates(standCoordinate: FullStandCoordinate): GalacticCoordinates = {
    toGalacticCoordinates(toEquatorial(standCoordinate))
  }

  def toGalacticCoordinates(equatorial: EquatorialCoordinates): GalacticCoordinates = {
    val deltaRad = radiansFrom(equatorial.delta)
    val alphaRad = radiansFrom(equatorial.alpha)
    val rhsMatrix = MatrixUtils.createColumnRealMatrix(Array(cos(deltaRad) * cos(alphaRad), cos(deltaRad) * sin(alphaRad), sin(deltaRad)))
    val product = galacticMatrix.multiply(rhsMatrix)
    val solution = solveSphericalTriangleEquation(product.getEntry(0, 0), product.getEntry(1, 0), product.getEntry(2, 0))
    GalacticCoordinates(gradFrom(solution._1), gradFrom(solution._2))
  }

  def toEquatorial(coords: FullStandCoordinate) = {
    val deltaTime = toDeltaTime(coords)
    EquatorialCoordinates(deltaTime.delta, sideralTime(coords) - deltaTime.t)
  }

  def solveSphericalTriangleEquation(firstEquationArg: Double, secondEquationArg: Double, thirdEquationArg: Double) = {
    val deltaRad = asin(thirdEquationArg)
    val cosDelta = cos(deltaRad)
    val tRad = atan2(secondEquationArg / cosDelta, firstEquationArg / cosDelta)
    (deltaRad, tRad)
  }

  def toDeltaTime(coords: FullStandCoordinate) = {
    val thetaRad = radiansFrom(coords.stand.theta)
    val phiRad = radiansFrom(coords.labCoord.latitude)
    val result = solveSphericalTriangleEquation(
      -sin(phiRad) * cos(thetaRad),
      -sin(thetaRad),
      cos(thetaRad) * cos(phiRad)
    )
    DeltaTime(gradFrom(result._1), gradFrom(result._2))
  }
}

case class FullStandCoordinate(stand: StandCoordinate, labCoord: LabCoordinates)

case class StandCoordinate(theta: Double, time: Date)

case class LabCoordinates(longitude: Double, latitude: Double)

case class DeltaTime(delta: Double, t: Double)

case class EquatorialCoordinates(delta: Double, alpha: Double)

case class GalacticCoordinates(b: Double, l: Double)