package org.sade


object StubSadeServer extends Application {
  StubWebRunner.jdbcUrl = "jdbc:h2:~/sade"
  StubWebRunner.prepareJettyAndStart()
}