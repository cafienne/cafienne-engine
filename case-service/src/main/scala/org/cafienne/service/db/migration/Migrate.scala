package org.cafienne.service.db.migration

import com.typesafe.scalalogging.Logger
import org.cafienne.service.db.migration.versions.V1Migration
import slick.migration.api.flyway.{MigrationInfo, SlickFlyway}
import org.slf4j.LoggerFactory
import slick.migration.api.Migration
import slick.migration.api.flyway.MigrationInfo.Provider
import slick.migration.api.flyway.MigrationInfo.Provider.{crc32, sql}

import scala.concurrent.Await
import scala.util.{Failure, Success}

/**
  * This is a hack to prevent comparison of migration.toString in the description field of the flyway table
  * as that comparison contains object references and may be longer than 200 chars (length of the description field)
  * Ticket added: https://github.com/nafg/slick-migration-api-flyway/issues/26
  */
object MigrationInfoHack {
  def hack: Provider[Migration] =
    new Provider[Migration]({ migration =>
      val sqlStrings = sql(migration)
      MigrationInfo(
        description = migration.getClass.getSimpleName, // <- actual override
        script = sqlStrings.mkString("\n"),
        checksum = Some(crc32(sqlStrings).toInt),
        location = migration.getClass.getName
      )
    })
}

object Migrate extends MigrationConfig {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import dbConfig.profile.api._
  implicit val infoProvider: MigrationInfo.Provider[Migration] = MigrationInfoHack.hack
//  implicit val infoProvider: MigrationInfo.Provider[Migration] = MigrationInfo.Provider.strict

  val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  val my = sql"""select description from flyway_schema_history""".as[String]

  def migrateDatabase(): Unit = {
    try {
      val flyway = SlickFlyway(db)(V1Migration.getMigrations).load()
      flyway.migrate()
    } catch {
      case e: Exception => {
        logger.error("An issue with migration happened {}", e.getMessage)
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
