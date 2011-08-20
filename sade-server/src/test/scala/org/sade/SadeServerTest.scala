package org.sade

import org.scalatest.junit.MustMatchersForJUnit
import org.junit.Test
import org.sade.SadeServer._


class SadeServerTest extends MustMatchersForJUnit {
  @Test
  def parseOption() {
    parseOptions(Array("--port", "8080", "--jdbc-url", "jdbc://123")) must be (Right(Map(
      "port" ->"8080", "jdbc-url" -> "jdbc://123"
    )))
    parseOptions(Array("--port", "8080", "--jdbc-url=123")) must be (Left("Invalid option: --jdbc-url=123"))
  }
}