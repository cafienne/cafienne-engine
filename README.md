# Cafienne Engine for CMMN 1.1

Cafienne hosts an open source Case Management Engine which natively executes the CMMN 1.1 standard.
The engine is written in Java and Scala co-operated with the Pekko toolkit.
This technical foundation makes it a platform for building highly concurrent,
distributed, and resilient message-driven case management applications.

## 1. Introduction

The suite comprises several components, namely;

1. A multi-tenant service hosting among others a CMMN interpreter
2. The Cafienne IDE, for creating models that are interpreted by the engine
3. A basic User Interface that can help in running and debugging models

In this readme we limit to the installation of a working case engine.
After you installed the case engine we advise you to follow the [Getting Started wiki](https://github.com/cafienne/cafienne-engine/wiki/Getting-Started).

## 1.1 Installation
Currently, we develop and work with the Case Service on Mac OS X and Windows.
Cassandra and Postgres configuration is the most comprehensive part of the installation.
We assume you know how to install Java, Scala and sbt, etc.

### 1.1.1 Prerequisites
1. OpenJDK 11
2. Apache Cassandra 3.x
3. Postgres 12.x
4. Scala 2.13 (http://www.scala-lang.org/download/)
5. SBT build tool >= 1.3

### 1.1.2 Case Service

#### 1.1.2.1 Installation
After you successfully installed Cassandra and Postgres you are ready for the Case Service installation.

#### 1.1.2.2 Run the Case Service from IntelliJ
1. If you want to run Case Service from IntelliJ you have to checkout the sources directly from IntelliJ and create a new project based on these sources:

```sh
https://github.com/cafienne/cafienne-engine.git
```

2. In the Run Configuration change the VM options to:
```sh
-Dcom.sun.management.jmxremote.port=9999
-Djava.net.preferIPv4Stack=true
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
-Dfile.encoding=UTF-8 -Dconfig.file=local.conf
-XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled
-XX:MaxPermSize=512M
-Xms256m
-Xmx1024m
-Djava.awt.headless=true
```

3. Run Case Service:

Run Main.scala:
```sh
case-service/src/main/scala/org/cafienne/service/Main.scala
```

#### 1.1.2.3 Run Case Service from the console

1. clone Case Service from github
```sh
$ git clone https://github.com/cafienne/cafienne-engine.git
```

2. Build the Case Service sources (this will generate a zip containing the engine)
```sh
$ cd ./cafienne
$ sbt universal:packageBin
```

3. Copy and unpack the generated zip
``` sh
$ cp case-service/target/universal/cafienne.zip ~
$ cd ~ && unzip ~/cafienne.zip
```

4. Run the Case Service:
```sh
$ cd ~/cafienne/bin
$ ./case-service (or case-service.bat on Windows)
```

#### 1.1.2.4 Run as a Docker image

Checkout the [Getting Started](https://github.com/cafienne/getting-started) repository.

## 1.2 FAQ

1.2.1 I get a log4J timeout error, what to do?
Answer: Increase the timeout setting which is 5 seconds by default. Go to local.conf or application.conf and add the following setting which you can find below the loggers entry.
```sh
logger-startup-timeout = 10s
```

## 1.3 Contribution Process

This project uses the [C4 process](https://rfc.zeromq.org/spec:42/C4/) for all code changes. "Everyone, without distinction or discrimination, SHALL have an equal right to become a Contributor under the terms of this contract."

## 1.4 Getting started
After you installed the Case Service you are ready to start building and running your first demo CMMN Case Model.
You can read the [Getting Started wiki](https://github.com/cafienne/cafienne-engine/wiki/Getting-Started) to learn how to use the Case Service.

## 1.5 Dual License

Free use of this software is granted under the terms of the GNU Affero General Public License version 3. 
For details see the file `LICENSE` included with the distribution.

Commercial use is covered by the Batav Cafienne SLA. 

All the documentation is covered by the CC0 license *(do whatever you want with it - public domain)*.

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

To the extent possible under law, [Cafienne B.V.](http://cafienne.io) has waived all copyright and related or neighboring rights to this work.
