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

import sbt._

object Dependencies {

  val pekkoHttpVersion    = "1.1.0"
  val pekkoVersion        = "1.1.2"
  val pekkoPersistenceVersion = "1.1.2"
  val jacksonVersion     = "2.18.3"
  val enumeratumVersion  = "1.7.5"
  val swaggerVersion     = "2.2.28"
  val slickVersion       = "3.5.2"
  val jasperVersion      = "6.20.0"

  def pekkoModule(name: String, version: String = pekkoVersion): ModuleID = "org.apache.pekko" %% s"pekko-$name" % version
  def pekkoPersistenceModule(name: String, version: String = pekkoPersistenceVersion): ModuleID = pekkoModule(s"persistence-$name", version)
  def pekkoHttpModule(name: String, version: String = pekkoHttpVersion): ModuleID = "org.apache.pekko" %% s"pekko-$name" % version
  def jacksonModule(name: String, version: String = jacksonVersion): ModuleID = "com.fasterxml.jackson.core" % s"jackson-$name" % version

  val engine: Seq[ModuleID] = Seq(
      pekkoModule("actor")
    , pekkoModule("cluster-tools")
    , pekkoModule("cluster-sharding")
    , pekkoModule("serialization-jackson")
    , pekkoModule("stream")
    // Dependencies for Persistence
    , pekkoModule("persistence", version = pekkoPersistenceVersion)
    , pekkoPersistenceModule("query")
    // JDBC Persistence support
    , pekkoPersistenceModule("jdbc", version = "1.1.0")
    , "com.h2database"          %  "h2"                                   % "2.3.232"
    , "org.postgresql"          %  "postgresql"                           % "42.7.4"
    , "com.microsoft.sqlserver" %  "mssql-jdbc"                           % "12.8.1.jre11"
    // Cassandra Persistence support
    , pekkoPersistenceModule("cassandra", version = "1.0.0")
    , "com.datastax.oss"        %  "java-driver-core"                     % "4.17.0"
    , "com.datastax.oss"        %  "java-driver-query-builder"            % "4.17.0"
    // In-Memory persistence support
    , "io.github.alstanchev"    %% "pekko-persistence-inmemory"            % "1.2.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
    // Config & logging
    , pekkoModule("slf4j")
    , "com.typesafe"            %  "config"                               % "1.4.3"
    , "com.typesafe.scala-logging" %% "scala-logging"                     % "3.9.5"
    , "ch.qos.logback"          %  "logback-classic"                      % "1.5.17"
    , "org.apache.commons"      %  "commons-text"                         % "1.13.0" // StrSubstitutor usage inside process tasks
    , "com.beachape"            %% "enumeratum"                           % enumeratumVersion
    , "jakarta.xml.bind"        %  "jakarta.xml.bind-api"                 % "4.0.2" // Used in StringValue xsd date conversions
    // DB Schema
    , "org.flywaydb"            %  "flyway-core"                          % "9.22.3"
    , "org.flywaydb"            %  "flyway-sqlserver"                     % "9.22.3"
    , "com.typesafe.slick"      %% "slick-hikaricp"                       % slickVersion
    , "com.typesafe.slick"      %% "slick"                                % slickVersion
    , "com.zaxxer"              %  "HikariCP"                             % "6.2.1"
    , "io.github.nafg.slick-migration-api" %% "slick-migration-api-flyway" % "0.11.0"
    , "io.github.nafg.slick-migration-api" %% "slick-migration-api"       % "0.10.0"
    // JSON support
    , jacksonModule("core")
    , jacksonModule("databind")
    , "com.fasterxml.jackson.module" %% "jackson-module-scala"            % jacksonVersion
    // Expression support (SPEL and JSONPath)
    , "com.jayway.jsonpath"     %  "json-path"                            % "2.9.0" // 2.7.0 is not compatible in expressions
    , "org.springframework"     %  "spring-expression"                    % "6.2.4"
  )

