package org.sade.servlet

import javax.servlet.http.HttpServlet
import org.sade.worker.MainWorker


class WorkerInitServlet extends HttpServlet {
  val worker = new MainWorker()

  override def init() {
    worker.startWorking()
  }
}