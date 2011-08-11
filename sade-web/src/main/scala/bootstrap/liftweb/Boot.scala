package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import org.sade.view.AllPointIdView

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("org.sade")

    // Build SiteMap
    val entries = Menu(Loc("Home", List("index"), "Home")) :: Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))
    LiftRules.liftRequest.prepend {
      case Req("upload-point" :: Nil, _, _) => false
    }

    LiftRules.onBeginServicing

    LiftRules.dispatch.prepend {
      case Req("loaded-point-ids" :: Nil, _, _) => new AllPointIdView().dispatch _
    }
  }
}

