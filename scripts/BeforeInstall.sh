#!/bin/sh

disk_size=`df | awk '{if ($6 == "/") print $4}'`
if [ "$disk_size" -lt 1048576 ];then
    exit 1
fi

awsflag=`curl -s http://169.254.169.254/latest/meta-data/services/domain | grep amazonaws.com|wc -l`
if [ $awsflag = 1 ]
then
    public_ip=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`
    private_ip=`curl http://169.254.169.254/latest/meta-data/local-ipv4 2>/dev/null`
else
    private_ip=`ip -4 -o addr show eth0 |awk '{print $4}' | sed 's/\/.*//' 2>/dev/null`
    $NO_NEED_PUBLIC_ACCESS
    if [ $? == 0 ]
    then
        echo "NO_NEED_PUBLIC_ACCESS is true switch[${NO_NEED_PUBLIC_ACCESS}]"
        public_ip=$private_ip
    else
        echo "NO_NEED_PUBLIC_ACCESS is false switch[${NO_NEED_PUBLIC_ACCESS}]"
        public_ip=`curl http://members.3322.org/dyndns/getip 2>/dev/null`
    fi
fi

basepath=$(cd `dirname $0`; pwd)
sed -i "s/\$PUBLIC_IPV4/${public_ip}/g" ${basepath}/../config/sengled.properties.ctmpl
sed -i "s/\$PRIVATE_IPV4/${private_ip}/g" ${basepath}/../config/sengled.properties.ctmpl

sed -i  "s/DEPLOY_GROUP_NAME/$DEPLOY_GROUP_NAME/g" ${basepath}/../config/sengled.properties.ctmpl
sed -i  "s/DEPLOY_GROUP_NAME/$DEPLOY_GROUP_NAME/g" ${basepath}/../config/haproxy.cfg.ctmpl


exit 0
