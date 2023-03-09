from adoptopenjdk/openjdk8:alpine-slim

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

cmd java -cp . -jar dms_server-1.1.jar