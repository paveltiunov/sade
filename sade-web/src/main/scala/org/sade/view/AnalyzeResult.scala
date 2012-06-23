package org.sade.view

import xml.NodeSeq
import net.liftweb.http.js.jquery.JqJsCmds.JqOnLoad
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import net.liftweb.http.js.jquery.JqJE.{JqAttr, JqId}
import net.liftweb.http.js.JsCmds._
import net.liftweb.common.Full
import net.liftweb.http.js.JE.JsNull
import org.sade.model.SadeDB
import org.sade.view.AnalyzeResultImageView
import net.liftweb.http.{LiftView, TemplateFinder, SHtml}

class AnalyzeResult extends LiftView {
  def dispatch = {
    case expName if SadeDB.experiments.exists(_ == expName) => () => render(expName)
  }

  var viewMode = "plane"
  var logarithm = false
  var meanFilter = false

  def flagParameter(flag: Boolean, paramName: String): String = {
    (if (flag) {
      "&" + paramName + "=true"
    } else "")
  }

  def render(expName: String) = {
    val template = TemplateFinder.findAnyTemplate(List("templates-hidden", "analyze-result")).open_!
    def imageUrl = {
      "/analyze-result-image/" + expName + "?" +
        AnalyzeResultImageView.modeParam + "=" + viewMode +
        flagParameter(logarithm, AnalyzeResultImageView.logarithmParam) +
        flagParameter (meanFilter, AnalyzeResultImageView.meanFilterParam)
    }
    def loadImageCmd = JqId("result-image") ~> JqAttr("src", imageUrl) & SetHtml("statistics", <div>{statistics(expName)}</div>)
    val timeOutCmd = Interval(10000, SHtml.ajaxCall(JsNull, _ => loadImageCmd)._2)
    val head = <head>
      {Script(
      JqOnLoad(timeOutCmd & loadImageCmd)
    )}
    </head>
    val binding =
      "#exp-name *" #> expName &
      "#result-image [src]" #> imageUrl &
      "#mode-selector *" #> SHtml.ajaxSelect(Seq("plane" -> "Plane", "galactic" -> "Galactic"), Full("plane"), v => {
        viewMode = v
        loadImageCmd
      }) &
      "#logarithm-selector *" #> SHtml.ajaxCheckbox(false, v => {
        logarithm = v
        loadImageCmd
      }) &
      "#mean-filter-selector *" #> SHtml.ajaxCheckbox(false, v => {
        meanFilter = v
        loadImageCmd
      })
    <lift:surround with="default" at="content">{binding(template) ++ head}</lift:surround>
  }

  private def statistics(expName: String) = {
    SadeDB.analyzeResultCount(expName) + " / " + SadeDB.pointCount(expName)
  }
}

case class Interval(millis: Int, toDo: JsCmd) extends JsCmd {
  def toJsCmd = "setInterval(function() {" + toDo.toJsCmd + "}, " + millis + ");"
}