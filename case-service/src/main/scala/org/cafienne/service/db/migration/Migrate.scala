package org.cafienne.service.db.migration

import com.typesafe.scalalogging.Logger
import org.cafienne.service.db.migration.versions.CafienneQueryDatabaseSchema
import org.slf4j.LoggerFactory
import slick.migration.api.Migration
import slick.migration.api.flyway.MigrationInfo.Provider
import slick.migration.api.flyway.{MigrationInfo, SlickFlyway}

import scala.concurrent.Await

/**
  * Due to an earlier bug in slick flyway migration library, description did not give repeated predictable outcome.
  * Therefore in Cafienne we made a CustomMigrationInfo (called MigrationInfoHack) to overcome this problem.
  *
  * Original ticket: https://github.com/nafg/slick-migration-api-flyway/issues/26
  *
  * The bug has been fixed in the library; however, in the new version, the construction of the description is done
  * in a different manner than in the Cafienne version. Hence we need to continue to use our own version.
  * So we have renamed it to CustomMigrationInfo instead of MigrationInfoHack...
  */
object CustomMigrationInfo {
  import slick.migration.api.flyway.MigrationInfo.Provider.{crc32, sql}

  def provider: Provider[Migration] = {
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
}

object Migrate extends QueryDbMigrationConfig {
  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  implicit val infoProvider: MigrationInfo.Provider[Migration] = CustomMigrationInfo.provider

  val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  val my = sql"""select description from flyway_schema_history""".as[String]

  def migrateDatabase(): Unit = {
    try {
      val flyway = SlickFlyway(db)(CafienneQueryDatabaseSchema.schema).load()
      flyway.migrate()
    } catch {
      case e: Exception => {
        logger.error("An issue with migration happened", e)
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
