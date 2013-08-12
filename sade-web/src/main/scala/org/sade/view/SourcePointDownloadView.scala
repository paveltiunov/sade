package org.sade.view

import org.squeryl.PrimitiveTypeMode
import net.liftweb.http.{InMemoryResponse, LiftResponse}
import net.liftweb.common.Box
import org.sade.model.{Point, SadeDB}
import java.sql.Timestamp
import org.squeryl.dsl.CompositeKey2

class SourcePointDownloadView extends PrimitiveTypeMode {
  def dispatch(id: Long, channelId: String): Box[LiftResponse] = {
    val idTimestamp = new Timestamp(id)
    val content = Point.unzippedContentBy(new CompositeKey2(idTimestamp, channelId))
    Box(InMemoryResponse(content, List(
      "Content-Type" -> "application/octet-stream",
      "Content-Disposition" -> "attachment; filename=%s.bin;".format(idTimestamp),
      "Content-Transfer-Encoding" -> "binary"
    ), Nil, 200))
  }

}
