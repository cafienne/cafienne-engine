import com.typesafe.sbt.packager.docker._

/**
  * Global settings
  */
name := "Cafienne Engine"
packageName := "cafienne-engine"
organization := "org.cafienne"

/**
  * Resolver repositories
  */
resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.DefaultMavenRepository,
  Resolver.sonatypeRepo("releases"),
  "typesafe releases" at "https://repo.typesafe.com/typesafe/releases",
)

/**
  * Plugins used
  */
enablePlugins(BuildInfoPlugin)
enablePlugins(GitPlugin)
enablePlugins(GitVersioning, GitBranchPrompt)
enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)
enablePlugins(ClasspathJarPlugin)

/**
  * Docker packaging
  */
Docker / packageName := "cafienne/engine"
Docker / version := "latest"
Docker / maintainer := """Cafienne <info@cafienne.io>"""
Docker / defaultLinuxInstallLocation := "/opt/cafienne"
dockerExposedPorts := Seq(2027, 9999)
dockerBaseImage := "openjdk:17-slim"
//Adding dependencies required for the PDF generation Process Task
dockerCommands := dockerCommands.value.flatMap {
  case c@Cmd("USER", "root") => Seq(c, Cmd("RUN",  "apt-get update && apt-get -y install fontconfig libfreetype6 && apt-get clean"))
  case other => Seq(other)
}
bashScriptDefines / scriptClasspath := Seq("../lib_ext/*") ++ (bashScriptDefines / scriptClasspath).value
bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=$${app_home}/../conf/logback.xml""""
bashScriptExtraDefines += s"""addJava "-Dconfig.file=$${app_home}/../conf/local.conf""""
// Do not publish to docker
Docker / publish / skip := true

/**
  * Compiler settings
  */
scalaVersion := "2.13.8"
Compile / doc / sources := List()
Compile / mainClass := Some("org.cafienne.service.Main")
// Package bin is required in case we ship a jar file with a manifest only. Think that's not happening at this moment.
packageBin / mainClass.withRank(KeyRanks.Invisible) := Some("org.cafienne.service.Main")
javacOptions ++= Seq("-source", "11", "-target", "11")
scalacOptions := Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-target:jvm-11",
  "-deprecation",
  "-Xlint", "deprecation",
  "-Xlint", "unchecked",
  "-Xlog-reflective-calls"
)

Test / parallelExecution := false
Test / fork := true
// Do not publish any test artifacts
Test / publishArtifact := false

/**
  * Publishing information for Sonatype and Maven
  */
homepage := Some(url("https://cafienne.org"))
scmInfo := Some(ScmInfo(url("https://github.com/cafienne/cafienne-engine.git"), "git@github.com:cafienne/cafienne-engine.git"))
licenses += ("Apache-2.0", url("https://www.mozilla.org/en-US/MPL/2.0/"))
developers := List(Developer(
  "tpetter",
  "Thijs Petter",
  "tpetter@cafienne.io",
  url("https://github.com/tpetter")),
  Developer("olger",
    "Olger Warnier",
    "olger@spectare.nl",
    url("https://github.com/olger"))
)
versionScheme := Some("semver-spec")
publishMavenStyle := true
pomIncludeRepository := { _ => false }
// Use old method for signing the published files.
Global / useGpg := false

// add sonatype repository settings
// snapshot versions publish to sonatype snapshot repository
// other versions publish to sonatype staging repository
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

/**
  * Version generation, depends on the [[GitInfoAnalyzer]]
  */
git.useGitDescribe := true // Not sure if this is required, it seems to also work when it is not set
git.gitTagToVersionNumber := { tag =>
  // This code sets the gitDescribedVersion from the current git tag.
  //  We do not actually return a version, but the version + the description.
  //  Next task is to pull them apart from each other again.
  //  This is done because somehow it is not possible to keep state inside the GitInfoAnalyzer object :(
  GitInfoAnalyzer.getVersionPlusDescription(tag, git.gitCurrentBranch.value, git.gitHeadCommit.value, git.gitUncommittedChanges.value)
}
// Now convert the generated gitDescribedVersion into a proper 'version' and 'description'
version := GitInfoAnalyzer.version(version.value, git.gitDescribedVersion.value)
description := GitInfoAnalyzer.description(description.value, git.gitDescribedVersion.value)

// Generate 'name', 'organization', 'version', 'description', 'gitHeadCommit', 'gitCurrentBranch', 'gitUncommittedChanges'
sbtbuildinfo.BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](name, organization, version, description, git.gitHeadCommit, git.gitCurrentBranch, git.gitUncommittedChanges)
sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.BuildTime
sbtbuildinfo.BuildInfoKeys.buildInfoOptions += BuildInfoOption.ToMap
sbtbuildinfo.BuildInfoKeys.buildInfoPackage := "org.cafienne"
sbtbuildinfo.BuildInfoKeys.buildInfoObject := "BuildInfo"

configs(MultiJvm) // Not sure what this adds, actually

/**
  * Dependencies
  */
val akkaHttpVersion    = "10.2.7"
val akkaVersion        = "2.6.18"
val jacksonVersion     = "2.13.0"
val enumeratumVersion  = "1.7.0"
val swaggerVersion     = "2.1.11"
val slickVersion       = "3.3.3"
val jasperVersion      = "6.18.1"

/**
  * Add runtime dependencies
  */
libraryDependencies ++= Seq(
  "com.typesafe.akka"       %% "akka-actor"                           % akkaVersion
  , "com.typesafe.akka"       %% "akka-cluster-tools"                   % akkaVersion
  , "com.typesafe.akka"       %% "akka-cluster-sharding"                % akkaVersion
  , "com.typesafe.akka"       %% "akka-http"                            % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-core"                       % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-xml"                        % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-jackson"                    % akkaHttpVersion
  , "com.lightbend.akka"      %% "akka-persistence-jdbc"                % "5.0.4"
  , "com.typesafe.akka"       %% "akka-persistence"                     % akkaVersion
  , "com.typesafe.akka"       %% "akka-persistence-query"               % akkaVersion
  , "com.typesafe.akka"       %% "akka-slf4j"                           % akkaVersion
  , "com.typesafe"            %  "config"                               % "1.4.1"
  , "com.typesafe.scala-logging"      %% "scala-logging"                % "3.9.4"
  , "com.typesafe.akka"       %% "akka-stream"  % akkaVersion
  , "com.typesafe.akka"		    %% "akka-persistence-cassandra" 		      % "0.107"
  , "com.datastax.cassandra"     % "cassandra-driver-extras"            % "3.11.0" // Needed for timestamp conversion
  , "org.apache.commons"      % "commons-text"                          % "1.9"
  , "com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
  , "com.beachape"            %% "enumeratum"                           % enumeratumVersion
  , "javax.xml.bind"          % "jaxb-api"                              % "2.3.1" // Used in StringValue xsd date conversions
  , "ch.megard"               %% "akka-http-cors"                       % "1.1.2"

  , "org.flywaydb"            % "flyway-core"                           % "7.2.1"
  , "com.typesafe.slick"      %% "slick-hikaricp"                       % slickVersion
  , "com.typesafe.slick"      %% "slick"                                % slickVersion
  , "io.github.nafg"          %% "slick-migration-api-flyway"           % "0.7.0"
  , "io.github.nafg"          %% "slick-migration-api"                  % "0.8.0"

  , "com.fasterxml.jackson.core"   % "jackson-databind"			            % jacksonVersion
  , "com.fasterxml.jackson.core"   % "jackson-core"					            % jacksonVersion
  , "com.fasterxml.jackson.module" %% "jackson-module-scala"            % jacksonVersion

  , "net.sf.jasperreports"    % "jasperreports"                         % jasperVersion
  , "net.sf.jasperreports"    % "jasperreports-fonts"                   % jasperVersion
  // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7 because that is what Jasper needs.
  , "com.lowagie"             % "itext"                                 % "2.1.7" // DO NOT CHANGE THIS VALUE

  , "com.sun.mail"            % "javax.mail"                            % "1.6.2"
  , "com.nimbusds"            % "nimbus-jose-jwt"                       % "9.15.2"
  , "org.mnode.ical4j"        % "ical4j"                                % "3.1.1"

  // As suggested in https://stackoverflow.com/questions/43574426/how-to-resolve-java-lang-noclassdeffounderror-javax-xml-bind-jaxbexception-in-j
  // to resolve blow-up due to swagger :  java.lang.NoClassDefFoundError: javax/xml/bind/annotation/XmlRootElement.
  , "javax.ws.rs"             % "javax.ws.rs-api"                       % "2.1.1"
  , "com.jayway.jsonpath"  	  % "json-path"                             % "2.6.0"
  , "com.h2database"          % "h2"                                    % "2.0.202"
  , "org.hsqldb"              % "hsqldb"                                % "2.5.1"
  , "com.github.dnvriend"     %% "akka-persistence-inmemory"            % "2.5.15.2"  excludeAll ExclusionRule(organization = "com.typesafe.akka")
  , "ch.qos.logback"          %  "logback-classic"                      % "1.2.10"
  , "org.postgresql"          % "postgresql"                            % "42.3.1"
  , "org.springframework"     %  "spring-expression"                    % "5.3.14"
  , "com.microsoft.sqlserver" % "mssql-jdbc"                            % "9.2.1.jre11"
  , "io.swagger.core.v3"      % "swagger-core"                          % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-annotations"                   % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-jaxrs2"                        % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-models"                        % swaggerVersion
  , "com.github.swagger-akka-http" %% "swagger-akka-http"               % "2.5.2"
  , "com.github.j5ik2o"       %% "sw4jj"                                % "1.1.59"
)

/**
  * Add test dependencies
  */
libraryDependencies ++= Seq(
  "org.junit.jupiter"       % "junit-jupiter-api"                     % "5.8.2"
  , "com.novocode"            % "junit-interface"                     % "0.11"
  , "org.scalamock"           %% "scalamock"                          % "5.2.0"
  , "org.scalatest"           %% "scalatest"                          % "3.2.9"
  , "commons-io"              %  "commons-io"                         % "20030203.000550"
  , "com.typesafe.akka"       %% "akka-testkit"                       % akkaVersion
  , "com.typesafe.akka"       %% "akka-http-testkit"                  % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-multi-node-testkit"            % akkaVersion
  , "com.github.dnvriend"     %% "akka-persistence-inmemory"          % "2.5.15.2"  excludeAll ExclusionRule(organization = "com.typesafe.akka")
  , "com.github.tomakehurst"  % "wiremock"                            % "2.27.2"
).map(dep => dep % Test)
