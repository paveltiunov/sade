package org.sade

import model.Point._
import java.sql.Timestamp
import model.{PointContent, SadeDB, Point}
import starcoords.Directions


trait Fixtures {
  def setupPointContentFixture() {
    val id = new Timestamp(123)
    val pointContent = Point(id, "foo", 1, 2, 3, Directions.Backward)
    SadeDB.points.insert(pointContent)
    SadeDB.pointContents.insert(PointContent(id, "foo".getBytes))
  }
}