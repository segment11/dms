# docker run -d --name dms_server --net=host -v /var/log/dms:/var/log/dms -v /var/lib/dms:/var/lib/dms -v /opt/dms/conf.properties:/opt/dms/conf.properties -e ADMIN_PASSWORD=123456 -e LOCAL_IP_FILTER_PRE=192. key232323/dms_server:1.2.1
# nohup java -Xms128m -Xmx256m -Djava.library.path=. -cp . -jar dms_agent-1.2.jar > /dev/null 2>&1 &
FROM docker.1ms.run/library/eclipse-temurin:21-jre
# docker build -t key232323/dms_server:1.2.1 .

COPY build/libs /opt/dms
WORKDIR /opt/dms

EXPOSE 5010
EXPOSE 5011

VOLUME /var/log/dms
VOLUME /var/lib/dms

ENV ADMIN_PASSWORD=123456
ENV DB_DATA_FILE="/var/lib/dms/db;FILE_LOCK=SOCKET"
ENV LOCAL_IP_FILTER_PRE=192.

CMD java -Xms2048M -Xmx2048M -cp . -jar dms_server-1.2.jar