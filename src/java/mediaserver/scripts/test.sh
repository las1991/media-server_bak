
workdir=/home/ec2-user
url_prefix=rtsp://172.31.22.236:554
to_url_prefix=rtsp://127.0.0.1:554
curl  "http://172.31.22.236:8888/monitor/getStreams?context=rtsp-server" >media.txt
data_array=`python py.py`
array_media=()
n=0
for i in $data_array
do
	array_media[n]=$i
	n=$((n+1))
done

len=${#array_media[@]}
run(){
	echo "--------------------------"
	for i in `seq 1 $1`
	do
	    echo "=============================="
	    n=$(($RANDOM%${len}))
	    pair_end1=$(($RANDOM%1000))
	    pair_end2=$(($RANDOM%1000))
	    a="${workdir}/ffmpeg  -rtsp_transport tcp -i ${url_prefix}${array_media[$n]}  -vcodec copy -acodec copy  -rtsp_transport tcp -f rtsp  ${to_url_prefix}${array_media[$n]}_from_${pair_end1}_${pair_end2}.sdp"
	    echo $a
	    $a &>/dev/null &

	done
}
begin_num=$1
echo "begin.....$begin_num"
date
run $begin_num
exit 0
for i in `seq 1 50`
do
    sleep 180
    echo "add.....50"
    date
    run 50
done
