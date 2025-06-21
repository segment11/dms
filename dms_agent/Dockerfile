# docker build -t dms_agent:1.2.0 .
# docker run -d --name dms_agent --cpu-period 1000000 --cpu-quota 250000 --net host -v /var/log/dms:/var/log/dms -v /opt/dms/config:/opt/dms/config -v /var/run/docker.sock:/var/run/docker.sock -v $pwd/conf.properties:/opt/dms_agent/conf.properties key232323/dms_agent:1.2.0
FROM eclipse-temurin:17-jre-jammy

WORKDIR /opt/dms_agent

VOLUME /var/run/docker.sock
VOLUME /var/log/dms
VOLUME /opt/dms/config
VOLUME /opt/dms_agent/conf.properties

ENV SERVER_HOST=192.168.1.1
ENV SERVER_PORT=5010
ENV CLUSTER_ID=1
ENV SECRET=1

COPY build/libs /opt/dms_agent

CMD java -Xms256M -Xmx256M -Djava.library.path=. -cp .:vendor/* -jar dms_agent-1.2.jar