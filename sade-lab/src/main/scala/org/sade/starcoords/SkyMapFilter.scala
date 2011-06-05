package org.sade.starcoords

object SkyMapFilter {
  def averageFilter(points: Seq[SkyMapPoint]) = {
    val meanValues = points.groupBy(p => p.dirIndex).mapValues(vs => vs.map(_.value).sum / vs.size)
    points.map(p => p.copy(value = p.value - meanValues(p.dirIndex)))
  }
}