package org.sade

import servlet.WorkerInitServlet


object StubSadeServer extends Application {
  StubWebRunner.jdbcUrl = "jdbc:h2:~/sade"
  WorkerInitServlet.disableWorker = true
  StubWebRunner.prepareJettyAndStart()
}