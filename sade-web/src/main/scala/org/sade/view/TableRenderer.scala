package org.sade.view

import scala.xml.{Text, NodeSeq}
import net.liftweb.util.Helpers._
import scala.collection._
import net.liftweb.http.SHtml
import scala.Predef._
import scala.xml.Text
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.util.CssSel

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

  def pagingTemplate =
    <span class="pull-right">
      <span class="btn-group">
        <a class="btn page-left"><i class="icon-chevron-left"></i></a>
        <a class="btn page-right"><i class="icon-chevron-right"></i></a>
      </span>
    </span>

  def paging[T](selector: String, renderFun: Seq[T] => CssSel, pageSize: Int = 100)(items: Seq[T]): NodeSeq => NodeSeq = {
    var currentPage = 0
    val pageCount = if (items.size % pageSize == 0) items.size / pageSize else items.size / pageSize + 1
    Refreshable.refreshable(updateCmd => {
      val pageBinds = ".page-left" #> (if (currentPage > 0) SHtml.a(() => {
        currentPage -= 1
        updateCmd()
      }, <i class="icon-chevron-left"></i>) else <a class="disabled"><i class="icon-chevron-left"></i></a>) &
        ".page-right" #> (if (currentPage < pageCount - 1) SHtml.a(() => {
          currentPage += 1
          updateCmd()
        }, <i class="icon-chevron-right"></i>)else <a class="disabled"><i class="icon-chevron-right"></i></a>)
      selector #> pageBinds(pagingTemplate) & renderFun(items.drop(currentPage * pageSize).take(pageSize))
    })
  }

  def textColumn[T](name: String, valueFun: T => String): (NodeSeq, T => NodeSeq) = {
    (Text(name), valueFun.andThen(Text))
  }

  def downloadLinkColumn[T](name: String, linkFun: T => String): (NodeSeq, T => NodeSeq) = {
    (Text(name), linkFun.andThen(s => <a class="btn btn-success" href={s}><i class="icon-white icon-download"></i></a>))
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
