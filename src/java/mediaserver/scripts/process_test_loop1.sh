
#./node-v6.9.2-linux-x64/bin/node app_tcp_server_test.js $1 172.31.22.236 1554 &
sleep 2 
echo '-------------------------------------------'$i
currentTimeStamp=`echo $[$(date +%s%N)/1000000]`  
./ffmpeg -rtsp_transport tcp -i rtsp://172.31.22.236:554/662B69D43FA7224DC8ADB27C8F904F7A.sdp -vcodec copy -acodec copy -rtsp_transport tcp -f rtsp rtsp://127.0.0.1:$1/$currentTimeStamp.sdp &

sleep 1

loop_1=$currentTimeStamp

for i in `seq 1 $2`;
do
        #./node-v6.9.2-linux-x64/bin/node app_tcp_server_test.js $i 172.31.22.236 1554 &
        sleep 5 
        echo '-------------------------------------------'$i
        currentTimeStamp=`echo $[$(date +%s%N)/1000000]`  
        loop_2=$currentTimeStamp
        ./ffmpeg -rtsp_transport tcp -i rtsp://172.31.22.236:554/$loop_1.sdp -vcodec copy -acodec copy -rtsp_transport tcp -f rtsp rtsp://127.0.0.1:$1/$loop_2.sdp &
        loop_1=$loop_2

        sleep 1
done
