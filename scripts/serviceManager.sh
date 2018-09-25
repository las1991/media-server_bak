#!/bin/sh
if [ $# != 2 ];then
    echo "USAGE: $0 service_name action"
    exit 0
fi
echo $1
echo $2
curl http://127.0.0.1:5000/service/$1?action=$2

exit 0