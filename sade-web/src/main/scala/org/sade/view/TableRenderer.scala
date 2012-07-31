package org.sade.view

import xml.NodeSeq
import net.liftweb.util.Helpers._

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

  def renderTable[T](columns: (String, T => NodeSeq)*)(items: Seq[T]): NodeSeq = {
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
}
