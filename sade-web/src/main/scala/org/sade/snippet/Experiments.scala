package org.sade.snippet

import net.liftweb.util.BindHelpers
import org.sade.model.SadeDB


class Experiments extends BindHelpers {
  def render = ".experiment *" #> SadeDB.experiments.map (name => {
    ".link *" #> name &
    ".link [href]" #> ("/analyze_result/" + name) &
    ".uploaded *" #> ("Total points: " + SadeDB.pointCount(name).toString) &
    ".processed *" #> ("Processed points: " + SadeDB.analyzeResultCount(name).toString)
  })
}