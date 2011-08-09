package org.sade.servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.FileItem
import scala.collection.JavaConversions._
import java.util.{Enumeration, List}
import org.sade.starcoords.MeasuredPointCoordinates

class PointUploadServlet extends HttpServlet {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val headerMap = req.getHeaderNames.asInstanceOf[Enumeration[String]].map(s => s -> req.getHeader(s)).toMap
    val coordinates = MeasuredPointCoordinates.fromMap(headerMap)
    println(coordinates.pointCount)
    val buffer = Array.ofDim[Byte](req.getContentLength)
    req.getInputStream.read(buffer)
    println(new String(buffer))
  }
}