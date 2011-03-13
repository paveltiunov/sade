package org.sade.analyzers.math

import org.junit.Test
import org.scalatest.junit.ShouldMatchersForJUnit
import org.apache.commons.math.transform.FastFourierTransformer
import scala.math._


class FFTTest extends ShouldMatchersForJUnit with ComplexHelpers {
  @Test
  def gutter {
    val doubles = (0 until 512).map(i => random).toArray
    val transform = FFT.transform(doubles, doubles.length)
    val fourierTransformer = new FastFourierTransformer
    val expectedTransform = fourierTransformer.transform(doubles).map(_ / doubles.length)
    transform.zip(expectedTransform).map{ case(a,b) => (a - b).abs should be < 1E-10}
  }
}