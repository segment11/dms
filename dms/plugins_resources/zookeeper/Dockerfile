# docker build -t key232323/zookeeper:3.6.4 .
from adoptopenjdk/openjdk8:alpine-slim

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
RUN apk update && apk upgrade

RUN apk --no-cache add bash busybox busybox-extras curl
RUN rm -rf /tmp/* /var/cache/apk/*

ENV VERSION 3.6.4

RUN curl https://dlcdn.apache.org/zookeeper/zookeeper-${VERSION}/apache-zookeeper-${VERSION}-bin.tar.gz -o zookeeper.tar.gz && \
    tar xzf zookeeper.tar.gz && \
    rm zookeeper.tar.gz && \
    mv apache-zookeeper-${VERSION}-bin zookeeper

expose 2181
expose 2888
expose 3888
expose 7000

workdir /zookeeper
cmd bin/zkServer.sh start-foreground zoo.cfg
