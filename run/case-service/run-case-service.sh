#!/bin/bash
java -Dcom.sun.management.jmxremote.port=9999 \
-Djava.net.preferIPv4Stack=true \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dfile.encoding=UTF-8 \
-Dconfig.file=local.conf \
-XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:MaxPermSize=512M \
-Xms256m -Xmx1024m -Djava.awt.headless=true \
-cp ../../case-service/target/scala-2.11/case-service-assembly-0.6-SNAPSHOT.jar \
org.cafienne.service.Main

