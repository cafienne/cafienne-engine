package org.cafienne.akka.actor

/**
  * Special logger for engine developers to print indented logging for SentryNetwork executions
  */
object EngineDeveloperConsole {
  val enabled = false // Potentially make this a config setting, but better not. Note, if enabling probably switch off logback.xml logging
  private var currentIndent = 0

  def debugIndentedConsoleLogging(any: Any): Unit = {
    enabled match {
      case false => // Nothing to to log
      case true => {
        if (any.isInstanceOf[Throwable]) { // Print exception with newline before and after it. Will not be indented
          println("\n")
          any.asInstanceOf[Throwable].printStackTrace(System.out)
          println("\n")
        } else {
          var logMessage = String.valueOf(any) // String.valueOf converts JSON Value if required
          // Now get current level of indent from current behavior (recursive method)
          val indent = getIndent
          // Make sure if it is a message with newlines, the new lines also get indented
          logMessage = logMessage.replaceAll("\n", "\n" + indent)
          // Print to console.
          println(indent + logMessage)
        }
      }
    }
  }

  def getIndent: String = {
    val space: Char = ' '
    List.fill(currentIndent)(space).mkString
  }

  def indent(size: Int = 2): String = {
    currentIndent = currentIndent + size
    getIndent
  }

  def outdent(size: Int = 2): String = {
    currentIndent = currentIndent - size
    if (currentIndent < 0) {
      currentIndent = 0
    }
    getIndent
  }
}
