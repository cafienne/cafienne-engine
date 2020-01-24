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
      |            command_serializer = "org.cafienne.akka.actor.serialization.CommandSerializer"
      |            response_serializer = "org.cafienne.akka.actor.serialization.ResponseSerializer"
      |            event_serializer = "org.cafienne.akka.actor.serialization.EventSerializer"
      |          }
      |          serialization-bindings {
      |            "org.cafienne.akka.actor.event.ModelEvent" = event_serializer
      |            "org.cafienne.akka.actor.command.ModelCommand" = command_serializer
      |            "org.cafienne.akka.actor.command.response.ModelResponse" = response_serializer
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
      |         tagging = "org.cafienne.akka.actor.tagging.CaseTaggingEventAdapter"
      |
      |      }
      |      event-adapter-bindings {
      |        "org.cafienne.akka.actor.event.ModelEvent" = tagging
      |      }
      |    }
    """.stripMargin
  )
}
