import sbtbuildinfo._

object GitInfoAnalyzer {

  case class Version(baseVersion: Seq[Int], isSnapshot: Boolean) {
    val major: Int = baseVersion.head
    val minor: Int = baseVersion(1)
    val patch: Int = baseVersion(2)

    override val toString: String = {
      if (isSnapshot) {
        s"$major.$minor.${patch + 1}-SNAPSHOT"
      } else {
        s"$major.$minor.$patch"
      }
    }

    val next: String = {
      if (isSnapshot) {
        s"$major.$minor.${patch + 1}"
      } else {
        s"$major.$minor.$patch"
      }
    }
  }

  private var analyzedDescription: String  = ""
  private var baseline: String = ""
  private var branch: String = ""
  private var gitCommit: String = ""
  private var additionalCommits: Int = 0
  private var isSnapshot: Boolean = true
  private var version: Version = null

  /**
    * Returns the next version number, i.e. patch level increased with 1,
    * or baseline patch version if nothing has changed since the baseline tag
    * @return
    */
  def nextVersion: String = version.next

  /**
    * Return BuildInfoKey with the description
    * @return
    */
  def description: BuildInfoKey = BuildInfoKey("description" -> analyzedDescription)

  /**
    * Analyze the given information on git source code status
    */
  def load(currentGitTag: String, gitBranch: String, gitHeadCommit: Option[String], localChanges: Boolean): Unit = {
    val dashedVersions = currentGitTag.split("-")

    GitInfoAnalyzer.baseline = dashedVersions(0)
    GitInfoAnalyzer.branch = gitBranch
    GitInfoAnalyzer.gitCommit = gitHeadCommit.getOrElse("")
    GitInfoAnalyzer.additionalCommits = {
      if (dashedVersions.length > 1) dashedVersions(1).toInt
      else 0
    }
    GitInfoAnalyzer.isSnapshot = additionalCommits > 0 || localChanges;

    println(s"--- Analyzing Git Information")
    println(s" - git tag given:         $currentGitTag")
    println(s" - base version:          $baseline")
    println(s" - branch:                $gitBranch")
    println(s" - commits in branch:     $additionalCommits")
    println(s" - current commit:        ${gitHeadCommit.getOrElse("no commit has been done")}")
    println(s" - found local changes:   $localChanges")
    println(s" - is snapshot:           $isSnapshot\n")

    GitInfoAnalyzer.version = Version(dashedVersions(0).split("[.]").map(_.toInt), isSnapshot)

    GitInfoAnalyzer.analyzedDescription = {
      if (additionalCommits > 0) {
        if (localChanges) {
          s"This engine is built on commit [$gitCommit] in branch [$gitBranch] having $additionalCommits additional commits on top of version [$baseline]. The build also contains some local changes that have not been committed."
        } else {
          s"This engine is built on commit [$gitCommit] in branch [$gitBranch] having $additionalCommits additional commits on top of version [$baseline]."
        }
      } else {
        if (localChanges) {
          s"This engine is built on branch [$gitBranch] with tag [$baseline] and some local changes that have not been committed."
        } else {
          s"This engine is built on branch [$gitBranch] with tag [$baseline]."
        }
      }
    }

    println("--- Resulting Description and Version")
    println(" -  description: " + GitInfoAnalyzer.analyzedDescription)
    println(" -  version:     " + GitInfoAnalyzer.version)
    println("")
  }
}
