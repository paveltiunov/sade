package org.sade.starcoords
import scala.math._

object SkyMapFilter {
  def averageFilter(points: Seq[SkyMapPoint]) = {
    val meanValues = points.groupBy(p => p.dirIndex).mapValues(vs => vs.map(_.value).sum / vs.size)
    points.map(p => p.copy(value = p.value - meanValues(p.dirIndex)))
  }

  def logarithmFilter(points: Seq[SkyMapPoint]) = {
    points.map(p => p.copy(value = log10(p.value)))
  }
}