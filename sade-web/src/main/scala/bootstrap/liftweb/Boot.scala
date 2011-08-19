package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import org.sade.view.{AnalyzeResultImageView, AllPointIdView}
import org.sade.model.SadeDB
import org.slf4j.LoggerFactory
import org.squeryl.{Session, SessionFactory, PrimitiveTypeMode}
import javax.naming.InitialContext
import javax.sql.DataSource
import org.squeryl.adapters.H2Adapter

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot extends PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def setupSqueryl() {
    SessionFactory.concreteFactory = Some(() => {
      Session.create(InitialContext.doLookup[DataSource]("SadeDS").getConnection, new H2Adapter)
    })
    inTransaction {
      try {
        SadeDB.create
      } catch {
        case e: Exception => logger.error("Errors during schema install", e)
      }
    }
  }

  def boot {
    setupSqueryl()
    // where to search snippet
    LiftRules.addToPackages("org.sade")

    // Build SiteMap
    val entries = Menu(
      Loc("Home", List("index"), "Home")
    ) :: Menu(Loc("Analyze results", List("analyze-result"), "Analyze results")) :: Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))
    LiftRules.liftRequest.prepend {
      case Req("upload-point" :: Nil, _, _) => false
    }

    LiftRules.dispatch.prepend {
      case Req("loaded-point-ids" :: Nil, _, _) => new AllPointIdView().dispatch _
    }
    LiftRules.dispatch.prepend(new AnalyzeResultImageView().dispatch)
  }
}

