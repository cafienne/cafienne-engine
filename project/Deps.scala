import sbt._

object Deps {
  val depsRepos = Seq(
    "maven repo"          at "https://repo.maven.apache.org/maven2",
    "typesafe releases"   at "https://repo.typesafe.com/typesafe/releases",
    "typesafe snapshots"  at "https://repo.typesafe.com/typesafe/snapshots",
    "akka repo"           at "https://repo.akka.io/"
  )

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  object V {
    val scala       = "2.13.6"
    val spray       = "1.3.6"
    val akkaHttp    = "10.2.7"
    val akka        = "2.6.17"
    val jackson     = "2.13.0"
    val enumeratum  = "1.7.0"
    val swagger     = "2.1.11"
    val slick       = "3.3.3"
    val jasper      = "6.18.1"
  }

  val akkaActor             = "com.typesafe.akka"       %% "akka-actor"                           % V.akka
  val akkaStream            = "com.typesafe.akka"       %% "akka-stream"                          % V.akka
  val akkaContrib           = "com.typesafe.akka"       %% "akka-contrib"                         % "2.5.32"
  val akkaClusterTools      = "com.typesafe.akka"       %% "akka-cluster-tools"                   % V.akka
  val akkaPersistence       = "com.typesafe.akka"       %% "akka-persistence"                     % V.akka    // exclude("org.iq80.leveldb","leveldb")
  val akkaQuery             = "com.typesafe.akka"       %% "akka-persistence-query"               % V.akka
  val akkaClusterSharding   = "com.typesafe.akka"       %% "akka-cluster-sharding"                % V.akka
  val cassandraPersistence	= "com.typesafe.akka"		    %% "akka-persistence-cassandra" 		      % "0.107"
  val cassandraExtras =     "com.datastax.cassandra"     % "cassandra-driver-extras"              % "3.11.0"

  val akkaInMemoryTestDB    = "com.github.dnvriend"     %% "akka-persistence-inmemory"            % "2.5.15.2" excludeAll ExclusionRule(organization = "com.typesafe.akka")
  val akkaPersistenceJDBC   = "com.lightbend.akka"      %% "akka-persistence-jdbc"                % "4.0.0"
  val akkaTestKit           = "com.typesafe.akka"       %% "akka-testkit"                         % V.akka
  val akkaMultiNodeTestKit  = "com.typesafe.akka"       %% "akka-multi-node-testkit"              % V.akka
  val akkaSlf4j             = "com.typesafe.akka"       %% "akka-slf4j"                           % V.akka
  val bcrypt                = "com.github.t3hnar"       %% "scala-bcrypt"                         % "4.3.0"
  val logback               = "ch.qos.logback"          %  "logback-classic"                      % "1.2.9"
  val config                = "com.typesafe"            %  "config"                               % "1.4.1"
  val scalaLogging          = "com.typesafe.scala-logging"      %% "scala-logging"                % "3.9.4"
  val enumeratum            = "com.beachape"            %% "enumeratum"                           % V.enumeratum
  val joseJwt               = "com.nimbusds"            % "nimbus-jose-jwt"                       % "9.15.2"

  val akkaHttp              = "com.typesafe.akka"       %% "akka-http"                            % V.akkaHttp
  val akkaHttpXml           = "com.typesafe.akka"       %% "akka-http-xml"                        % V.akkaHttp
  val akkaHttpSprayJson     = "com.typesafe.akka"       %% "akka-http-spray-json"                 % V.akkaHttp
  val akkaHttpCore          = "com.typesafe.akka"       %% "akka-http-core"                       % V.akkaHttp
  val akkaHttpTestkit       = "com.typesafe.akka"       %% "akka-http-testkit"                    % V.akkaHttp
  val akkaHtppJackson       = "com.typesafe.akka"       %% "akka-http-jackson"                    % V.akkaHttp
  val akkaHttpCors          = "ch.megard"               %% "akka-http-cors"                       % "1.1.2"
  // As suggested in https://stackoverflow.com/questions/43574426/how-to-resolve-java-lang-noclassdeffounderror-javax-xml-bind-jaxbexception-in-j
  // to resolve blow-up due to swagger :  java.lang.NoClassDefFoundError: javax/xml/bind/annotation/XmlRootElement.
  val javaxws               = "javax.ws.rs"             % "javax.ws.rs-api"                       % "2.1.1"
  val swaggerAkkaHttp       = "com.github.swagger-akka-http" %% "swagger-akka-http"               % "2.5.2"
  val swaggerCore           = "io.swagger.core.v3"      % "swagger-core"                          % V.swagger
  val swaggerAnnotations    = "io.swagger.core.v3"      % "swagger-annotations"                   % V.swagger
  val swaggerModels         = "io.swagger.core.v3"      % "swagger-models"                        % V.swagger
  val swaggerjaxrs2         = "io.swagger.core.v3"      % "swagger-jaxrs2"                        % V.swagger

