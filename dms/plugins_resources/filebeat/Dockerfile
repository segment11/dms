# docker build -t key232323/filebeat:6.1.2 .
from frolvlad/alpine-glibc

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
RUN apk update && apk upgrade

RUN apk --no-cache add bash busybox busybox-extras curl
RUN rm -rf /tmp/* /var/cache/apk/*

ENV FILEBEAT_VERSION=6.1.2

RUN curl https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-${FILEBEAT_VERSION}-linux-x86_64.tar.gz -o /filebeat.tar.gz && \
    tar xzvf filebeat.tar.gz && \
    rm filebeat.tar.gz && \
    mv filebeat-${FILEBEAT_VERSION}-linux-x86_64 filebeat

run mkdir /filebeat/conf.d
workdir /filebeat
cmd /filebeat/filebeat -e -c /filebeat/filebeat.yml