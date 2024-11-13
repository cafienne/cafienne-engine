package com.casefabric.infrastructure.config

import com.typesafe.config.ConfigFactory

object TestConfig {
  val config = ConfigFactory.parseString(
    """
      |      pekko {
      |        loglevel = "DEBUG"
      |        stdout-loglevel = "DEBUG"
      |        loggers = ["org.apache.pekko.testkit.TestEventListener"]
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
      |            casefabric_serializer = "com.casefabric.infrastructure.serialization.CaseFabricSerializer"
      |          }
      |          serialization-bindings {
      |            "com.casefabric.infrastructure.serialization.CaseFabricSerializable" = casefabric_serializer
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
      |    }
    """.stripMargin
  )
}
