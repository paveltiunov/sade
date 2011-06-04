package org.sade.analyzers.starcoords

import org.scalatest.Spec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.sade.starcoords._
import org.scalatest.matchers.{MustMatchers}
import java.util.{TimeZone, GregorianCalendar}

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
    it("should convert from zero J2000") {
      val calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
      calendar.set(2000, 0, 1, 12, 0)
      StarCoordsConverter.sideralTime(FullStandCoordinate(
        StandCoordinate(0, calendar.getTime),
        LabCoordinates(0, 0))
      ) must be(4.89.plusOrMinus(1E-2))
    }
  }
}