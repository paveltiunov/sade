package org.sade.upload

import org.sade.lab.PointSource
import java.net.URL
import org.apache.commons.httpclient.methods.{ByteArrayRequestEntity, PostMethod}
import org.apache.commons.httpclient.{HttpVersion, HttpClient}
import org.sade.starcoords.MeasuredPointCoordinates

class PointUploader {
  def uploadPoint(point: PointSource) {
    val httpClient = new HttpClient()
    httpClient.getParams.setVersion(HttpVersion.HTTP_1_1)
    val method = new PostMethod("http://localhost:8080/upload-point")
    MeasuredPointCoordinates.toMap(point.coordinate).foreach(t => method.setRequestHeader(t._1, t._2))
    method.setRequestEntity(new ByteArrayRequestEntity(point.content(), "binary/octet-stream"))
    httpClient.executeMethod(method)
  }
}