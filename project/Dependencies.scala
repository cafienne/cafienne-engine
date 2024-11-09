/*
 * Copyright (C) 2016-2019 Cafienne B.V. <https://www.cafienne.io/bounded>
 */

import sbt._

object Dependencies {

  val pekkoHttpVersion    = "1.1.0"
  val pekkoVersion        = "1.1.2"
  val pekkoPersistenceVersion = "1.1.2"
  val jacksonVersion     = "2.17.2"
  val enumeratumVersion  = "1.7.4"
  val swaggerVersion     = "2.2.23"
  val slickVersion       = "3.5.1"
  val jasperVersion      = "6.20.0"

  def pekkoModule(name: String, version: String = pekkoVersion) =
    "org.apache.pekko" %% s"pekko-$name" % version

  def pekkoHttpModule(name: String, version: String = pekkoHttpVersion) =
    "org.apache.pekko" %% s"pekko-$name" % version

  val baseEngineDeps = Seq(
       pekkoModule("actor")
      ,pekkoModule("cluster-tools")
      ,pekkoModule("cluster-sharding")
      ,pekkoModule("serialization-jackson")
      ,pekkoModule("persistence", version = pekkoPersistenceVersion)
      ,pekkoModule("persistence-query", version = pekkoPersistenceVersion)
      ,pekkoModule("persistence-jdbc", version = "1.1.0")
      ,pekkoModule("stream")
      // Logging
      ,pekkoModule("slf4j")
      , "com.typesafe"            %  "config"                               % "1.4.3"
      , "com.typesafe.scala-logging" %% "scala-logging"                     % "3.9.5"
      , "ch.qos.logback"          %  "logback-classic"                      % "1.5.6"
      , "org.apache.commons"      %  "commons-text"                         % "1.11.0" // StrSubstitutor usage inside process tasks
      , "com.beachape"            %% "enumeratum"                           % enumeratumVersion
      , "jakarta.xml.bind"        %  "jakarta.xml.bind-api"                 % "4.0.2" // Used in StringValue xsd date conversions
      // DB Schema
      , "org.flywaydb"            %  "flyway-core"                          % "9.22.3"
      , "org.flywaydb"            %  "flyway-sqlserver"                     % "9.22.3"
      , "com.typesafe.slick"      %% "slick-hikaricp"                       % slickVersion
      , "com.typesafe.slick"      %% "slick"                                % slickVersion
      , "com.zaxxer"              %  "HikariCP"                              % "5.1.0"
      , "io.github.nafg.slick-migration-api" %% "slick-migration-api-flyway" % "0.11.0"
      , "io.github.nafg.slick-migration-api" %% "slick-migration-api"       % "0.10.0"
      , "com.fasterxml.jackson.core"   %  "jackson-databind"			    % jacksonVersion
      , "com.fasterxml.jackson.core"   %  "jackson-core"					% jacksonVersion
      , "com.fasterxml.jackson.module" %% "jackson-module-scala"            % jacksonVersion
      // Expression support (SPEL and JSONPath)
      , "com.jayway.jsonpath"  	  %  "json-path"                            % "2.6.0" // 2.7.0 is not compatible in expressions
      , "org.springframework"     %  "spring-expression"                    % "5.3.23"
      // Persistence support
      , "com.h2database"          %  "h2"                                   % "2.2.220"
      , "org.postgresql"          %  "postgresql"                           % "42.5.5"
      , "com.microsoft.sqlserver" %  "mssql-jdbc"                           % "12.8.1.jre11"
    )

