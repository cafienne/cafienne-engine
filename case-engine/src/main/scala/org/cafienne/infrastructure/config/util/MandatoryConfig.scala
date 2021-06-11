package org.cafienne.infrastructure.config.util

trait MandatoryConfig extends ChildConfigReader {
  val msg: String = s"Missing configuration property cafienne.$fullPath"

  override lazy val config = {
    if (parent.config.hasPath(path)) {
      parent.config.getConfig(path)
    } else {
      fail(msg)
    }
  }
}
