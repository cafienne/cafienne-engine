package org.cafienne.infrastructure

import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.config.CafienneConfig

/**
  * JVM wide configurations and settings
  */
object Cafienne {

  /**
    * Configuration settings of this Cafienne Platform
    */
  lazy val config = new CafienneConfig

  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  lazy val version = new CafienneVersion

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    config.platform.isPlatformOwner(userId)
  }
}
