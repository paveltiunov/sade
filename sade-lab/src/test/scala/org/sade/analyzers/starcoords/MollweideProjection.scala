package org.sade.analyzers.starcoords

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import math._
import org.sade.starcoords.MollweideProjection

@RunWith(classOf[JUnitRunner])
class MollweideProjection extends Spec with MustMatchers {
  def assertProjection(longitude: Double, latitude: Double, expectedX: Double, expectedY: Double) {
    val result = MollweideProjection.project(longitude, latitude)
    result._1 must be(expectedX.plusOrMinus(1E-3))
    result._2 must be(expectedY.plusOrMinus(1E-3))
  }

  def assertEqual(longitude1: Double, latitude1: Double, longitude2: Double, latitude2: Double) {
    val result1 = MollweideProjection.project(longitude1, latitude1)
    val result2 = MollweideProjection.project(longitude2, latitude2)
    result1._1 must be (result2._1)
    result1._2 must be (result2._2)
  }

  describe("Mollweide projector") {
    it("should project zero case") {
      assertProjection(0, 0, 0, 0)
    }

    it("should project left case") {
      assertProjection(180, 0, -2*sqrt(2), 0)
    }

    it("should truncate over angles") {
      assertEqual(270, 0, -90, 0)
      assertEqual(-270, 0, 90, 0)
      assertEqual(270+360, 0, -90, 0)
      assertEqual(-270-360, 0, 90, 0)
    }
  }
}