  // The test scope will be added in the build so we don't need to declare it in here
  val scalaMock             = "org.scalamock"           %% "scalamock"                            % "5.1.0"
  val scalaTest             = "org.scalatest"           %% "scalatest"                            % "3.2.9"
  val junit                 = "org.junit.jupiter"       % "junit-jupiter-api"                     % "5.8.2"
  val sbtJUnitInterface     = "com.novocode"            % "junit-interface"                       % "0.11"
  val wireMock              = "com.github.tomakehurst"  % "wiremock"                              % "2.27.2"
  val commonsIO             = "commons-io"              %  "commons-io"                           % "20030203.000550"
  val apacheCommonsText     = "org.apache.commons"      % "commons-text"                          % "1.9"
  val jsonJava              = "com.fasterxml.jackson.core"   % "jackson-core"					            % V.jackson
  val jacksonDatabind       = "com.fasterxml.jackson.core"   % "jackson-databind"			            % V.jackson
  val jacksonScala          = "com.fasterxml.jackson.module" %% "jackson-module-scala"            % V.jackson

  val spel                  = "org.springframework"     %  "spring-expression"                    % "5.3.13"
  val jsonPath              = "com.jayway.jsonpath"  	  % "json-path"                             % "2.6.0"

  val javaMail              = "com.sun.mail"            % "javax.mail"                            % "1.6.2"
  val ical4j                = "org.mnode.ical4j"        % "ical4j"                                % "3.1.1"
  val jasperReports         = "net.sf.jasperreports"    % "jasperreports"                         % V.jasper
  val jasperReportFonts     = "net.sf.jasperreports"    % "jasperreports-fonts"                   % V.jasper
  // Lowagie is for PDF document generation with Jasper. It must remain fixed on 2.1.7, because that is what Jasper needs.
  val lowagie               = "com.lowagie"             % "itext"                                 % "2.1.7" // DO NOT CHANGE THIS VALUE

  val sw4jj                 = "com.github.j5ik2o"       %% "sw4jj"                                % "1.1.59"
  val slick                 = "com.typesafe.slick"      %% "slick"                                % V.slick
  val hikariCP              = "com.typesafe.slick"      %% "slick-hikaricp"                       % V.slick
  val postgres              = "org.postgresql"          % "postgresql"                            % "42.3.1"
  val h2                    = "com.h2database"          % "h2"                                    % "2.0.202"
  val hsqldb                = "org.hsqldb"              % "hsqldb"                                % "2.5.1"
  val sqlserver             = "com.microsoft.sqlserver" % "mssql-jdbc"                            % "9.2.1.jre11"
  val flyway                = "org.flywaydb"            % "flyway-core"                           % "7.2.1"
  val slickMigration        = "io.github.nafg"          %% "slick-migration-api"                  % "0.8.0"
  val flywaySlickBindings   = "io.github.nafg"          %% "slick-migration-api-flyway"           % "0.7.0"

  val javaXmlJDK9Compat     = "javax.xml.bind"          % "jaxb-api"                              % "2.3.1" // Used in StringValue xsd date conversions

  object gatling {
    private val v   = "3.4.0-M1"
    val app                 = "io.gatling"              %  "gatling-app"                          % v
    val recorder            = "io.gatling"              %  "gatling-recorder"                     % v
    val charts              = "io.gatling.highcharts"   %  "gatling-charts-highcharts"            % v
  }
}
