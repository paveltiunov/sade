package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import org.sade.view.{AnalyzeResultImageView, AllPointIdView}
import org.slf4j.LoggerFactory
import org.squeryl.{Session, SessionFactory, PrimitiveTypeMode}
import javax.naming.InitialContext
import javax.sql.DataSource
import org.squeryl.adapters.H2Adapter
import liquibase.Liquibase
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.database.jvm.JdbcConnection

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def installSchema() {
    new Liquibase(
      "install-scripts/all.xml",
      new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader),
      new JdbcConnection(dataSourceConnection)
    ).update(null)
  }

  def dataSourceConnection = {
    InitialContext.doLookup[DataSource]("SadeDS").getConnection
  }

  def setupSqueryl() {
    installSchema()
    SessionFactory.concreteFactory = Some(() => {
      Session.create(dataSourceConnection, new H2Adapter)
    })
  }

  def boot() {
    setupSqueryl()
    // where to search snippet
    LiftRules.addToPackages("org.sade")

    // Build SiteMap
    LiftRules.setSiteMap(
      SiteMap(
        Menu(
          Loc("Home", List("index"), "Home")
        ),
        Menu("Analyze results") / "analyze_result" / ** >> Hidden,
        Menu("Experiments") / "experiments"
      )
    )
    LiftRules.liftRequest.prepend {
      case Req("upload-point" :: Nil, _, _) => false
    }

    LiftRules.stdRequestTimeout = Box(300)

    LiftRules.dispatch.prepend {
      case Req("loaded-point-ids" :: Nil, _, _) => new AllPointIdView().dispatch _
    }
    LiftRules.dispatch.prepend(new AnalyzeResultImageView().dispatch)
  }
}

