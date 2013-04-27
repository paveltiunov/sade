package org.sade.view

import scala.xml.{Null, UnprefixedAttribute, Text, NodeSeq}
import net.liftweb.http.js.JsCmd
import java.util.UUID
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.SetHtml
import scala.Null
import net.liftweb.http.js.jquery.JqJsCmds.{JqSetHtml, Hide, Show}
import net.liftweb.util.BindHelpers._
import net.liftweb.util.CssSel
import net.liftweb.common.Box

object Refreshable {
  def refreshable(renderFun: (() => JsCmd) => (NodeSeq) => NodeSeq, id: String = UUID.randomUUID().toString, initialRender: Boolean = true): NodeSeq => NodeSeq = {
    template => {
      def render: NodeSeq = renderFun(() => JqJsCmds.JqSetHtml(id, render))(template)
      val content = render //TODO: hack for initial renderFun call
      <div id={id}>
        {if (initialRender) content else NodeSeq.Empty}
      </div>
    }
  }

  def actionToolbar(actions: ((() => JsCmd) => NodeSeq)*)(updateFun: () => JsCmd): NodeSeq = {
    actions.map(_(updateFun)).flatten
  }

  def action(iconClass: String, btnClass: String, action: () => Unit): ((() => JsCmd) => NodeSeq) = {
    updateFun => <div class="btn-group">
      {SHtml.a(() => {
        action()
        updateFun()
      }, <i class={"icon-white " + iconClass}></i>, "class" -> ("btn " + btnClass))}
    </div>
  }

  def pillTemplate: NodeSeq =
    <ul class="nav nav-pills">
      <li class="item"></li>
    </ul>

  case class Pill(name: String, containerSel: String, sel: NodeSeq => NodeSeq)

  implicit def toPillHelper(containerSel: String) = new {
    def bindToPill(name: String, sel: NodeSeq => NodeSeq) = Pill(name, containerSel, sel)
  }

  def pills(pill: Pill*): NodeSeq => NodeSeq = {
    template => {
      val idToPill = pill.map(UUID.randomUUID().toString -> _)
      var currentMode: String = idToPill.head._1

      var idToUpdateCmd = Map[String, () => JsCmd]()
      val containers = idToPill.map { case (id, pill) => pill.containerSel #> refreshable(updateCmd => {
        idToUpdateCmd += id -> updateCmd
        pill.sel
      }, id, id == currentMode)}.reduceLeft(_ & _).apply(template)

      def updatePillContentCmd: JsCmd = {
        idToPill.map(mm => if (mm._1 == currentMode) Show(mm._1) & idToUpdateCmd(mm._1)() else Hide(mm._1)).reduceLeft(_ & _)
      }
      val pills = refreshable(updateCmd => {
        ".item" #> idToPill.map(m => {
          "* [class+]" #> (if (m._1 == currentMode) "active" else "") &
          "* *" #> SHtml.a(() => {
            currentMode = m._1
            updateCmd() & updatePillContentCmd
          }, Text(m._2.name))

        })
      })(pillTemplate)
      pills ++ containers
    }
  }

  def comboBoxFilter[T](selector: String, options: Seq[(T, String)], default: Option[T] = None): (Option[Any] => JsCmd) => (NodeSeq => NodeSeq) = updateValue => {
    selector #> SHtml.ajaxSelectObj[Option[T]](Seq(None -> "") ++ options.map{case (key, value) => Option(key) -> value}, Box(Option(default)), updateValue)
  }

  def filtering(filteredSelector: String, filters: ((Option[Any] => JsCmd) => (NodeSeq => NodeSeq))*)(filteringValue: Seq[Option[Any]] => NodeSeq => NodeSeq): NodeSeq => NodeSeq = {
    var indexToValue = Map[Int, Option[Any]]().withDefault(i => None)
    val filterToIndex = filters.zipWithIndex
    var updCmd: () => JsCmd = null //TODO
    val filteredBind = filteredSelector #> refreshable(
      updateCmd => {
        updCmd = updateCmd
        filteringValue((0 until filters.size).map(indexToValue))
      }
    )
    val filterBind = filterToIndex.map {case (filter, index) => "*" #> filter(value => {
      indexToValue += index -> value
      updCmd()
    })}.reduceLeft(_ & _)
    filterBind & filteredBind
  }
}
