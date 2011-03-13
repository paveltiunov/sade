package org.sade

import javax.ejb.{Stateless, Local}

@Local
trait Sade {
  def sayHello(name: String): String
}

@Stateless
class SadeBean extends Sade {
  def sayHello(name: String) = "Hello " + name
}