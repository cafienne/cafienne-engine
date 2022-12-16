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

package org.cafienne.infrastructure.config

import org.cafienne.infrastructure.config.util.ChildConfigReader
import org.cafienne.storage.archive.Storage
import org.cafienne.storage.archive.file.FileBasedStorage

import java.io.File

class StorageConfig (val parent: CafienneConfig) extends ChildConfigReader {
  val path = "archive"

  val enabled: Boolean = readBoolean("enabled", default = false)

  val archive: Storage = {
    val plugin = readString("plugin", "file")
    plugin match {
      case "file" =>
        new FileBasedStorage(new FileStorageConfig(this))
      case "db" => fail("Database plugin is not yet available")
      case other => fail(s"Plugin of type $other is not supported")
    }
  }
}

class FileStorageConfig(val parent: StorageConfig) extends ChildConfigReader {
  val path = "file"

  val directory: File = {
    val string = readString("directory", "./archive")
    val file = new File(string)
    if (! file.exists()) {
      logger.warn("Creating archive directory " + file)
      file.mkdirs()
    }
    if (file.isFile) {
      val message = "========= Cannot use a file as archive. Change the below config setting to point to a directory  ========="
      val setting: String = s"""           directory = "$string" """
      val lengthOfLongestLine = Math.max(message.length, setting.length)
      val manyHashes = List.fill(lengthOfLongestLine)('=').mkString

      fail(s"\n$message\n\n$setting\n\n$manyHashes\n")
    }
    file
  }
}
