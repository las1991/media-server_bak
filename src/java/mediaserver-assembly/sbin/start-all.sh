if [ $# -lt 1 ];
then
        echo "USAGE: $0 [-daemon] [-loggc] [-name DAEMON_NAME]"
        exit 1
fi

xmx="1024M"
xms="1024M"
MaxDirectMemorySize="1024M"

###################### call run-class.sh   ######################
SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)
SPRING_CONFIG_LOCATION="file:$SENGLED_APP_HOME/config/application.properties"
HAPROXY_CONFIG_LOCATION="file:$SENGLED_APP_HOME/config/haproxy.properties"
STORAGE_CONFIG_LOCATION="file:$SENGLED_APP_HOME/config/storage.properties"
KINESIS_ALGORITHM_LOCATION="file:$SENGLED_APP_HOME/config/kinesis-producer.properties"


export JMEDIA_OPTS=" -Dserver.type=RtspServer -Dspring.config.location=$SPRING_CONFIG_LOCATION,$HAPROXY_CONFIG_LOCATION,$STORAGE_CONFIG_LOCATION,file:/etc/sengled/sengled.properties -Dkinesis.config.algorithm.location=$KINESIS_ALGORITHM_LOCATION "
export JMEDIA_HEAP_OPTS=" -Xmx${xmx} -Xms${xms} -XX:MaxDirectMemorySize=${MaxDirectMemorySize}"
export JMEDIA_JVM_PERFORMANCE_OPTS=" -server -XX:SurvivorRatio=8 -XX:+UseParallelGC -XX:+UseParallelOldGC  -XX:+UseAdaptiveSizePolicy -XX:+PrintAdaptiveSizePolicy -XX:MaxGCPauseMillis=50 -Djava.awt.headless=true "
export JMEDIA_NETTY_OPTS=" -Dio.netty.allocator.type=pooled -Dio.netty.noPreferDirect=false -Dio.netty.leakDetection.level=DISABLED"

EXTRA_ARGS="-name media-server -loggc "
COMMAND=$1
case $COMMAND in
  -daemon)
    EXTRA_ARGS="-daemon "$EXTRA_ARGS
    shift
    ;;
  *)
    ;;
esac

exec $SENGLED_APP_HOME/bin/run-class.sh $EXTRA_ARGS com.sengled.cloud.media.MediaServer all
