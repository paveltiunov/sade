package org.sade.starcoords

import cern.colt.matrix.DoubleMatrix1D
import annotation.tailrec
import math._

object MollweideProjection {
  val eps = 1E-3

  private def findTheta(latitudeRad: Double) = {
    if (abs(latitudeRad) - Pi/2 < eps) latitudeRad
    findThetaRecursive(latitudeRad, latitudeRad)
  }

  @tailrec
  private def findThetaRecursive(initialTheta: Double, latitudeRad: Double): Double = {
    val doubledInitial = 2 * initialTheta
    val next = initialTheta - (doubledInitial + sin(doubledInitial) - Pi * sin(latitudeRad)) / (2 + 2*cos(doubledInitial))
    if (abs(next - initialTheta) < eps) next else findThetaRecursive(next, latitudeRad)
  }

  def project(longitude: Double, latitude: Double): (Double, Double) = {
    val longitudeRad = longitude.toRadians % Pi
    val latitudeRad = latitude.toRadians % Pi
    val theta = findTheta(latitudeRad)
    val sqrt2 = sqrt(2)
    (2 * sqrt2 / Pi * longitudeRad * cos(theta), -sqrt2 * sin(theta))
  }

  def project(galacticCoordinates: GalacticCoordinates): (Double, Double) = {
    project(galacticCoordinates.l, galacticCoordinates.b)
  }
}