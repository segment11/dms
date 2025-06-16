# docker run -d --name dms_server --net=host -v /opt/log:/opt/log -v /data/dms:/data/dms -v /opt/dms/conf.properties:/opt/dms/conf.properties -e ADMIN_PASSWORD=123456 -e LOCAL_IP_FILTER_PRE=192. key232323/dms_server:1.2.0
# nohup java -Xms128m -Xmx256m -Djava.library.path=. -cp . -jar dms_agent-1.2.jar > /dev/null 2>&1 &
FROM eclipse-temurin:17-jre-alpine
# docker build -t key232323/dms .

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories
RUN apk update && apk upgrade

RUN apk --no-cache add bash busybox busybox-extras curl
RUN rm -rf /tmp/* /var/cache/apk/*

COPY build/libs /opt/dms
WORKDIR /opt/dms

EXPOSE 5010
EXPOSE 5011

VOLUME /opt/log
VOLUME /data/dms

ENV ADMIN_PASSWORD=123456
ENV DB_DATA_FILE="/data/dms/db;FILE_LOCK=SOCKET"
ENV LOCAL_IP_FILTER_PRE=192.

CMD java -Xms2048M -Xmx2048M -cp . -jar dms_server-1.2.jar