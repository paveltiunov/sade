package org.sade.analyzers.starcoords

import org.scalatest.Spec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.sade.starcoords._
import org.scalatest.matchers.{MustMatchers}
import java.util.{TimeZone, GregorianCalendar}
import math._

@RunWith(classOf[JUnitRunner])
class StarCoordsConverterTest extends Spec with MustMatchers {
  def assertCoords(coords: FullStandCoordinate, expectedDelta: Double, expectedT: Double) {
    val deltaTime = StarCoordsConverter.toDeltaTime(coords)
    deltaTime.delta must be(expectedDelta.plusOrMinus(1E-5))
    deltaTime.t must be(expectedT.plusOrMinus(1E-5))
  }

  describe("The Converter") {
    it("should convert polar case") {
      assertCoords(FullStandCoordinate(StandCoordinate(15, null), LabCoordinates(15, 90)), 0d, -165d)
    }

    it("should convert equator case") {
      assertCoords(FullStandCoordinate(StandCoordinate(0, null), LabCoordinates(0, 0)), 90, -180)
      assertCoords(FullStandCoordinate(StandCoordinate(90, null), LabCoordinates(90, 0)), 0, -90)
      assertCoords(FullStandCoordinate(StandCoordinate(-90, null), LabCoordinates(-90, 0)), 0, 90)
    }
  }

  describe("Sideral time converter") {
    val j2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    j2000.set(2000, 0, 1, 12, 0)

    it("should convert from zero J2000") {
      StarCoordsConverter.sideralTime(FullStandCoordinate(
        StandCoordinate(0, j2000.getTime),
        LabCoordinates(0, 0))
      ) must be(280.46.plusOrMinus(1E-2))
    }

    it("should convert from zero J2000 + longitude") {
      StarCoordsConverter.sideralTime(FullStandCoordinate(
        StandCoordinate(0, j2000.getTime),
        LabCoordinates(90, 0))
      ) must be((370.46).plusOrMinus(1E-2))
    }
  }

  def assertGalacticCoords(coords: EquatorialCoordinates, expectedB: Double, expectedL: Double) {
    val result = StarCoordsConverter.toGalacticCoordinates(coords)
    result.b must be(expectedB.plusOrMinus(1E-2))
    result.l must be(expectedL.plusOrMinus(1E-2))
  }

  describe("Galactic converter") {
    it("should convert zero case") {
      assertGalacticCoords(EquatorialCoordinates(0, 0), -60.1887, 96.3379)
    }

    it("should convert some arbitrary case") {
      assertGalacticCoords(EquatorialCoordinates(90, 90), 27.1282, 122.9319)
    }
  }
}