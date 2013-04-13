package org.sade

import servlet.WorkerInitServlet
import org.h2.tools.Server


object StubSadeServer extends Application {
  Server.createTcpServer("-tcpAllowOthers").start()
  StubWebRunner.jdbcUrl = "jdbc:h2:~/sade"
  WorkerInitServlet.disableWorker = true
  StubWebRunner.prepareJettyAndStart(8080)
}

object StubSadeWorkServer extends Application {
  StubWebRunner.jdbcUrl = "jdbc:h2:tcp://localhost/~/sade"
  WorkerInitServlet.disableWorker = false
  WorkerInitServlet.akkaPort = 2553
  StubWebRunner.prepareJettyAndStart(8090)
}