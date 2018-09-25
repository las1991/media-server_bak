
./node-v6.9.2-linux-x64/bin/node app_tcp_server_test.js $1 172.31.22.236 1554 &
sleep 2 
echo '-------------------------------------------'$i
current=`date "+%Y-%m-%d %H:%M:%S"`  
timeStamp=`date -d "$current" +%s`   
currentTimeStamp=$((timeStamp*1000+`date "+%N"`/1000000)) #将current转换为时间戳，精确到毫秒  
echo $currentTimeStamp 
./ffmpeg -rtsp_transport tcp -i rtsp://172.31.22.236:554/662B69D43FA7224DC8ADB27C8F904F7A.sdp -vcodec copy -acodec copy -rtsp_transport tcp -f rtsp rtsp://127.0.0.1:$1/$currentTimeStamp.sdp &

sleep 1

start=`echo $1| awk '{print int($0)+1}'`
end=`echo $2| awk '{print int($0)}'`
loop_1=$currentTimeStamp
for i in `seq $start $end`;
do
    ./node-v6.9.2-linux-x64/bin/node app_tcp_server_test.js $i 172.31.22.236 1554 &
    sleep 7 
    echo '-------------------------------------------'$i
    current=`date "+%Y-%m-%d %H:%M:%S"`  
    timeStamp=`date -d "$current" +%s`   
    currentTimeStamp=$((timeStamp*1000+`date "+%N"`/1000000)) #将current转换为时间戳，精确到毫秒 
    loop_2=$currentTimeStamp
    echo $currentTimeStamp 
    ./ffmpeg -rtsp_transport tcp -i rtsp://172.31.22.236:554/$loop_1.sdp -vcodec copy -acodec copy -rtsp_transport tcp -f rtsp rtsp://127.0.0.1:$i/$loop_2.sdp &
    loop_1=$loop_2
    sleep 1
done
