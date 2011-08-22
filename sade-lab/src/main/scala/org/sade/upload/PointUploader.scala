package org.sade.upload

import org.sade.lab.PointSource
import org.apache.commons.httpclient.{HttpVersion, HttpClient}
import org.sade.starcoords.MeasuredPointCoordinates
import java.util.Date
import org.apache.commons.httpclient.methods.{GetMethod, ByteArrayRequestEntity, PostMethod}
import xml.XML
import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.params.HttpMethodParams
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class PointUploader(serverUrl: String) {
  val httpClient = {
    val httpClient = new HttpClient()
    httpClient.getParams.setVersion(HttpVersion.HTTP_1_1)
    httpClient
  }

  var loadedIds: Set[Date] = Set()

  private def gzippedContent(point: PointSource): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    val gZIPOutputStream = new GZIPOutputStream(outputStream)
    gZIPOutputStream.write(point.content())
    gZIPOutputStream.close()
    outputStream.toByteArray
  }

  def uploadPoint(point: PointSource) = {
    if (!loadedIds.contains(point.coordinate.time)) {
      val method = new PostMethod(serverUrl + "/upload-point")
      MeasuredPointCoordinates.toMap(point.coordinate).foreach(t => method.setRequestHeader(t._1, t._2))
      method.setRequestHeader("expName", point.expName)
      val fileName = point.coordinate.time.toString
      val entity = new MultipartRequestEntity(Array(
        new FilePart(fileName, new ByteArrayPartSource(fileName, gzippedContent(point)))
      ),  new HttpMethodParams())
      method.setRequestEntity(entity)
      val isOk = httpClient.executeMethod(method).ensuring(r => r == 200 || r == 409) == 200
      if (!isOk) {
        updateLoadedIds()
      }
      isOk
    } else false
  }

  def updateLoadedIds() {
    val method = new GetMethod(serverUrl + "/loaded-point-ids")
    if (httpClient.executeMethod(method) == 200) {
      val loadedXml = XML.load(method.getResponseBodyAsStream)
      val pointIds = loadedXml \ "pointId"
      loadedIds ++= pointIds.map(_.text.toLong).map(new Date(_))
    }
  }
}