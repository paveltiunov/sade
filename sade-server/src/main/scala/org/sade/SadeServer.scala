package org.sade

import org.slf4j.LoggerFactory
import com.mchange.v2.c3p0.ComboPooledDataSource
import javax.naming.InitialContext
import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.webapp.WebAppContext
import org.squeryl.PrimitiveTypeMode

object SadeServer extends Application with PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def prepareDataSource(jdbcUrl: String) {
    val source = new ComboPooledDataSource()
    source.setJdbcUrl(jdbcUrl)
    val initialContext = new InitialContext()
    initialContext.bind("SadeDS", source)
  }

  def prepareJetty(jdbcUrl: String ) = {
    prepareDataSource(jdbcUrl)

    val server = new Server
    val scc = new SelectChannelConnector
    scc.setPort(8080)
    server.setConnectors(Array(scc))

    val context = new WebAppContext()
    context.setServer(server)
    context.setContextPath("/")
    context.setWar(
      getClass
      .getResource(getClass.getSimpleName + ".class").toString
        .replace(getClass.getName.replace(".", "/") + ".class", "")
      + "sade-web/"
    )
    server.addHandler(context)

    server
  }

  val server = prepareJetty("jdbc:h2:~/sade")

  server.start()
}
