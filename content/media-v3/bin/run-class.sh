#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ $# -lt 1 ];
then
  echo "USAGE: $0 [-daemon] [-name servicename] [-loggc] classname [opts]"
  exit 1
fi

export SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)

# create logs directory
if [ "x$LOG_DIR" = "x" ]; then
    LOG_DIR="/var/log/sengled"
fi

if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# classpath addition for release
for file in $SENGLED_APP_HOME/libs/*.jar;
do
  CLASSPATH=$CLASSPATH:$file
done


# JMX settings
if [  $JMX_PORT ]; then
  JMEDIA_JMX_OPTS="-Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT -Dcom.sun.management.jmxremote.host=127.0.0.1  -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
fi

# Log4j settings
if [ -z "$JMEDIA_LOG4J_OPTS" ]; then
  #JMEDIA_LOG4J_OPTS="-Dlog4j.configuration=file:$SENGLED_APP_HOME/config/log4j.properties"
  JMEDIA_LOG4J_OPTS="-Dlogback.configurationFile=file:$SENGLED_APP_HOME/config/logback.xml -Dlogging.config=file:$SENGLED_APP_HOME/config/logback.xml"
fi

# netty io.netty.leakDetection.maxRecords 的默认参数为 4， 会造成频繁的资源泄漏检查， 造成 CPU 过高
JMEDIA_OPTS=$JMEDIA_OPTS" -Dio.netty.allocator.type=pooled -Dio.netty.noPreferDirect=true  -Dio.netty.leakDetection.level=DISABLED -Dio.netty.leakDetection.maxRecords=128"


# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# Memory options
if [ -z "$JMEDIA_HEAP_OPTS" ]; then
  JMEDIA_HEAP_OPTS="-Xmx512M"
fi

# JVM performance options
if [ -z "$JMEDIA_JVM_PERFORMANCE_OPTS" ]; then
  JMEDIA_JVM_PERFORMANCE_OPTS="-server -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+CMSScavengeBeforeRemark -XX:+DisableExplicitGC -Djava.awt.headless=true"
fi


while [ $# -gt 0 ]; do
  COMMAND=$1
  case $COMMAND in
    -name)
      DAEMON_NAME=$2
      CONSOLE_OUTPUT_FILE=$LOG_DIR/$DAEMON_NAME.out
      shift 2
      ;;
    -loggc)
      if [ -z "$JMEDIA_GC_LOG_OPTS"] ; then
        GC_LOG_ENABLED="true"
      fi
      shift
      ;;
    -daemon)
      DAEMON_MODE="true"
      shift
      ;;
    *)
      break
      ;;
  esac
done

# GC options
GC_FILE_SUFFIX='-gc.log'
GC_LOG_FILE_NAME=''
if [ "x$GC_LOG_ENABLED" = "xtrue" ]; then
  GC_LOG_FILE_NAME=$DAEMON_NAME$GC_FILE_SUFFIX
  JMEDIA_GC_LOG_OPTS="-Xloggc:$LOG_DIR/$GC_LOG_FILE_NAME -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps "
fi

# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup $JAVA $JMEDIA_OPTS $JMEDIA_HEAP_OPTS $JMEDIA_JVM_PERFORMANCE_OPTS $JMEDIA_GC_LOG_OPTS $JMEDIA_JMX_OPTS $JMEDIA_LOG4J_OPTS -cp $CLASSPATH   "$@" >> "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
else
  exec $JAVA $JMEDIA_OPTS $JMEDIA_HEAP_OPTS $JMEDIA_JVM_PERFORMANCE_OPTS $JMEDIA_GC_LOG_OPTS $JMEDIA_JMX_OPTS $JMEDIA_LOG4J_OPTS  -cp $CLASSPATH   "$@"
fi
