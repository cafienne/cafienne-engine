package org.cafienne.scheduler

import akka.persistence.PersistentActor

class Scheduler extends PersistentActor {
  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = ???

  override def persistenceId: String = ???
}
