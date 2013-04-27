package org.sade.view

import scala.xml.{Text, NodeSeq}
import net.liftweb.util.Helpers._
import scala.collection._
import net.liftweb.http.SHtml
import scala.Predef._
import scala.xml.Text
import net.liftweb.http.js.{JsCmd, JsCmds}

object TableRenderer {
  def template =
    <table class="table table-bordered">
      <thead>
        <tr>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td></td>
        </tr>
      </tbody>
    </table>

  def renderTable[T](columns: (NodeSeq, T => NodeSeq)*)(items: Seq[T]): NodeSeq = {
    val tableBind = "table *" #> (
      "thead *" #> (
        "tr *" #> (
          "th *" #> columns.map(_._1)
          )
        )
        &
        "tbody *" #> (
          "tr *" #> items.map(i => "td *" #> (columns.map(_._2(i))))
          )
      )
    tableBind(template)
  }

  def textColumn[T](name: String, valueFun: T => String): (NodeSeq, T => NodeSeq) = {
    (Text(name), valueFun.andThen(Text))
  }

  def multiSelectColumn[T,I](selectedIds: mutable.Set[I], idFun: T => I, items: Seq[T], tableUpdateCmd: => JsCmd): (NodeSeq, T => NodeSeq) = {
    (SHtml.ajaxCheckbox(selectedIds.size == items.size, v => {
      if (v) selectedIds ++= items.map(idFun) else selectedIds.clear()
      tableUpdateCmd
    }), {
      p => SHtml.ajaxCheckbox(selectedIds.contains(idFun(p)), checked => {
        if (checked) selectedIds += idFun(p) else selectedIds -= idFun(p)
        JsCmds.Noop
      })
    })
  }
}
