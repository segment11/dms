# docker run -d --name dms_server --net=host -v /opt/log:/opt/log -v /data/dms:/data/dms -v /opt/dms/conf.properties:/opt/dms/conf.properties -e ADMIN_PASSWORD=123456 -e LOCAL_IP_FILTER_PRE=192. key232323/dms_server:1.2.0
# nohup java -Xms128m -Xmx256m -Djava.library.path=. -cp . -jar dms_agent-1.2.jar > dmc.log 2>&1 &
from eclipse-temurin:17-jre-alpine
# docker build -t key232323/dms .

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
RUN apk update && apk upgrade

RUN apk --no-cache add bash busybox busybox-extras curl
RUN rm -rf /tmp/* /var/cache/apk/*

copy build/libs /opt/dms
workdir /opt/dms

expose 5010
expose 5011

volume /opt/log
volume /data/dms

env ADMIN_PASSWORD=123456
env DB_DATA_FILE=/data/dms/db;FILE_LOCK=SOCKET
env LOCAL_IP_FILTER_PRE=192.

cmd java -Xms512M -Xmx512M -cp . -jar dms_server-1.2.jar