package org.sade

import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.webapp.WebAppContext
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.H2Adapter
import org.squeryl.{PrimitiveTypeMode, SessionFactory, Session}
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID
import javax.naming.InitialContext
import java.sql.DriverManager
import org.junit.{After, Before}
import servlet.WorkerInitServlet

trait StubWebServerRunner extends PrimitiveTypeMode {
  val session = Session.create(DriverManager.getConnection(StubWebRunner.jdbcUrl), new H2Adapter)

  @Before
  def startJetty() {
    WorkerInitServlet.inTest = true
    StubWebRunner.prepareJettyAndStart()
    session.connection.setAutoCommit(false)
    session.bindToCurrentThread
  }

  @After
  def tearDownConnection() {
    session.unbindFromCurrentThread
    session.connection.rollback()
    session.connection.close()
  }
}

object StubWebRunner {
  var jdbcUrl = "jdbc:h2:mem:stub"

  var prepared = false

   def prepareDataSource(jdbcUrl: String) {
    val source = new ComboPooledDataSource()
    source.setJdbcUrl(jdbcUrl)
    val initialContext = new InitialContext()
    initialContext.bind("SadeDS", source)
  }

  def prepareJettyAndStart() {
    if (!prepared) {
      prepareDataSource(jdbcUrl)

      val server = new Server
      val scc = new SelectChannelConnector
      scc.setPort(8080)
      server.setConnectors(Array(scc))

      val context = new WebAppContext()
      context.setServer(server)
      context.setContextPath("/")
      if (new File("sade-web/src/main/webapp").exists)
        context.setWar("sade-web/src/main/webapp")
      else
        context.setWar("src/main/webapp")

      server.addHandler(context)

      server.start()
      prepared = true
    }
  }
}