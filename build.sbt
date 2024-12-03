/**
  * Global settings
  */
val basicSettings = {
  val scala213 = "2.13.15"
  val supportedScalaVersions = List(scala213)

  Seq(
    name := "Case Engine",
    organization := "com.casefabric",
    organizationName := "Batav B.V.",
    startYear := Some(2014),

    /**
      * Resolver repositories
      */
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.DefaultMavenRepository,
      "typesafe releases" at "https://repo.typesafe.com/typesafe/releases",
    ) ++ Resolver.sonatypeOssRepos("releases"),


    /**
      * Compiler settings
      */
    scalaVersion := scala213,
    Compile / doc / sources := List(),
    //javacOptions ++= Seq("-source", "11", "-target", "11")
    scalacOptions := Seq(
      "-encoding", "UTF-8",
      "-unchecked",
      //"-release:11",
      "-deprecation",
      "-Xlint", "deprecation",
      "-Xlint", "unchecked",
      "-Xlog-reflective-calls"
    ),

    Test / parallelExecution := false,
    Test / fork := true,
    // Do not publish any test artifacts
    Test / publishArtifact := false,

    /**
      * Publishing information for Sonatype and Maven
      */
    homepage := Some(url("https://casefabric.com")),
    scmInfo := Some(ScmInfo(url("https://github.com/casefabric/case-engine.git"), "git@github.com:casefabric/case-engine.git")),
    licenses += ("AGPL-3.0", url("https://www.gnu.org/licenses/agpl-3.0.txt")),
    developers := List(Developer(
      "tpetter",
      "Thijs Petter",
      "tpetter@cafienne.io",
      url("https://github.com/tpetter")),
      Developer("olger",
        "Olger Warnier",
        "olger@spectare.nl",
        url("https://github.com/olger"))
    ),
    versionScheme := Some("semver-spec"),
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },

    // add sonatype repository settings
    // snapshot versions publish to sonatype snapshot repository
    // other versions publish to sonatype staging repository
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeOssSnapshots.head
      else
        Opts.resolver.sonatypeStaging
    ),

    /**
      * Version generation, depends on the [[GitInfoAnalyzer]]
      */
    git.useGitDescribe := true, // Not sure if this is required, it seems to also work when it is not set
    git.gitTagToVersionNumber := { tag =>
      // This code sets the gitDescribedVersion from the current git tag.
      //  We do not actually return a version, but the version + the description.
      //  Next task is to pull them apart from each other again.
      //  This is done because somehow it is not possible to keep state inside the GitInfoAnalyzer object :(
      GitInfoAnalyzer.getVersionPlusDescription(tag, git.gitCurrentBranch.value, git.gitHeadCommit.value, git.gitUncommittedChanges.value)
    },
    // Now convert the generated gitDescribedVersion into a proper 'version' and 'description'
    version := GitInfoAnalyzer.version(version.value, git.gitDescribedVersion.value),
    description := GitInfoAnalyzer.description(description.value, git.gitDescribedVersion.value),

    // Generate 'name', 'organization', 'version', 'description', 'gitHeadCommit', 'gitCurrentBranch', 'gitUncommittedChanges'
    sbtbuildinfo.BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](name, organization, version, description, git.gitHeadCommit, git.gitCurrentBranch, git.gitUncommittedChanges),
    sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.BuildTime,
    sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.ToMap,
    sbtbuildinfo.BuildInfoKeys.buildInfoPackage := "com.casefabric",
    sbtbuildinfo.BuildInfoKeys.buildInfoObject := "BuildInfo"
  )
}

lazy val engineRoot = (project in file("."))
  .settings(basicSettings: _*)
  .settings(publishArtifact := false,
            publish / skip := true,
            crossScalaVersions := Nil)
  .enablePlugins(BuildInfoPlugin, GitPlugin, GitVersioning, GitBranchPrompt)
   //AutomateHeaderPlugin
  //.settings(releaseSettings)
  .aggregate(engine, plugins, service)

val engine = (project in file("engine"))
   //AutomateHeaderPlugin
  .enablePlugins(BuildInfoPlugin, GitPlugin, GitVersioning, GitBranchPrompt)
  .settings(basicSettings: _*)
  .settings(
    name := "case-engine",
    libraryDependencies ++= Dependencies.baseEngineDeps ++ Dependencies.engineTestDeps)

val plugins = (project in file("plugins"))
  .dependsOn(engine)
  .enablePlugins(BuildInfoPlugin, GitPlugin, GitVersioning, GitBranchPrompt)
  .settings(basicSettings: _*)
  .settings(
    name := "case-plugins",
    publishArtifact := true,
    publish / skip := true,
    libraryDependencies ++= Dependencies.pluginDeps ++ Dependencies.baseEngineDeps ++ Dependencies.engineTestDeps)

val service = (project in file("service"))
  .dependsOn(engine, plugins)
   //AutomateHeaderPlugin
  .enablePlugins(BuildInfoPlugin, GitPlugin, GitVersioning, GitBranchPrompt, JavaAppPackaging, AshScriptPlugin, DockerPlugin, ClasspathJarPlugin)
  .settings(basicSettings: _*)
  .settings(
    name := "case-service",
    libraryDependencies ++= Dependencies.pluginDeps ++ Dependencies.serviceDeps ++ Dependencies.serviceTestDeps,
    Compile / mainClass := Some("com.casefabric.service.Main"),
    // Package bin is required in case we ship a jar file with a manifest only. Think that's not happening at this moment.
    packageBin / mainClass.withRank(KeyRanks.Invisible) := Some("com.casefabric.service.Main"),
    /**
      * Docker packaging
      */
    Docker / packageName := "casefabric/engine",
    Docker / version := "latest",
    Docker / maintainer := """CaseFabric <info@casefabric.com>""",
    Docker / defaultLinuxInstallLocation := "/opt/casefabric",
    dockerBaseImage := "eclipse-temurin:21.0.4_7-jre-jammy",
    dockerExposedPorts := Seq(2027, 9999),
    bashScriptDefines / scriptClasspath := Seq("../lib_ext/*") ++ (bashScriptDefines / scriptClasspath).value,
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=$${app_home}/../conf/logback.xml"""",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=$${app_home}/../conf/local.conf"""",
    // Do not publish to docker
    Docker / publish / skip := true
    )
