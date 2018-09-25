#!/bin/sh

ps -ef|grep consul-template | grep -v grep | grep sengled.properties.ctmpl | awk '{print $2}' | xargs kill -9 >/dev/null 2>&1

basepath=$(cd `dirname $0`; pwd)
${basepath}/../content/media-v3/bin/stop-all.sh


exit 0
