#start=`echo $2| awk '{print int($0)}'`
port=`echo $1| awk '{print int($0)}'`


./node-v6.9.2-linux-x64/bin/node app_tcp_server_test.js $1 172.31.22.236 1554 &
#for i in `seq 1 $2` ;
#do
#    ./process_test_loop1.sh $port 50 &
#done
sleep 2
./process_test_loop1.sh $port $2 &
