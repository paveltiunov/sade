package org.sade.servlet

import javax.servlet.http.HttpServlet
import org.sade.worker.{RegisterHost, StartAnalyze, MainWorker}
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.RootActorPath
import java.net.InetAddress


class WorkerInitServlet extends HttpServlet {
  override def init() {
    val config = ConfigFactory.parseString(
      """
        |akka {
        |  actor {
        |    provider = "akka.remote.RemoteActorRefProvider"
        |  }
        |  remote {
        |    transport = "akka.remote.netty.NettyRemoteTransport"
        |    netty {
        |      hostname = ""
        |      port = %s
        |    }
        | }
        |}
      """.stripMargin.format(System.getProperty("akka.port", "2552")))
    val system = ActorSystem("sade", config)
    if (WorkerInitServlet.disableWorker) {
      system.actorOf(Props[MainWorker], "mainWorker") ! StartAnalyze
    } else {
      system.actorFor(RootActorPath(AddressFromURIString(System.getProperty("akka.master", "akka://sade@127.0.0.1:2552"))) / "mainWorker") ! RegisterHost(InetAddress.getLocalHost.getHostAddress)
    }
  }
}

object WorkerInitServlet {
  var disableWorker: Boolean = System.getProperty("disableWorker", "false").toBoolean
}