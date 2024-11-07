# docker build -t dms_agent:1.2.0 .
# docker run -d --name dms_agent --cpu-period 1000000 --cpu-quota 500000 --net host -v /opt/log:/opt/log -v /opt/dms/config:/opt/dms/config -v /var/run/docker.sock:/var/run/docker.sock -v $pwd/src/conf.properties:/opt/dms_agent/conf.properties dms_agent:1.2.0
from eclipse-temurin:17-jre-jammy

workdir /opt/dms_agent

volume /var/run/docker.sock
volume /opt/log
volume /opt/dms/config
volume /opt/dms_agent/conf.properties

env SERVER_HOST=127.0.0.1

copy build/libs /opt/dms_agent

cmd java -Xms256M -Xmx256M -Djava.library.path=. -cp . -jar dms_agent-1.2.jar