#!/bin/sh

private_ip=`curl http://169.254.169.254/latest/meta-data/local-ipv4 2>/dev/null`
if [ $? -ne 0 ];then
    private_ip=`ip -4 -o addr show eth0 |awk '{print $4}' | sed 's/\/.*//' 2>/dev/null`
fi

timeout=120
timeuse=0

while true
do
  ret=`curl -s -I http://${private_ip}:8888 |grep HTTP|awk '{print $2}'`
  if [[ $timeuse -le $timeout && $ret -eq 200 ]]
  then
      exit 0
  else
      if [[ $timeuse -ge $timeout ]]
      then
          exit 1
      fi
  fi
  sleep 5
  let timeuse+=5
  
done