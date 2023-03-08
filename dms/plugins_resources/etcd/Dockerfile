# docker build -t key232323/etcd:3.4.24 .
from frolvlad/alpine-glibc

#RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
#RUN apk update && apk upgrade

RUN apk --no-cache add bash busybox busybox-extras curl
RUN rm -rf /tmp/* /var/cache/apk/*

ENV ETCD_VERSION=v3.4.24

RUN curl -L https://github.com/etcd-io/etcd/releases/download/${ETCD_VERSION}/etcd-${ETCD_VERSION}-linux-amd64.tar.gz -o /etcd.tar.gz && \
    tar xzvf etcd.tar.gz && \
    rm etcd.tar.gz && \
    mv etcd-${ETCD_VERSION}-linux-amd64 etcd

expose 2379
expose 2380

workdir /etcd
cmd /etcd/etcd --config-file /etcd/etcd.yml