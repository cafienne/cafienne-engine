lazy val basicSettings = {

  Seq(
    organization := "org.cafienne",
    description := "Case Engine",
    scalaVersion := Deps.V.scala,
    resolvers ++= Deps.depsRepos,
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions := Seq(
      "-encoding", "UTF-8",
      "-unchecked",
      "-deprecation",
      "-Xlint","deprecation",
      "-Xlint","unchecked",
      "-Xlog-reflective-calls"
    ),
    Compile / parallelExecution := true,
    Compile / doc / sources := List(),
    Test / parallelExecution  := false,
    Test / fork := true,
    homepage := Some(url("https://cafienne.org")),
    scmInfo := Some(ScmInfo(url("https://github.com/cafienne/cafienne-engine.git"), "git@github.com:cafienne/cafienne-engine.git")),
    licenses += ("Apache-2.0", url("https://www.mozilla.org/en-US/MPL/2.0/")),
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
    publishMavenStyle := true,

    // disable publishw ith scala version, otherwise artifact name will include scala version
    // e.g cassper_2.11
    //crossPaths := false,
    // add sonatype repository settings
    // snapshot versions publish to sonatype snapshot repository
    // other versions publish to sonatype staging repository
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
  )
}

lazy val moduleSettings = basicSettings ++ Seq(
  libraryDependencies ++=
    Deps.compile(
      Deps.javaXmlJDK9Compat
    ) ++
      Deps.test(
        Deps.junit,
        Deps.sbtJUnitInterface,
        Deps.scalaMock,
        Deps.scalaTest,
        Deps.commonsIO
      )
)

// PROJECT ROOT
lazy val cafienne = Project("cafienne-engine", file(""))
  .aggregate(
    engine,
    service
  )
  .settings(
    publish / skip := true
  )
  .settings(basicSettings: _*)

// CMMN CASE ENGINE
lazy val engine = project("case-engine")
  .settings(libraryDependencies ++=
    Deps.compile(
      Deps.akkaActor,
      Deps.akkaClusterTools,
      Deps.akkaClusterSharding,
      Deps.akkaContrib,
      Deps.akkaHttp,
      Deps.akkaHttpCors,
      Deps.akkaHttpSprayJson,
      Deps.akkaHttpXml,
      Deps.akkaInMemoryTestDB,
      Deps.cassandraPersistence,
      Deps.akkaPersistenceJDBC,
      Deps.akkaPersistence,
      Deps.akkaQuery,
      Deps.akkaSlf4j,
      Deps.akkaStream,
      Deps.apacheCommonsText,
      Deps.cassandraPersistence,
      Deps.config,
      Deps.enumeratum,
      Deps.flyway,
      Deps.flywaySlickBindings,
      Deps.jacksonDatabind,
      Deps.jasperReports,
      Deps.jasperReportFonts,
      Deps.javaMail,
      Deps.ical4j,
      Deps.javaxws,
      Deps.jsonJava,
      Deps.jsonPath,
      Deps.h2,
      Deps.hikariCP,
      Deps.hsqldb,
      Deps.logback,
      Deps.lowagie,
      Deps.postgres,
      Deps.scalaLogging,
      Deps.slick,
      Deps.slickMigration,
      Deps.spel,
      Deps.sqlserver,
      Deps.swaggerAkkaHttp,
      Deps.swaggerAnnotations,
      Deps.swaggerCore,
      Deps.swaggerjaxrs2,
      Deps.swaggerModels,
    ) ++
      Deps.test(
        Deps.akkaMultiNodeTestKit,
        Deps.akkaInMemoryTestDB,
        Deps.junit,
        Deps.sbtJUnitInterface,
        Deps.wireMock,
      )
  )
  .settings(
    sbtbuildinfo.BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](description, organization, version, git.baseVersion, git.gitHeadCommit, git.gitCurrentBranch, git.gitUncommittedChanges),
    sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.BuildTime,
    sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.ToMap,
    sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.ToJson,
    sbtbuildinfo.BuildInfoKeys.buildInfoPackage := "org.cafienne.cmmn.akka",
    sbtbuildinfo.BuildInfoKeys.buildInfoObject := "BuildInfo"
  )
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitPlugin)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
    git.gitTagToVersionNumber := { tag: String =>
      val splittedTag = tag.split("-")
      splittedTag.length match {
        case 0 => Some(tag)
        case 1 => Some(tag)
        case _ => splittedTag(1) match {
          case "1" => Some(splittedTag(0) + "-with-1-additional-commit")
          case more => Some(splittedTag(0) + s"-with-$more-additional-commits")
        }
      }
    },
    git.useGitDescribe := true
  )
  .configs(MultiJvm)

lazy val service = project("case-service")
  .dependsOn(engine)
  .settings(libraryDependencies ++=
    Deps.compile(
      Deps.akkaHttpCore,
      Deps.akkaHtppJackson,
      Deps.bcrypt,
      Deps.hikariCP,
      Deps.jacksonScala,
      Deps.javaxws,
      Deps.joseJwt,
      Deps.sw4jj,
    ) ++
      Deps.test(
        Deps.akkaTestKit,
        Deps.akkaHttpTestkit,
        Deps.akkaMultiNodeTestKit,
        Deps.junit,
        Deps.wireMock,
      )
  )
  .settings(
    Docker / packageName := "cafienne/engine",
    Docker / version := "latest",
    Docker / maintainer := """Cafienne <info@cafienne.io>""",
    Docker / defaultLinuxInstallLocation := "/opt/cafienne",
    bashScriptDefines / scriptClasspath := Seq("../lib_ext/*","*"),
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=$${app_home}/../conf/logback.xml"""",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=$${app_home}/../conf/local.conf"""",
    dockerExposedPorts := Seq(2027, 9999),
    dockerBaseImage := "cafienne/base:openjdk-11-buster",
    Universal / name := "cafienne",
    Universal / packageName := "cafienne",
    publish / skip := true
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(UniversalPlugin)
  .enablePlugins(DockerPlugin)
  .configs(MultiJvm)

def project(name: String) = Project(name, file(name)).settings(moduleSettings: _*)

