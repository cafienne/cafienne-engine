package org.cafienne.infrastructure.jdbc.schema

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.jdbc.CafienneJDBCConfig
import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, SlickFlyway}

import scala.concurrent.Await

/**
  * Simple flyway abstraction that can be used to define and validate a JDBC database schema
  */
trait CafienneDatabaseDefinition extends CafienneJDBCConfig with LazyLogging {
  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  implicit val infoProvider: MigrationInfo.Provider[Migration] = CustomMigrationInfo.provider

  def useSchema(schemas: Seq[DbSchemaVersion]) = {
    try {
      val flyway = SlickFlyway(db)(schemas.flatMap(schema => schema.getScript)).load()
      flyway.migrate()
    } catch {
      case e: Exception => {
        logger.error("An issue with migration happened", e)
        val my = sql"""select description from flyway_schema_history""".as[String]
        val res = db.stream(my)
        val bla = res.foreach { r => logger.debug("Migration: {}", r)}
        val answer = Await.result(bla, 5.seconds)
        //val answer = Await.result(res, 5.seconds)
        logger.debug("Migration contents: {}",answer)
        throw e
      }
    }
  }
}
