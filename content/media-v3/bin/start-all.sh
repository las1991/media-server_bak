if [ $# -lt 1 ];
then
        echo "USAGE: $0 [-daemon] [-loggc] [-name DAEMON_NAME]"
        exit 1
fi

SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)
SPRING_CONFIG_LOCATION="file:$SENGLED_APP_HOME/config/application.properties"


export JMEDIA_HEAP_OPTS="-Xmx2G -Xms512M"
export JMEDIA_OPTS=" -Dserver.name=MediaAndTalkbackServer -Dserver.type=RtspServer"
export JMEDIA_OPTS=$JMEDIA_OPTS" -Dspring.config.location=$SPRING_CONFIG_LOCATION,file:/etc/sengled/sengled.properties"


EXTRA_ARGS="-name media-server-java -loggc "
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
