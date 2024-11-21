/**
  * Global settings
  */
name := "Cafienne Engine"
packageName := "cafienne-engine"
organization := "org.cafienne"
organizationName := "Batav B.V."
startYear := Some(2014)

/**
  * Resolver repositories
  */
resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.DefaultMavenRepository,
  "typesafe releases" at "https://repo.typesafe.com/typesafe/releases",
) ++ Resolver.sonatypeOssRepos("releases")

/**
  * Plugins used
  */
enablePlugins(BuildInfoPlugin)
enablePlugins(GitPlugin)
enablePlugins(GitVersioning, GitBranchPrompt)
enablePlugins(JavaAppPackaging)
enablePlugins(AshScriptPlugin)
enablePlugins(DockerPlugin)
enablePlugins(ClasspathJarPlugin)

/**
  * Docker packaging
  */
Docker / packageName := "cafienne/engine"
Docker / version := "latest"
Docker / maintainer := """Cafienne <info@cafienne.io>"""
Docker / defaultLinuxInstallLocation := "/opt/cafienne"
dockerBaseImage := "eclipse-temurin:21.0.4_7-jre-jammy"
dockerExposedPorts := Seq(2027, 9999)
bashScriptDefines / scriptClasspath := Seq("../lib_ext/*") ++ (bashScriptDefines / scriptClasspath).value
bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=$${app_home}/../conf/logback.xml""""
bashScriptExtraDefines += s"""addJava "-Dconfig.file=$${app_home}/../conf/local.conf""""
// Do not publish to docker
Docker / publish / skip := true

/**
  * Compiler settings
  */
scalaVersion := "2.13.15"
Compile / doc / sources := List()
Compile / mainClass := Some("org.cafienne.service.Main")
// Package bin is required in case we ship a jar file with a manifest only. Think that's not happening at this moment.
packageBin / mainClass.withRank(KeyRanks.Invisible) := Some("org.cafienne.service.Main")
//javacOptions ++= Seq("-source", "11", "-target", "11")
scalacOptions := Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  //"-release:11",
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
licenses += ("AGPL-3.0", url("https://www.gnu.org/licenses/agpl-3.0.txt"))
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
    Opts.resolver.sonatypeOssSnapshots.head
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
val pekkoHttpVersion    = "1.1.0"
val pekkoVersion        = "1.1.2"
val pekkoPersistenceVersion = "1.1.2"
val jacksonVersion     = "2.17.2"
val enumeratumVersion  = "1.7.5"
val swaggerVersion     = "2.2.26"
val slickVersion       = "3.5.2"
val jasperVersion      = "6.20.0"

/**
  * Add runtime dependencies
  */
libraryDependencies ++= Seq(
  "org.apache.pekko"         %% "pekko-actor"                           % pekkoVersion
  , "org.apache.pekko"       %% "pekko-cluster-tools"                   % pekkoVersion
  , "org.apache.pekko"       %% "pekko-cluster-sharding"                % pekkoVersion
  , "org.apache.pekko"       %% "pekko-serialization-jackson"           % pekkoVersion
  , "org.apache.pekko"       %% "pekko-http"                            % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-http-core"                       % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-http-xml"                        % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-http-jackson"                    % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-http-cors"                       % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-persistence"                     % pekkoPersistenceVersion
  , "org.apache.pekko"       %% "pekko-persistence-query"               % pekkoPersistenceVersion
  , "org.apache.pekko"       %% "pekko-persistence-jdbc"                % "1.1.0"
  , "org.apache.pekko"       %% "pekko-persistence-cassandra"           % "1.0.0"
  , "com.datastax.oss"        %  "java-driver-core"                     % "4.17.0"
  , "com.datastax.oss"        %  "java-driver-query-builder"            % "4.17.0"

  // Logging
  , "org.apache.pekko"       %% "pekko-slf4j"                           % pekkoVersion
  , "org.apache.pekko"       %% "pekko-stream"                          % pekkoVersion
  , "com.typesafe"            %  "config"                               % "1.4.3"
  , "com.typesafe.scala-logging"      %% "scala-logging"                % "3.9.5"
  , "ch.qos.logback"          %  "logback-classic"                      % "1.5.12"
  , "org.apache.commons"      %  "commons-text"                         % "1.12.0" // StrSubstitutor usage inside process tasks
  , "com.beachape"            %% "enumeratum"                           % enumeratumVersion
  , "jakarta.xml.bind"        %  "jakarta.xml.bind-api"                 % "4.0.2" // Used in StringValue xsd date conversions

  // JWT Support
  , "com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
  , "com.github.j5ik2o"       %% "sw4jj"                                % "1.1.60" // Simple scala Wrapper For Java-Jwt
  , "com.nimbusds"            %  "nimbus-jose-jwt"                      % "9.40"
  , "com.nimbusds"            %  "oauth2-oidc-sdk"                      % "11.12"

  // DB Schema
  , "org.flywaydb"            %  "flyway-core"                          % "9.22.3"
  , "org.flywaydb"            %  "flyway-sqlserver"                      % "9.22.3"
  , "com.typesafe.slick"      %% "slick-hikaricp"                       % slickVersion
  , "com.typesafe.slick"      %% "slick"                                % slickVersion
  , "com.zaxxer"              %  "HikariCP"                              % "6.2.1"
  , "io.github.nafg.slick-migration-api" %% "slick-migration-api-flyway" % "0.11.0"
  , "io.github.nafg.slick-migration-api" %% "slick-migration-api"       % "0.10.0"

  , "com.fasterxml.jackson.core"   %  "jackson-databind"			            % jacksonVersion
  , "com.fasterxml.jackson.core"   %  "jackson-core"					            % jacksonVersion
  , "com.fasterxml.jackson.module" %% "jackson-module-scala"            % jacksonVersion

  // PDF Task support
  , "net.sf.jasperreports"    %  "jasperreports"                        % jasperVersion
  , "net.sf.jasperreports"    %  "jasperreports-fonts"                  % jasperVersion
  // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7 because that is what Jasper needs.
  , "com.lowagie"             %  "itext"                                % "2.1.7" // DO NOT CHANGE THIS VALUE

  // Mail & Calendar support
  , "com.sun.activation"      %  "jakarta.activation"                   % "2.0.1" // For mail & calendar support
  , "jakarta.activation"      %  "jakarta.activation-api"               % "2.1.3" // For mail & calendar support
  //, "jakarta.ws.rs"           %  "jakarta.ws.rs-api"                    % "3.1.0" // For mail & calendar support
  , "org.mnode.ical4j"        %  "ical4j"                               % "3.2.3"

  // Expression support (SPEL and JSONPath)
  , "com.jayway.jsonpath"  	  %  "json-path"                            % "2.6.0" // 2.7.0 is not compatible in expressions
  , "org.springframework"     %  "spring-expression"                    % "5.3.23"

  // Persistence support
  , "com.h2database"          %  "h2"                                   % "2.3.232"
  , "org.hsqldb"              %  "hsqldb"                               % "2.7.4"
  , "io.github.alstanchev"    %% "pekko-persistence-inmemory"            % "1.1.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
  , "org.postgresql"          %  "postgresql"                           % "42.7.4"
  , "com.microsoft.sqlserver" %  "mssql-jdbc"                           % "12.8.1.jre11"
  , "com.microsoft.azure"     % "msal4j"                                % "1.17.2"
  , "com.azure"               % "azure-identity"                        % "1.14.2"

  // Swagger support
  ,"jakarta.ws.rs" % "jakarta.ws.rs-api" % "4.0.0"
  , "io.swagger.core.v3"      %  "swagger-core"                         % swaggerVersion
  , "io.swagger.core.v3"      %  "swagger-annotations"                  % swaggerVersion
  , "io.swagger.core.v3"      %  "swagger-models"                       % swaggerVersion
  , "com.github.swagger-akka-http" %% "swagger-pekko-http"              % "2.14.0" excludeAll(ExclusionRule(organization = "org.apache.pekko"))
  ,"com.github.swagger-akka-http"  %% "swagger-scala-module"            % "2.13.0" excludeAll(ExclusionRule(organization = "org.apache.pekko"))
  , "javax.xml.bind"          %  "jaxb-api"                             % "2.3.1" // Note: this one is still needed for swagger-pekko-http :(
  ,"org.yaml"                 %"snakeyaml"                              % "2.3"
  // metrics support
//  ,"io.kamon"                 %% "kamon-pekko"                          % "2.7.3"
//  ,"io.kamon"                 % "kanela-agent"                          % "1.0.18"
)

/**
  * Add test dependencies
  */
libraryDependencies ++= Seq(
  "org.junit.jupiter"         %  "junit-jupiter-api"                    % "5.11.3"
  , "com.novocode"            %  "junit-interface"                      % "0.11"
  , "org.scalamock"           %% "scalamock"                            % "6.0.0"
  , "org.scalatest"           %% "scalatest"                            % "3.2.19"
  , "commons-io"              %  "commons-io"                           % "20030203.000550"
  , "org.apache.pekko"       %% "pekko-testkit"                         % pekkoVersion
  , "org.apache.pekko"       %% "pekko-http-testkit"                    % pekkoHttpVersion
  , "org.apache.pekko"       %% "pekko-multi-node-testkit"              % pekkoVersion
  , "io.github.alstanchev"   %% "pekko-persistence-inmemory"            % "1.1.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
  , "com.github.tomakehurst"  %  "wiremock"                             % "3.0.1"
).map(dep => dep % Test)
