package org.sade.view

import xml.{Null, UnprefixedAttribute, Text, NodeSeq}
import net.liftweb.http.js.jquery.JqJsCmds.{Show, Hide, JqOnLoad}
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.util.Helpers._
import net.liftweb.http.js.jquery.JqJE.{JqAttr, JqId}
import net.liftweb.http.js.JsCmds._
import net.liftweb.common.Full
import net.liftweb.http.js.JE.JsNull
import org.sade.model.{AnalyzeToken, Point, SadeDB}
import org.sade.view.AnalyzeResultImageView
import net.liftweb.http.{LiftView, TemplateFinder, SHtml}
import org.sade.starcoords.SkyMapPoint

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
      }) &
      "#dataMode *" #> (new DataModes(expName).modePills)
    <lift:surround with="default" at="content">{binding(template) ++ head}</lift:surround>
  }

  private def statistics(expName: String) = {
    SadeDB.analyzeResultCount(expName) + " / " + SadeDB.pointCount(expName)
  }
}

class DataModes(expName: String) {
  var currentMode: DataMode = PlotMode

  case object PlotMode extends DataMode {
    def name = "Point map"

    def contentId = "pointMap"
  }

  case object TableMode extends DataMode {
    def name = "Point table"

    def contentId = "pointTable"

    def table: NodeSeq = TableRenderer.renderTable[(Point, Option[org.sade.model.AnalyzeResult], Option[AnalyzeToken])](
      ("Time", {p => Text(p._1.id.toString)}),
      ("Point Index", {p => Text(p._1.pointIndex.toString)}),
      ("Dir index", {p => Text(p._1.dirIndex.toString)}),
      ("Direction", {p => Text(p._1.direction.toString)}),
      ("Value", {p => Text(p._2.map(_.meanValue.toString).getOrElse(""))}),
      ("Absolute error", {p => Text(p._2.map(_.absoluteError.toString).getOrElse(""))}),
      ("Frequency", {p => Text(p._2.map(_.meanFrequency.toString).getOrElse(""))}),
      ("Analyze started", {p => Text(p._3.map(_.analyzeStarted.toString).getOrElse(""))}),
      ("Actions", {p => SHtml.a(() => {
        SadeDB.dropPoint(p._1.id)
        tableUpdateCmd
      }, <i class="icon-trash icon-white"></i>, "class" -> "btn btn-danger")})
    )(SadeDB.analyzeResultAndTokenStatus(expName).toSeq)

    def tableUpdateCmd = SetHtml(contentId, table)

    override def update(enabled: Boolean) = super.update(enabled) & (if (enabled) tableUpdateCmd else JsCmds.Noop)
  }

  trait DataMode {
    def name: String

    def contentId: String

    def update(enabled: Boolean): JsCmd = if (enabled) Show(contentId) else Hide(contentId)
  }

  val modes = Seq(PlotMode, TableMode)

  def modePills: NodeSeq = {
    modes.map(m => <li>
      {SHtml.a(() => {
        currentMode = m
        SetHtml("dataMode", modePills) & modes.map(mm => mm.update(mm == currentMode)).reduceLeft(_ & _)
      }, Text(m.name))}
    </li> % (if (m == currentMode) new UnprefixedAttribute("class", "active", Null) else Null))
  }

}

case class Interval(millis: Int, toDo: JsCmd) extends JsCmd {
  def toJsCmd = "setInterval(function() {" + toDo.toJsCmd + "}, " + millis + ");"
}