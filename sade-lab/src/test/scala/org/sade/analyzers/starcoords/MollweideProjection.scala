package org.sade.analyzers.starcoords

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.sade.starcoords.MollweideProjection
import math._

@RunWith(classOf[JUnitRunner])
class MollweideProjection extends Spec with MustMatchers {
  def assertProjection(longitude: Double, latitude: Double, expectedX: Double, expectedY: Double) {
    val result = MollweideProjection.project(longitude, latitude)
    result._1 must be(expectedX.plusOrMinus(1E-3))
    result._2 must be(expectedY.plusOrMinus(1E-3))
  }

  describe("Mollweide projector") {
    it("should project zero case") {
      assertProjection(0, 0, 0, 0)
    }

    it("should project left case") {
      assertProjection(-180, 0, -2*sqrt(2), 0)
    }
  }
}