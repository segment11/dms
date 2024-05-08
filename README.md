# dms
A docker instances manage system like k8s write in java/groovy, including web ui.

# features

- docker instance management
- host machine process management
- web ui
- work node init
- application configuration files are generated by groovy template
- stateful application support
- hpa
- A/B tests
- traefik http gateway like k8s ingress
- metrics collect by prometheus, auto reload jobs like k8s service monitor
- log collect by vector and open observe
- plugins support like k8s operator
- multi-region worker node support by underlay network

# architecture

## dms server agent overview
![dms server agent overview](./pic/arch/dms-server-agent.png)

## dms server agent modules
![dms server agent modules](./pic/arch/dms-server-agent-module.png)

## create container/process steps
![create container/process steps](./pic/arch/dms-create-container.png)

## dms build-in plugins
![dms build-in plugins](./pic/arch/dms-plugin-build-in.png)

# run dms server

- docker run -v /opt/log:/opt/log -v /data/dms:/data/dms --name=dms -d --net=host key232323/dms
- open http://your-ip:5010/admin/login.html user/password -> admin/abc

# or run dms server by compiling from source

TIPS: Need jdk17+/gradle7+

- cd ~/ws
- git clone git@github.com:segment11/dms.git
- cd ~/ws/dms/dms_agent
- gradle tar
- cd ~/ws/dms/dms
- gradle buildToRun
- cd ~/ws/dms/dms/build/libs & java -cp . -jar dms_server-1.2.jar
- open http://your-ip:5010/admin/login.html user/password -> admin/abc

# run dms agent

TIPS: Need jdk17+

- cd ~/ws/dms/dms_agent/build/libs
- vi conf.properties

```properties
# change to your dms server ip
serverHost=192.168.1.1
# change to your host ip prefix
localIpFilterPre=192.
# there is cluster demo cluster with id=1 and secret=1
clusterId=1
secret=1
```

- java -Djava.library.path=. -cp . -jar dms_agent-1.2.jar

### TIPS:
run 'java -Djava.library.path=. -cp . -jar dms_agent-1.2.jar' on another node, will add this node as a work node to target dms cluster.


# screenshots

- cluster overview

![cluster overview](./pic/cluster_overview.PNG)

- cluster container overview by node ip

![cluster container overview](./pic/cluster_container_overview.PNG)

- worker node init

![node init deploy](./pic/node_init_deploy.PNG)

- node cpu stats 

![node chart](./pic/node_chart.png)

- application list

![application list](./pic/application_list.PNG)

- one application container list

![application one](./pic/application_one.PNG)

- one application event list

![application event list](./pic/application_event_list.png)

- job steps log

![job steps](./pic/job_steps.png)

# author contact

- wechat: key232323
- email: dingyong87@163.com
