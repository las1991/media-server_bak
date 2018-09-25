#!/bin/sh
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

source /etc/profile
SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)


# 1, call api do soft stop
if [[ $(jps | grep MediaServer | wc -l) -gt 0 ]];then
  curl -X POST   http://127.0.0.1:8888/media/stop -H 'content-type: application/json' -H 'user: chenxh'
  echo  -e ""
  echo -n "media-server-java remove connections with SNAP."
  for i in {1..8}
  do
    echo -n "."
    sleep 1
  done
  echo  -e ""
fi

# 2, wait TCP recovered
echo -n "media-server-java waiting for TCP connection to be recovered."
for i in {1..60}
do
  if [[ $(netstat  -lnpta | grep 1554 | grep WAIT | wc -l) -gt 0 ]];then
    echo -n "."
    sleep 1
  else
    echo  -e ""
    break
  fi
done



# 3, kill process
echo -n "Kill MediaServer"
jps | grep MediaServer | awk '{print $1}' | xargs kill -SIGTERM

# 4, wait kill
echo -e ""
echo -n "Killing ."
for i in {1..60}
do
  if [[ $(netstat  -lnpta | grep 1554 | wc -l) -gt 0 ]];then
    echo -n "."
    sleep 1
  elif [[ $(jps | grep MediaServer | wc -l) -gt 0 ]];then
    echo -n "."
    sleep 1
  else
    echo  -e ""
    break
  fi
done

echo  -e "BYE"