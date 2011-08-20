package org.sade

import org.slf4j.LoggerFactory
import com.mchange.v2.c3p0.ComboPooledDataSource
import javax.naming.InitialContext
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.webapp.WebAppContext
import org.squeryl.PrimitiveTypeMode
import org.mortbay.jetty.Server
import org.h2.tools.{Server => HServer}

object SadeServer extends Application with PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def prepareDataSource(jdbcUrl: Option[String]) {
    val source = new ComboPooledDataSource()
    jdbcUrl match {
      case Some(url) => source.setJdbcUrl(url)
      case None => {
        HServer.createTcpServer().start()
        source.setJdbcUrl("jdbc:h2:tcp://localhost/~/sade")
      }
    }
    val initialContext = new InitialContext()
    initialContext.bind("SadeDS", source)
  }

  def prepareJetty(jdbcUrl: Option[String], portNumber: Int) = {
    prepareDataSource(jdbcUrl)

    val server = new Server
    val scc = new SelectChannelConnector
    scc.setPort(portNumber)
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

  def parseOptions(args: Array[String]): Either[String, Map[String, String]] = {
    val result = args.zipWithIndex.groupBy {
      case (_, index) => index / 2
    }.values.map(_.map(_._1)).map {
      case Array(name, value) if name.startsWith("--") => Right((name.replaceFirst("--", "") -> value))
      case array => Left("Invalid option: " + array.mkString(" "))
    }
    result.find(_.isLeft).map[Either[String, Map[String, String]]](v => Left(v.left.get)).getOrElse(Right(result.map(_.right.get).toMap))
  }

  override def main(args: Array[String]) {
    parseOptions(args) match {
      case Left(error) => printUsage(error)
      case Right(optionMap) => {
        val server = prepareJetty(optionMap.get("jdbc-url"), optionMap.getOrElse("port", "80").toInt)
        server.start()
      }
    }
  }

  private def printUsage(error: String) {
    println(error)
    println("""usage: <sade-server-cmd> [options]
options:
    --port <port-number>
    --jdbc-url <jdbc-url>
    """)
  }

}
