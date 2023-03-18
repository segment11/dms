from python:3.10.8-alpine3.15

RUN apk --no-cache add bash busybox busybox-extras curl
RUN apk --no-cache add gcc libpq-dev libffi-dev musl-dev linux-headers
RUN apk --no-cache add postgresql
RUN rm -rf /tmp/* /var/cache/apk/*

ENV PG_HOME=/usr/libexec/postgresql

RUN pip install psycopg2
RUN pip install patroni[etcd]

RUN echo -e 'repl@pass\nrepl@pass'|adduser repl -G root
run chown -R repl $PG_HOME
USER repl

#CMD ["sh", "-c", "patroni /etc/patroni.yml"]
#ENTRYPOINT ["patroni", "/etc/patroni.yml"]
