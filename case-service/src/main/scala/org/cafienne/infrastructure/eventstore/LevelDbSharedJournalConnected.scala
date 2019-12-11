package org.cafienne.infrastructure.eventstore

import akka.actor._

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import akka.actor.ActorIdentity
import akka.actor.Identify

import scala.util.{Failure, Success}

trait LevelDbSharedJournalConnected extends Actor with ActorLogging {
  override def preStart(): Unit = {

    val levelDBisUsed = context.system.settings.config.getString("akka.persistence.journal.plugin").contains("leveldb")

    if (levelDBisUsed) {
      log.debug("Try to find actor address for shared store")
      val path = "akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"
      import context.dispatcher
      implicit val timeout = Timeout(1.minute)
      val f = context.actorSelection(path) ? Identify(None)
      f.onComplete {
        case Success(res) => res match {
          case ActorIdentity(_, Some(ref)) =>
            log.info("LevelDB shared journal found at {}", ref)
            SharedLeveldbJournal.setStore(ref, context.system)
          case _ =>
            log.error("LevelDB shared journal not found at {}", path)
            context.system.terminate()
        }
        case Failure(_) =>
          log.error("Lookup of LevelDB shared journal at {} timed out", path)
          context.system.terminate()
      }
    } else {
      val foundDriver = context.system.settings.config.getString("akka.persistence.journal.plugin")
      log.info(s"no leveldb setup, did not connect, setup is $foundDriver")
    }
  }
}