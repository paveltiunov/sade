package org.sade.analyzers.starcoords

import org.scalatest.Spec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.sade.starcoords.{DeltaTime, LabCoordinates, StarCoordsConverter}

@RunWith(classOf[JUnitRunner])
class StarCoordsConverterTest extends Spec with MustMatchers {
  def assertCoords(coords: LabCoordinates, expectedDelta: Double, expectedT: Double): Unit = {
    val deltaTime = StarCoordsConverter.toDeltaTime(coords)
    deltaTime.delta must be(expectedDelta.plusOrMinus(1E-5))
    deltaTime.t must be(expectedT.plusOrMinus(1E-5))
  }

  describe("The Converter") {
    it ("should convert polar case") {
      assertCoords(LabCoordinates(15, 90), 0d, -165d)
    }

    it ("should convert equator case") {
      assertCoords(LabCoordinates(0, 0), 90, -180)
      assertCoords(LabCoordinates(90, 0), 0, -90)
      assertCoords(LabCoordinates(-90, 0), 0, 90)
    }
  }
}