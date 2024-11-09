/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.enginedeveloper

/**
  * Special logger for engine developers to print indented logging for SentryNetwork executions
  */
object EngineDeveloperConsole {
  val enabled = false // Potentially make this a config setting, but better not. Note, if enabling probably switch off logback.xml logging
  private var currentIndent = 0

  def debugIndentedConsoleLogging(any: Any): Unit = {
    if (enabled) {
      any match {
        case throwable: Throwable => // Print exception with newline before and after it. Will not be indented
          println("\n")
          throwable.printStackTrace(System.out)
          println("\n")
        case _ =>
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
