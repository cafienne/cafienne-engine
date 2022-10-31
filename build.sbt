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
dockerBaseImage := "eclipse-temurin:17.0.3_7-jre-jammy"
dockerExposedPorts := Seq(2027, 9999)
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
val akkaHttpVersion    = "10.2.10"
val akkaVersion        = "2.6.20"
val jacksonVersion     = "2.13.4"
val enumeratumVersion  = "1.7.0"
val swaggerVersion     = "2.2.4"
val slickVersion       = "3.3.3"
val jasperVersion      = "6.20.0"

/**
  * Add runtime dependencies
  */
libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"                           % akkaVersion
  , "com.typesafe.akka"       %% "akka-cluster-tools"                   % akkaVersion
  , "com.typesafe.akka"       %% "akka-cluster-sharding"                % akkaVersion
  , "com.typesafe.akka"       %% "akka-http"                            % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-core"                       % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-xml"                        % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-http-jackson"                    % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-persistence"                     % akkaVersion
  , "com.typesafe.akka"       %% "akka-persistence-query"               % akkaVersion
  , "com.lightbend.akka"      %% "akka-persistence-jdbc"                % "5.1.0"
  , "com.typesafe.akka"       %% "akka-persistence-cassandra"           % "1.0.6"
  , "com.datastax.oss"        %  "java-driver-core"                     % "4.15.0"
  , "com.datastax.oss"        %  "java-driver-query-builder"            % "4.15.0"

  // Logging
  , "com.typesafe.akka"       %% "akka-slf4j"                           % akkaVersion
  , "com.typesafe.akka"       %% "akka-stream"                          % akkaVersion
  , "com.typesafe"            %  "config"                               % "1.4.2"
  , "com.typesafe.scala-logging"      %% "scala-logging"                % "3.9.5"
  , "ch.qos.logback"          %  "logback-classic"                      % "1.4.4"
  , "org.apache.commons"      %  "commons-text"                         % "1.10.0" // StrSubstitutor usage inside process tasks
  , "com.beachape"            %% "enumeratum"                           % enumeratumVersion
  , "jakarta.xml.bind"        %  "jakarta.xml.bind-api"                 % "4.0.0" // Used in StringValue xsd date conversions
  , "ch.megard"               %% "akka-http-cors"                       % "1.1.3"

  // JWT Support
  , "com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
  , "com.github.j5ik2o"       %% "sw4jj"                                % "1.1.60" // Simple scala Wrapper For Java-Jwt
  , "com.nimbusds"            %  "nimbus-jose-jwt"                      % "9.25.6"
  , "com.nimbusds"            %  "oauth2-oidc-sdk"                      % "10.0"

  // DB Schema
  , "org.flywaydb"            %  "flyway-core"                           % "7.2.1"
  , "com.typesafe.slick"      %% "slick-hikaricp"                       % slickVersion
  , "com.typesafe.slick"      %% "slick"                                % slickVersion
  , "io.github.nafg"          %% "slick-migration-api-flyway"           % "0.7.0"
  , "io.github.nafg"          %% "slick-migration-api"                  % "0.8.0"

  , "com.fasterxml.jackson.core"   % "jackson-databind"			            % jacksonVersion
  , "com.fasterxml.jackson.core"   % "jackson-core"					            % jacksonVersion
  , "com.fasterxml.jackson.module" %% "jackson-module-scala"            % jacksonVersion

  // PDF Task support
  , "net.sf.jasperreports"    % "jasperreports"                         % jasperVersion
  , "net.sf.jasperreports"    % "jasperreports-fonts"                   % jasperVersion
  // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7 because that is what Jasper needs.
  , "com.lowagie"             % "itext"                                 % "2.1.7" // DO NOT CHANGE THIS VALUE

  // Mail & Calendar support
  , "com.sun.activation"      % "jakarta.activation"                    % "2.0.1" // For mail & calendar support
  , "jakarta.activation"      % "jakarta.activation-api"                % "2.1.0" // For mail & calendar support
  , "jakarta.ws.rs"           % "jakarta.ws.rs-api"                     % "3.1.0" // For mail & calendar support
  , "org.mnode.ical4j"        % "ical4j"                                % "3.2.3"

  // Expression support (SPEL and JSONPath)
  , "com.jayway.jsonpath"  	  % "json-path"                             % "2.6.0" // 2.7.0 is not compatible in expressions
  , "org.springframework"     %  "spring-expression"                    % "5.3.23"

  // Persistence support
  , "com.h2database"          % "h2"                                    % "2.1.214"
  , "org.hsqldb"              % "hsqldb"                                % "2.7.1"
  , "com.github.dnvriend"     %% "akka-persistence-inmemory"            % "2.5.15.2"  excludeAll ExclusionRule(organization = "com.typesafe.akka")
  , "org.postgresql"          % "postgresql"                            % "42.5.0"
  , "com.microsoft.sqlserver" % "mssql-jdbc"                            % "9.2.1.jre11"

  // Swagger support
  , "io.swagger.core.v3"      % "swagger-core"                          % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-annotations"                   % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-jaxrs2"                        % swaggerVersion
  , "io.swagger.core.v3"      % "swagger-models"                        % swaggerVersion
  , "com.github.swagger-akka-http" %% "swagger-akka-http"               % "2.5.2"
  , "javax.ws.rs"             % "javax.ws.rs-api"                       % "2.1.1" // Note: this one is still needed for swagger-akka-http :(
  , "javax.xml.bind"          % "jaxb-api"                              % "2.3.1" // Note: this one is still needed for swagger-akka-http :(
)

/**
  * Add test dependencies
  */
libraryDependencies ++= Seq(
  "org.junit.jupiter"         % "junit-jupiter-api"                     % "5.9.0"
  , "com.novocode"            % "junit-interface"                       % "0.11"
  , "org.scalamock"           %% "scalamock"                            % "5.2.0"
  , "org.scalatest"           %% "scalatest"                            % "3.2.14"
  , "commons-io"              %  "commons-io"                           % "20030203.000550"
  , "com.typesafe.akka"       %% "akka-testkit"                         % akkaVersion
  , "com.typesafe.akka"       %% "akka-http-testkit"                    % akkaHttpVersion
  , "com.typesafe.akka"       %% "akka-multi-node-testkit"              % akkaVersion
  , "com.github.dnvriend"     %% "akka-persistence-inmemory"            % "2.5.15.2"  excludeAll ExclusionRule(organization = "com.typesafe.akka")
  , "com.github.tomakehurst"  % "wiremock"                              % "2.27.2"
).map(dep => dep % Test)
