package org.cafienne.service.akkahttp

object ApiValidator {

  def required(identifier: String, errorMsg: String): Unit = {
    if (identifier == null || identifier.isBlank) {
      throw new IllegalArgumentException(errorMsg)
    }
  }

  def runDuplicatesDetector(groupType: String, memberType: String, identifiers: Seq[String]): Unit = {
    val duplicates = identifiers.diff(identifiers.distinct)
    if (duplicates.nonEmpty) {
      throw new IllegalArgumentException(s"$groupType contains duplicate $memberType entries: " + duplicates.mkString("['", "' '", "']"))
    }
  }
}
