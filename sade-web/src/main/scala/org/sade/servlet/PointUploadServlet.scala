package org.sade.servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import scala.collection.JavaConversions._
import java.util.{Enumeration}
import org.sade.starcoords.MeasuredPointCoordinates
import org.sade.model.{PointContent, SadeDB}
import org.squeryl.PrimitiveTypeMode

class PointUploadServlet extends HttpServlet with PrimitiveTypeMode {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val headerMap = req.getHeaderNames.asInstanceOf[Enumeration[String]].map(s => s -> req.getHeader(s)).toMap
    val coordinates = MeasuredPointCoordinates.fromMap(headerMap)
    val buffer = Array.ofDim[Byte](req.getContentLength)
    req.getInputStream.read(buffer)
    val pointContent = PointContent(buffer, coordinates)
    inTransaction {
      if (SadeDB.pointContents.lookup(pointContent.id).isEmpty) {
        SadeDB.pointContents.insert(pointContent)
      } else {
        resp.setStatus(409)
      }
    }
  }
}