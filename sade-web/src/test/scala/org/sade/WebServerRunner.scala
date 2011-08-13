package org.sade

import model.SadeDB
import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.webapp.WebAppContext
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.H2Adapter
import org.squeryl.{PrimitiveTypeMode, SessionFactory, Session}

trait WebServerRunner extends PrimitiveTypeMode {
  def prepareJetty(jdbcUrl: String = "jdbc:h2:mem:") = {
    val server = new Server
    val scc = new SelectChannelConnector
    scc.setPort(8080)
    server.setConnectors(Array(scc))

    val context = new WebAppContext()
    context.setServer(server)
    context.setContextPath("/")
    context.setWar("sade-web/src/main/webapp")

    server.addHandler(context)
    val source = new ComboPooledDataSource()
    source.setJdbcUrl(jdbcUrl)
    SessionFactory.concreteFactory = Some(() => {
      Session.create(source.getConnection, new H2Adapter)
    })

    inTransaction {
      SadeDB.create
    }

    server
  }
}