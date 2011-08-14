package org.sade.servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import scala.collection.JavaConversions._
import java.util.{Enumeration}
import org.sade.starcoords.MeasuredPointCoordinates
import org.sade.model.{PointContent, SadeDB}
import org.squeryl.PrimitiveTypeMode
import org.apache.commons.fileupload.servlet.ServletFileUpload
import scala.collection.JavaConversions._
import org.apache.commons.fileupload.FileItem
import java.io.InputStream
import org.apache.commons.fileupload.disk.DiskFileItemFactory

class PointUploadServlet extends HttpServlet with PrimitiveTypeMode {
  def readContent(inputStream: InputStream): Array[Byte] = {
    val buffer = Array.ofDim[Byte](inputStream.available())
    inputStream.read(buffer)
    buffer
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val headerMap = req.getHeaderNames.asInstanceOf[Enumeration[String]].map(s => s -> req.getHeader(s)).toMap
    val coordinates = MeasuredPointCoordinates.fromMap(headerMap)
    val servletFileUpload = new ServletFileUpload(new DiskFileItemFactory())
    val fileItems = servletFileUpload.parseRequest(req).asInstanceOf[java.util.List[FileItem]]
    val buffer = readContent(fileItems.head.getInputStream)
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