package org.sade.view

import net.liftweb.http.{XmlResponse, LiftResponse}
import org.sade.model.SadeDB
import org.squeryl.PrimitiveTypeMode
import net.liftweb.common.{Full, Box}


class AllPointIdView extends PrimitiveTypeMode {
  def dispatch(): Box[LiftResponse] = {
    val pointIds = from(SadeDB.points)(p => select(p)).iterator.map(p => <pointId>
      <timestamp>{p.id.a1.getTime}</timestamp>
      <channelId>{p.id.a2}</channelId>
    </pointId>).toSeq
    Full(XmlResponse(<pointIds>{pointIds}</pointIds>))
  }
}