  //Extended engine deps are bundled with service
  val plugins: Seq[ModuleID] = Seq(
    // PDF Task support
      "net.sf.jasperreports"    %  "jasperreports"                        % jasperVersion
    , "net.sf.jasperreports"    %  "jasperreports-fonts"                  % jasperVersion
    // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7 because that is what Jasper needs.
    , "com.lowagie"             %  "itext"                                % "2.1.7" // DO NOT CHANGE THIS VALUE
    // Mail & Calendar support
    , "com.sun.activation"      %  "jakarta.activation"                   % "2.0.1" // For mail & calendar support
    , "jakarta.activation"      %  "jakarta.activation-api"               % "2.1.3" // For mail & calendar support
    //, "jakarta.ws.rs"           %  "jakarta.ws.rs-api"                    % "3.1.0" // For mail & calendar support
    , "org.mnode.ical4j"        %  "ical4j"                               % "3.2.3"
    // Persistence support
    , "org.hsqldb"              %  "hsqldb"                               % "2.7.4"
    // metrics support
  //  ,"io.kamon"                 %% "kamon-pekko"                          % "2.7.3"
  //  ,"io.kamon"                 % "kanela-agent"                          % "1.0.18"
  )

  val service: Seq[ModuleID] = engine ++ Seq(
      pekkoHttpModule("http")
    , pekkoHttpModule("http-core")
    , pekkoHttpModule("http-xml")
    , pekkoHttpModule("http-jackson")
    , pekkoHttpModule("http-cors")
    // Swagger support
    , "jakarta.ws.rs"           % "jakarta.ws.rs-api"                     % "4.0.0"
    , "io.swagger.core.v3"      %  "swagger-core"                         % swaggerVersion
    , "io.swagger.core.v3"      %  "swagger-annotations"                  % swaggerVersion
    , "io.swagger.core.v3"      %  "swagger-models"                       % swaggerVersion
    , "com.github.swagger-akka-http" %% "swagger-pekko-http"              % "2.12.2" excludeAll ExclusionRule(organization = "org.apache.pekko")
    , "com.github.swagger-akka-http" %% "swagger-scala-module"            % "2.12.3" excludeAll ExclusionRule(organization = "org.apache.pekko")
    , "javax.xml.bind"          %  "jaxb-api"                             % "2.3.1" // Note: this one is still needed for swagger-pekko-http
    , "org.yaml"                %"snakeyaml"                              % "2.4"
    // JWT Support
    , "com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
    , "com.github.j5ik2o"       %% "sw4jj"                                % "1.1.60" // Simple scala Wrapper For Java-Jwt
    , "com.nimbusds"            %  "nimbus-jose-jwt"                      % "10.0.2"
    , "com.nimbusds"            %  "oauth2-oidc-sdk"                      % "11.23.1"
  )

  val testEngine: Seq[ModuleID] = Seq(
    "org.junit.jupiter"         %  "junit-jupiter-api"                    % "5.11.3"
    , "com.novocode"            %  "junit-interface"                      % "0.11"
    , "org.scalamock"           %% "scalamock"                            % "6.0.0"
    , "org.scalatest"           %% "scalatest"                            % "3.2.19"
    , "commons-io"              %  "commons-io"                           % "20030203.000550"
    , pekkoModule("testkit")
    , pekkoModule("persistence-testkit")
    , pekkoModule("multi-node-testkit")
    , "io.github.alstanchev"    %% "pekko-persistence-inmemory"           % "1.1.1"  excludeAll ExclusionRule(organization = "org.apache.pekko")
    , "com.github.tomakehurst"  %  "wiremock"                             % "3.0.1"
    , "org.hsqldb"              %  "hsqldb"                               % "2.7.4"
  ).map(dep => dep % Test)

  val testService: Seq[ModuleID] = testEngine ++ Seq(
    "org.apache.pekko"       %% "pekko-http-testkit"                    % pekkoHttpVersion
  ).map(dep => dep % Test)
}
