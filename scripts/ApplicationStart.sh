#!/bin/sh

basepath=$(cd `dirname $0`; pwd)
ps -ef|grep consul-template | grep -v grep | grep sengled.properties.ctmpl >/dev/null 2>&1
if [ $? -ne 0 ];then
    consul-template -consul 127.0.0.1:8500 -template "/etc/sengled/sengled.properties.ctmpl:/etc/sengled/sengled.properties:/opt/sengled/apps/scripts/serviceManager.sh media-v3 reload || true" >/dev/null 2>&1 &
fi

int=1
while(( $int<=5 ))
do
    if [ -f /etc/sengled/sengled.properties ];then
        break
    fi
    let "int++"
    sleep 1
done

${basepath}/../content/media-v3/bin/start-all.sh -daemon


exit 0
