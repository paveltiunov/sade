package org.sade.servlet

import javax.servlet.http.HttpServlet
import org.sade.worker.{RegisterHost, StartAnalyze, MainWorker}
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.RootActorPath
import java.net.InetAddress


class WorkerInitServlet extends HttpServlet {
  override def init() {
    SadeActors.mainWorker //TODO
    if (!WorkerInitServlet.disableWorker) {
      SadeActors.mainWorker !
        RegisterHost(InetAddress.getLocalHost.getHostAddress, WorkerInitServlet.akkaPort)
    }
  }
}

object WorkerInitServlet {
  var disableWorker: Boolean = System.getProperty("disableWorker", "false").toBoolean

  var akkaPort: Int = System.getProperty("akka.port", "2552").toInt
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
      |    }
      |  }
      |}
    """.stripMargin.format(InetAddress.getLocalHost.getHostAddress, WorkerInitServlet.akkaPort))
  val system = ActorSystem("sade", config)
  val mainWorker = {
    if (WorkerInitServlet.disableWorker) {
      system.actorOf(Props[MainWorker], "mainWorker")
    } else {
      system.actorFor(RootActorPath(AddressFromURIString(System.getProperty("akka.master", "akka://sade@%s:2552".format(InetAddress.getLocalHost.getHostAddress)))) / "user" / "mainWorker")
    }
  }
}