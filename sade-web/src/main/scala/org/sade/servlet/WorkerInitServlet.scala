package org.sade.servlet

import javax.servlet.http.HttpServlet
import org.sade.worker.{RegisterHost, StartAnalyze, MainWorker}
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.RootActorPath
import java.net.InetAddress
import akka.util.duration._


class WorkerInitServlet extends HttpServlet {
  override def init() {
    SadeActors.mainWorker //TODO
    if (!WorkerInitServlet.disableWorker) {
      SadeActors.system.scheduler.schedule(0 minutes, 1 minutes, SadeActors.mainWorker, RegisterHost(InetAddress.getLocalHost.getHostAddress, WorkerInitServlet.akkaPort))
    }
  }
}

object WorkerInitServlet {
  var disableWorker: Boolean = System.getProperty("disableWorker", "false").toBoolean

  var akkaPort: Int = System.getProperty("akka.port", "2552").toInt

  var akkaMaster: Option[String] = Option(System.getProperty("akka.master"))
}

object SadeActors {
  val config = ConfigFactory.parseString(
    """
      |akka {
      |  actor {
      |    provider = "akka.remote.RemoteActorRefProvider"
      |  }
      |  remote {
      |    transport = "akka.remote.netty.NettyRemoteTransport"
      |    netty {
      |      hostname = "%s"
      |      port = %s
      |      message-frame-size = 10 MiB
      |    }
      |  }
      |}
    """.stripMargin.format(InetAddress.getLocalHost.getHostAddress, WorkerInitServlet.akkaPort))
  val system = ActorSystem("sade", config)
  val mainWorker = {
    WorkerInitServlet.akkaMaster.map(master =>
      system.actorFor(RootActorPath(AddressFromURIString(master)) / "user" / "mainWorker")
    ).getOrElse(system.actorOf(Props[MainWorker], "mainWorker"))
  }
}