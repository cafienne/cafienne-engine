  lazy val basicSettings = {
    val currentScalaVersion = "2.12.7"
    val scala211Version     = "2.11.11"


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
        "-Xlog-reflective-calls"
      ),
      parallelExecution in Compile := true,
      parallelExecution in Test := false,
      fork in Test := true,
      sources in doc in Compile := List()
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
    .settings(basicSettings: _*)

  // CMMN CASE ENGINE
  lazy val engine = project("case-engine")
    .settings(libraryDependencies ++=
      Deps.compile(
        Deps.akkaActor,
        Deps.akkaStream,
        Deps.akkaPersistence,
        Deps.akkaQuery,
        Deps.akkaClusterTools,
        Deps.akkaQuery,
        Deps.akkaClusterSharding,
        Deps.akkaContrib,
        Deps.akkaSlf4j,
        Deps.slf4j,
        Deps.akkaInMemoryTestDB,
        Deps.logback,
        Deps.jsonJava,
        Deps.jacksonDatabind,
        Deps.scalaLogging,
        Deps.spel,
        Deps.apacheCommonsText,
        Deps.jsonPath,
        Deps.javaMail,
        Deps.jasperReports,
        Deps.jasperReportFonts,
        Deps.lowagie,
        Deps.enumeratum,
        Deps.slick,
        Deps.hikariCP,
        Deps.postgres,
        Deps.sqlserver,
        Deps.h2,
        Deps.hsqldb,
        Deps.slickMigration,
        Deps.flywaySlickBindings,
        Deps.akkaHttp,
        Deps.akkHttpXml,
        Deps.akkaHttpSprayJson,
        Deps.akkaHttpCors,
        Deps.javaxws,
        Deps.swaggerAkkaHttp, Deps.swaggerAkkaHttpFix, Deps.swaggerAkkaHttpFix2, Deps.swaggerAkkaHttpScala, Deps.swaggerCore, Deps.swaggerAnnotations, Deps.swaggerModels, Deps.swaggerjaxrs2

        
      ) ++
        Deps.test(
          Deps.junit,
          Deps.sbtJUnitInterface,
          Deps.akkaMultiNodeTestKit,
          Deps.akkaInMemoryTestDB,
          Deps.wireMock
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
      git.gitTagToVersionNumber := { tag: String =>
        val splittedTag = tag.split("-");
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
          Deps.akkaActor,
          Deps.akkaStream,
          Deps.akkaPersistence,
          Deps.levelDbFixed,
          Deps.levelDbJNI,
          Deps.cassandraPersistence,
          Deps.akkaPersistenceJDBC,
          Deps.akkaQuery,
          Deps.akkaClusterTools,
          Deps.akkaClusterSharding,
          Deps.akkaContrib,
          Deps.akkaSlf4j,
          Deps.slf4j,
          Deps.logback,
          Deps.scalaLogging,
          Deps.akkaHttp,
          Deps.akkHttpXml,
          Deps.akkaHttpSprayJson,
          Deps.akkaHttpCors,
          Deps.javaxws,
          Deps.swaggerAkkaHttp, Deps.swaggerAkkaHttpFix, Deps.swaggerAkkaHttpFix2, Deps.swaggerAkkaHttpScala, Deps.swaggerCore, Deps.swaggerAnnotations, Deps.swaggerModels, Deps.swaggerjaxrs2,
          Deps.akkaHttpCore,
          Deps.akkaHtppJackson,
          Deps.bcrypt,
          Deps.joseJwt,
          Deps.config,
          Deps.akkaTestKit,
          Deps.sw4jj,
          Deps.enumeratum,
          Deps.jacksonScala,
          Deps.slick,
          Deps.hikariCP,
          Deps.postgres,
          Deps.h2,
          Deps.flyway
      ) ++
        Deps.test (
            Deps.junit,
            Deps.akkaMultiNodeTestKit,
            Deps.wireMock,
            Deps.akkaHttpTestkit
        )
    )
    .settings(
      packageName in Docker := "cafienne/engine",
      version in Docker := "latest",
      maintainer in Docker := """Cafienne <info@cafienne.io>""",
      defaultLinuxInstallLocation in Docker := "/opt/cafienne",
      bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=$${app_home}/../conf/logback.xml"""",
      bashScriptExtraDefines += s"""addJava "-Dconfig.file=$${app_home}/../conf/local.conf"""",
      dockerExposedPorts := Seq(2027, 9999),
      dockerBaseImage := "cafienne/base:openjdk-11-buster",
      name in Universal := "cafienne",
      packageName in Universal := "cafienne"
    )
    .enablePlugins(JavaAppPackaging)
    .enablePlugins(UniversalPlugin)
    .enablePlugins(ClasspathJarPlugin)
    .enablePlugins(LauncherJarPlugin)
    .enablePlugins(DockerPlugin)
    .configs(MultiJvm)

  def project(name: String) = Project(name, file(name)).settings(moduleSettings: _*)

