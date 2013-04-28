package org.sade.view

import scala.xml._
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.JsNull
import org.sade.model.{Point, SadeDB}
import net.liftweb.http.{LiftView, TemplateFinder, SHtml}
import java.sql.Timestamp
import org.sade.servlet.SadeActors
import akka.util.{Duration, Timeout}
import java.util.concurrent.TimeUnit
import TableRenderer._
import org.sade.worker._
import org.sade.view.Refreshable._
import org.sade.worker.GetAnalyzeState
import net.liftweb.http.js.jquery.JqJE.JqId
import org.sade.worker.StopExp
import net.liftweb.common.Full
import org.sade.model.AnalyzeToken
import org.sade.worker.AnalyzeState
import net.liftweb.http.js.jquery.JqJE.JqAttr
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.jquery.JqJsCmds.JqOnLoad
import org.sade.worker.StartExp
import net.liftweb.http.js.jquery.JqJsCmds.JqSetHtml
import akka.pattern.ask
import akka.dispatch.Await

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
        flagParameter(meanFilter, AnalyzeResultImageView.meanFilterParam)
    }
    def loadImageCmd = JqId("result-image") ~> JqAttr("src", imageUrl) & SetHtml("statistics", <div>
      {statistics(expName)}
    </div>)
    val timeOutCmd = Interval(10000, SHtml.ajaxCall(JsNull, _ => loadImageCmd)._2)
    val head = <head>
      {Script(
        JqOnLoad(timeOutCmd & loadImageCmd)
      )}
    </head>
    val binding =
      "#exp-name *" #> expName &
        "#start-button *" #> startButton(expName) &
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
        "#dataMode *" #> pills(
          "#pointMap *" bindToPill("Point map", "#result-image [src]" #> imageUrl),
          "#pointTable *" bindToPill("Point table", table(expName))
        )
    <lift:surround with="default" at="content">
      {binding(template) ++ head}
    </lift:surround>
  }

  case object CurrentlyAnalyzing

  case object Timeouted

  case object Pending

  def analyzeStatusFilter = comboBoxFilter(".filter", Seq(
    CurrentlyAnalyzing -> "Currently analyzing",
    Timeouted -> "Timeouted",
    Pending -> "Pending"
  ))

  def table(expName: String): NodeSeq => NodeSeq = {
    val selectedPointIds = scala.collection.mutable.Set[Timestamp]()
    filtering(".filtered", analyzeStatusFilter) {
      case Seq(analyzeStatusFilter) => refreshable(updateCmd => {
        implicit val timeout = Timeout(5 seconds)
        val status = Await.result((SadeActors.mainWorker ? GetAnalyzeStatus).mapTo[AnalyzeStatus], Duration(5, TimeUnit.SECONDS))
        var tableSeq = SadeDB.analyzeResultAndTokenStatus(expName).toSeq
        tableSeq = analyzeStatusFilter.map {
          case CurrentlyAnalyzing => tableSeq.filter(i => status.analyzeStarted.contains(i._1.id))
          case Timeouted => tableSeq.filter(i => status.timeouted.contains(i._1.id))
          case Pending => tableSeq.filter(i => status.analyzing.contains(i._1.id))
        }.getOrElse(tableSeq)

        type Item = (Point, Option[org.sade.model.AnalyzeResult], Option[AnalyzeToken])

        paging[Item](".paging", items => ".toolbar *" #> actionToolbar(
          action("icon-refresh", "btn-warning", () => SadeDB.dropAnalyze(selectedPointIds.toSet)),
          action("icon-trash", "btn-danger", () => SadeDB.dropPoints(selectedPointIds.toSet))
        )(updateCmd) & ".tableContent" #> renderTable[Item](
          multiSelectColumn[(Point, Option[org.sade.model.AnalyzeResult], Option[AnalyzeToken]), Timestamp](selectedPointIds, _._1.id, tableSeq, updateCmd()),
          textColumn("Time", _._1.id.toString),
          textColumn("Point Index", _._1.pointIndex.toString),
          textColumn("Dir index", _._1.dirIndex.toString),
          textColumn("Direction", _._1.direction.toString),
          textColumn("Value", _._2.map(_.meanValue.toString).getOrElse("")),
          textColumn("Absolute error", _._2.map(_.absoluteError.toString).getOrElse("")),
          textColumn("Frequency", _._2.map(_.meanFrequency.toString).getOrElse("")),
          textColumn("Elapsed analyze time", i => {
            if (status.timeouted.contains(i._1.id))
              "Timeouted"
            else if (status.analyzeStarted.contains(i._1.id))
              "%s s".format((System.currentTimeMillis() - status.analyzeStarted(i._1.id)) / (1000))
            else if (status.analyzing.contains(i._1.id))
              "Pending"
            else
              ""
          }),
          textColumn("Analyze tries", i => status.tries(i._1.id).toString),
          downloadLinkColumn("Source", i => "/point-source/%s".format(i._1.id.getTime))
        )(items))(tableSeq)
      })
    }

  }


  def startButton(expName: String): Elem = {
    implicit val timeout = Timeout(5 seconds)
    val started = Await.result((SadeActors.mainWorker ? GetAnalyzeState(expName)).mapTo[AnalyzeState].map(_.analyzing), Duration(5, TimeUnit.SECONDS))
    if (!started) {
      SHtml.ajaxButton("Start analyze", () => {
        SadeActors.mainWorker ! StartExp(expName)
        JqSetHtml("start-button", startButton(expName))
      }, "class" -> "btn btn-success")
    } else {
      SHtml.ajaxButton("Stop analyze", () => {
        SadeActors.mainWorker ! StopExp(expName)
        JqSetHtml("start-button", startButton(expName))
      }, "class" -> "btn btn-danger")
    }
  }

  private def statistics(expName: String) = {
    SadeDB.analyzeResultCount(expName) + " / " + SadeDB.pointCount(expName)
  }
}

case class Interval(millis: Int, toDo: JsCmd) extends JsCmd {
  def toJsCmd = "setInterval(function() {" + toDo.toJsCmd + "}, " + millis + ");"
}