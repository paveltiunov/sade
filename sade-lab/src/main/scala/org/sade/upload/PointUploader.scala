package org.sade.upload

import org.sade.lab.PointSource
import org.apache.commons.httpclient.{HttpVersion, HttpClient}
import org.sade.starcoords.MeasuredPointCoordinates
import java.util.Date
import org.apache.commons.httpclient.methods.{GetMethod, ByteArrayRequestEntity, PostMethod}
import xml.XML

class PointUploader(serverUrl: String) {
  val httpClient = {
    val httpClient = new HttpClient()
    httpClient.getParams.setVersion(HttpVersion.HTTP_1_1)
    httpClient
  }

  var loadedIds: Set[Date] = Set()

  def uploadPoint(point: PointSource) = {
    if (!loadedIds.contains(point.coordinate.time)) {
      val method = new PostMethod(serverUrl + "/upload-point")
      MeasuredPointCoordinates.toMap(point.coordinate).foreach(t => method.setRequestHeader(t._1, t._2))
      method.setRequestEntity(new ByteArrayRequestEntity(point.content(), "binary/octet-stream"))
      val uploadConflict = httpClient.executeMethod(method).ensuring(r => r == 200 || r == 409) == 200
      if (uploadConflict) {
        updateLoadedIds()
      }
      uploadConflict
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