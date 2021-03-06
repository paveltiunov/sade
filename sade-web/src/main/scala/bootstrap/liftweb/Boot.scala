package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import org.sade.view.{SourcePointDownloadView, AnalyzeResultImageView, AllPointIdView}
import org.slf4j.LoggerFactory
import org.squeryl.{Session, SessionFactory, PrimitiveTypeMode}
import javax.naming.InitialContext
import javax.sql.DataSource
import org.squeryl.adapters.H2Adapter
import liquibase.Liquibase
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.database.jvm.JdbcConnection
import liquibase.lockservice.{DatabaseChangeLogLock, LockService}
import java.util.Date

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends PrimitiveTypeMode {
  val logger = LoggerFactory.getLogger(getClass)

  def installSchema() {
    val liquibase: Liquibase = new Liquibase(
      "install-scripts/all.xml",
      new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader),
      new JdbcConnection(dataSourceConnection)
    )
    val locks = LockService.getInstance(liquibase.getDatabase).listLocks()

    if (locks.forall(l => new Date().getTime - l.getLockGranted.getTime > 60 * 5 * 1000))
      liquibase.forceReleaseLocks()
    liquibase.update(null)
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

    LiftRules.htmlProperties.default.set((r: Req) => new XHtmlInHtml5OutProperties(r.userAgent))
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
      case Req("point-source" :: pointId :: channelId :: Nil, _, _) => () => new SourcePointDownloadView().dispatch(pointId.toLong, channelId)
    }
    LiftRules.dispatch.prepend(new AnalyzeResultImageView().dispatch)
  }
}

