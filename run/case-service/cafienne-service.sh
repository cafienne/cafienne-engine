#!/bin/sh
# ----------------------------------------------------------------------------
#  Copyright 2014 - 2019 Cafienne B.V., https://cafienne.io

# ----------------------------------------------------------------------------
# Main Script for the Server
#
# Environment Variable Prequisites
#
#   CAFIENNE_SERVICE_HOME   Home of the installation. If not set I will try to figure it out.
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#
#   JAVA_OPTS       (Optional) Java runtime options used when the commands is executed.
#
# NOTE: Borrowed generously from Apache Tomcat startup scripts.
# -----------------------------------------------------------------------------

# Make this platform depended (MacOSX, Linux)
#JAVA_HOME=$(/usr/libexec/java_home -v 1.8)

linux=false;
darwin=false;
case "`uname`" in
Linux*) linux=true;;
Darwin*) darwin=true
		JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
	;;
esac



# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set CAFIENNE_SERVICE_HOME if not already set
[ -z "$CAFIENNE_SERVICE_HOME" ] && CAFIENNE_SERVICE_HOME=`cd "$PRGDIR/.." ; pwd`

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=java
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo " Cafienne service cannot execute $JAVACMD"
  exit 1
fi

# if JAVA_HOME is not set we're not happy
if [ -z "$JAVA_HOME" ]; then
  echo "You must set the JAVA_HOME variable before running the case service."
  exit 1
fi

if [ -e "$CAFIENNE_SERVICE_HOME/cafienne-service.pid" ]; then
  PID=`cat "$CAFIENNE_SERVICE_HOME"/cafienne-service.pid`
fi

# ----- Process the input command ----------------------------------------------
args=""
for c in $*
do
    if [ "$c" = "--stop" ] || [ "$c" = "-stop" ] || [ "$c" = "stop" ]; then
          CMD="stop"
    elif [ "$c" = "--start" ] || [ "$c" = "-start" ] || [ "$c" = "start" ]; then
          CMD="start"
    elif [ "$c" = "--version" ] || [ "$c" = "-version" ] || [ "$c" = "version" ]; then
          CMD="version"
    elif [ "$c" = "--restart" ] || [ "$c" = "-restart" ] || [ "$c" = "restart" ]; then
          CMD="restart"
    elif [ "$c" = "--console" ] || [ "$c" = "-console" ] || [ "$c" = "console" ]; then
          CMD="console"
    elif [ "$c" = "--docker" ] || [ "$c" = "-docker" ] || [ "$c" = "docker" ]; then
          CMD="docker"
    else
        args="$args $c"
    fi
done

if [ "$CMD" = "start" ]; then
  if [ -e "$CAFIENNE_SERVICE_HOME/cafienne-service.pid" ]; then
    if  ps -p $PID >&- ; then
      echo "Cafienne service is already running"
      exit 0
    fi
  fi
  export CAFIENNE_SERVICE_HOME=$CAFIENNE_SERVICE_HOME
# using nohup bash to avoid erros in solaris OS.TODO
  nohup bash $CAFIENNE_SERVICE_HOME/bin/cafienne-service.sh $args > $CAFIENNE_SERVICE_HOME/data/logs/cafienne-service.log 2>&1 &
  echo $! > $CAFIENNE_SERVICE_HOME/cafienne-service.pid
  exit 0
elif [ "$CMD" = "stop" ]; then
  export CAFIENNE_SERVICE_HOME=$CAFIENNE_SERVICE_HOME
  kill -term `cat $CAFIENNE_SERVICE_HOME/cafienne-service.pid`
  rm $CAFIENNE_SERVICE_HOME/cafienne-service.pid
  exit 0
elif [ "$CMD" = "restart" ]; then
  export CAFIENNE_SERVICE_HOME=$CAFIENNE_SERVICE_HOME
  kill -term `cat $CAFIENNE_SERVICE_HOME/cafienne-service.pid`
  process_status=0
  pid=`cat $CAFIENNE_SERVICE_HOME/cafienne-service.pid`
  while [ "$process_status" -eq "0" ]
  do
        sleep 1;
        ps -p$pid 2>&1 > /dev/null
        process_status=$?
  done

