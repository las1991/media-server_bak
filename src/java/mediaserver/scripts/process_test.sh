start=`echo $2| awk '{print int($0)}'`
end=`echo $2| awk '{print int($0)+50}'`
for i in `seq 1 $1` ;
do
    ./process_test_loop.sh $start $end &
    start=`echo $end| awk '{print int($0)+50}'`
    end=`echo $start| awk '{print int($0)+50}'`
done
