package org.cafienne.service.api.writer

import com.typesafe.config.ConfigFactory

object TestConfig {
  val config = ConfigFactory.parseString(
    """
      |      akka {
      |        loglevel = "DEBUG"
      |        stdout-loglevel = "DEBUG"
      |        loggers = ["akka.testkit.TestEventListener"]
      |        actor {
      |          default-dispatcher {
      |            executor = "fork-join-executor"
      |            fork-join-executor {
      |              parallelism-min = 8
      |              parallelism-factor = 2.0
      |              parallelism-max = 8
      |            }
      |          }
      |          serialize-creators = off
      |          serialize-messages = off
      |
      |          serializers {
      |            cafienne_serializer = "org.cafienne.actormodel.serialization.CafienneSerializer"
      |          }
      |          serialization-bindings {
      |            "org.cafienne.actormodel.serialization.CafienneSerializable" = cafienne_serializer
      |          }
      |        }
      |
      |      persistence {
      |       publish-confirmations = on
      |       publish-plugin-commands = on
      |       journal {
      |          plugin = "inmemory-journal"
      |       }
      |      }
      |      test {
      |        single-expect-default = 10s
      |        timefactor = 1
      |      }
      |    }
      |
      |    inmemory-journal {
      |      event-adapters {
      |         tagging = "org.cafienne.actormodel.tagging.CaseTaggingEventAdapter"
      |      }
      |      event-adapter-bindings {
      |        "org.cafienne.actormodel.event.ModelEvent" = tagging
      |      }
      |    }
    """.stripMargin
  )
}