# using nohup bash to avoid erros in solaris OS.TODO
  nohup bash $CAFIENNE_SERVICE_HOME/bin/cafienne-service.sh $args > $CAFIENNE_SERVICE_HOME/data/logs/cafienne-service.log 2>&1 &
  echo $! > $CAFIENNE_SERVICE_HOME/cafienne-service.pid
  exit 0
elif [ "$CMD" = "console" ]; then
    JAVACMD="$JAVACMD" > $CAFIENNE_SERVICE_HOME/data/logs/cafienne-service.log 2>&1
elif [ "$CMD" = "docker" ]; then
    # Wait for elasticsearch to be up and running before starting the service
    response=""
    while [ "$response" = "" ]
    do
        response=$(curl -s http://elastic:9200/_cluster/health)
        if [[ ! "$response" =~ "red" ]]; then
            >&2 echo "ElasticSearch container is up"
        elif [[ ! "$response" == "" ]]; then
            >&2 echo "ElasticSearch container is not running yet"
        fi
        sleep 2
    done

    JAVACMD="$JAVACMD" > $CAFIENNE_SERVICE_HOME/data/logs/cafienne-service.log 2>&1
elif [ "$CMD" = "version" ]; then
  cat $CAFIENNE_SERVICE_HOME/bin/version.txt
  exit 0
fi

# ---------- Handle the SSL Issue with proper JDK version --------------------
jdk_18=`$JAVA_HOME/bin/java -version 2>&1 | grep "1.[8]"`
if [ "$jdk_18" = "" ]; then
   echo " [ERROR] Service is supported only on JDK 1.8"
   exit 1
fi

# ---------- Set Classpath ------------------------------------------

CAFIENNE_SERVICE_CLASSPATH=""

for f in "$CAFIENNE_SERVICE_HOME"/assembly/*.jar
do
    if [ "$f" != "$CAFIENNE_SERVICE_HOME/assembly/*.jar" ];then
        CAFIENNE_SERVICE_CLASSPATH="$CAFIENNE_SERVICE_CLASSPATH":$f
    fi
done
#for t in "$CAFIENNE_SERVICE_HOME"/lib/*.jar
#do
#    CAFIENNE_SERVICE_CLASSPATH="$CAFIENNE_SERVICE_CLASSPATH":$t
#done

#for z in "$CAFIENNE_SERVICE_HOME"/deploy/*.zip
#do
#   CAFIENNE_SERVICE_CLASSPATH="$CAFIENNE_SERVICE_CLASSPATH":$z
#done

# ----- Execute The Requested Command -----------------------------------------

echo JAVA_HOME environment variable is set to $JAVA_HOME
echo CAFIENNE_SERVICE_HOME environment variable is set to $CAFIENNE_SERVICE_HOME
echo CAFIENNE_SERVICE_CONF environment variable is set to $CAFIENNE_SERVICE_CONF

cd "$CAFIENNE_SERVICE_HOME"

START_EXIT_STATUS=121
status=$START_EXIT_STATUS

#To monitor a Carbon server in remote JMX mode on linux host machines, set the below system property.
#   -Djava.rmi.server.hostname="your.IP.goes.here"

# Wait for Elastic to be up and running

while [ "$status" = "$START_EXIT_STATUS" ]
do
    exec $JAVACMD \
    -Dcom.sun.management.jmxremote.port=9999 \
    -Djava.net.preferIPv4Stack=true \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dfile.encoding=UTF-8 \
    -Ddeploy.dir="$CAFIENNE_SERVICE_HOME/data/definitions" \
    -Dconfig.file="$CAFIENNE_SERVICE_CONF/application.conf" \
    -XX:+CMSClassUnloadingEnabled \
    -Xms256m -Xmx1024m -Djava.awt.headless=true \
    -classpath "$CAFIENNE_SERVICE_CLASSPATH" \
    org.cafienne.service.Main $*
    status=$?
done

