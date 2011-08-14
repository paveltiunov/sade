package org.sade.snippet

import xml.NodeSeq
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.jquery.JqJsCmds.JqOnLoad
import net.liftweb.http.js.JsCmds.{After, SetHtml, Script}
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import net.liftweb.http.js.jquery.JqJE.{JqAttr, JqId}
import net.liftweb.http.js.JsCmds._

class AnalyzeResult {
  def render(in: NodeSeq) = {
    val timeOutCmd = Interval(intToTimeSpan(10000), JqId("result-image") ~> JqAttr("src", "/analyze-result-image"))
    <head>
      {Script(
      JqOnLoad(timeOutCmd)
    )}
    </head>
    <img src="/analyze-result-image" id="result-image"/>
  }
}

case class Interval(time: TimeSpan, toDo: JsCmd) extends JsCmd {
  def toJsCmd = "setInterval(function() {" + toDo.toJsCmd + "}, " + time.millis + ");"
}