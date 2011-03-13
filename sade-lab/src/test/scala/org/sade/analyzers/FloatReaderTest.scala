package org.sade.analyzers

import org.junit.Test
import org.scalatest.junit.ShouldMatchersForJUnit
class FloatReaderTest extends ShouldMatchersForJUnit {
  @Test
  def gutter {
    val reader = new FloatReader(getClass.getResource("test_00000.txt").openStream)
    reader.chunkStream.take(16).print
  }
}