  //Extended engine deps are bundled with service
  val extendEngineDeps = Seq(
      //persistence cassandra support
      "org.apache.pekko"       %% "pekko-persistence-cassandra"           % "1.0.0"
      , "com.datastax.oss"        %  "java-driver-core"                     % "4.17.0"
      , "com.datastax.oss"        %  "java-driver-query-builder"            % "4.17.0"
      // PDF Task support
      , "net.sf.jasperreports"    %  "jasperreports"                        % jasperVersion
      , "net.sf.jasperreports"    %  "jasperreports-fonts"                  % jasperVersion
      // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7 because that is what Jasper needs.
      , "com.lowagie"             %  "itext"                                % "2.1.7" // DO NOT CHANGE THIS VALUE
      // Mail & Calendar support
      , "com.sun.activation"      %  "jakarta.activation"                   % "2.0.1" // For mail & calendar support
      , "jakarta.activation"      %  "jakarta.activation-api"               % "2.1.0" // For mail & calendar support
      //, "jakarta.ws.rs"           %  "jakarta.ws.rs-api"                    % "3.1.0" // For mail & calendar support
      , "org.mnode.ical4j"        %  "ical4j"                               % "3.2.3"
      // Persistence support
      , "org.hsqldb"              %  "hsqldb"                               % "2.7.2"
      , "io.github.alstanchev"    %% "pekko-persistence-inmemory"            % "1.1.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
      // metrics support
    //  ,"io.kamon"                 %% "kamon-pekko"                          % "2.7.3"
    //  ,"io.kamon"                 % "kanela-agent"                          % "1.0.18"
      )

  val serviceDeps = baseEngineDeps ++ Seq(
      pekkoHttpModule("http")
      ,pekkoHttpModule("http-core")
      ,pekkoHttpModule("http-xml")
      ,pekkoHttpModule("http-jackson")
      , "org.apache.pekko"       %% "pekko-http-cors"                      % "1.1.0" 
      // Swagger support
      ,"jakarta.ws.rs"           % "jakarta.ws.rs-api"                     % "4.0.0"
      ,"io.swagger.core.v3"      %  "swagger-core"                         % swaggerVersion
      ,"io.swagger.core.v3"      %  "swagger-annotations"                  % swaggerVersion
      ,"io.swagger.core.v3"      %  "swagger-models"                       % swaggerVersion
      ,"com.github.swagger-akka-http" %% "swagger-pekko-http"              % "2.12.2" excludeAll(ExclusionRule(organization = "org.apache.pekko"))
      ,"com.github.swagger-akka-http" %% "swagger-scala-module"            % "2.12.3" excludeAll(ExclusionRule(organization = "org.apache.pekko"))
      ,"javax.xml.bind"          %  "jaxb-api"                             % "2.3.1" // Note: this one is still needed for swagger-pekko-http
      ,"org.yaml"                %"snakeyaml"                              % "2.0"
      // JWT Support
      ,"com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
      ,"com.github.j5ik2o"       %% "sw4jj"                                % "1.1.60" // Simple scala Wrapper For Java-Jwt
      ,"com.nimbusds"            %  "nimbus-jose-jwt"                      % "9.40"
      ,"com.nimbusds"            %  "oauth2-oidc-sdk"                      % "11.12"
    )
  
  val engineTestDeps = Seq(
    "org.junit.jupiter"         %  "junit-jupiter-api"                    % "5.10.3"
    , "com.novocode"            %  "junit-interface"                      % "0.11"
    , "org.scalamock"           %% "scalamock"                            % "5.2.0"
    , "org.scalatest"           %% "scalatest"                            % "3.2.19"
    , "commons-io"              %  "commons-io"                           % "20030203.000550"
    , "org.apache.pekko"       %% "pekko-testkit"                         % pekkoVersion
      ,"org.apache.pekko"      %% "pekko-persistence-testkit"             % pekkoVersion
    //, "org.apache.pekko"       %% "pekko-multi-node-testkit"              % pekkoVersion
    , "io.github.alstanchev"   %% "pekko-persistence-inmemory"            % "1.1.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
    , "com.github.tomakehurst"  %  "wiremock"                             % "2.27.2"
  ).map(dep => dep % Test)

  val serviceTestDeps = engineTestDeps ++ Seq(
    "org.apache.pekko"       %% "pekko-http-testkit"                    % pekkoHttpVersion
  ).map(dep => dep % Test)
}
