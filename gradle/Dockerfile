FROM openjdk:8u151-jre-alpine3.7
MAINTAINER Maksim Kostromin https://github.com/daggerok
RUN apk --no-cache --update add busybox-suid bash curl unzip sudo openssh-client shadow wget \
 && adduser -h /home/appuser -s /bin/bash -D -u 1025 appuser wheel \
 && echo "appuser ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers \
 && sed -i "s/.*requiretty$/Defaults !requiretty/" /etc/sudoers \
 && wget --no-cookies \
         --no-check-certificate \
         --header "Cookie: oraclelicense=accept-securebackup-cookie" \
                  "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip" \
         -O /tmp/jce_policy-8.zip \
 && unzip -o /tmp/jce_policy-8.zip -d /tmp \
 && mv -f ${JAVA_HOME}/lib/security ${JAVA_HOME}/lib/backup-security \
 && mv -f /tmp/UnlimitedJCEPolicyJDK8 ${JAVA_HOME}/lib/security \
 && apk del busybox-suid unzip openssh-client shadow wget \
 && rm -rf /var/cache/apk/* /tmp/*
USER appuser
WORKDIR /home/appuser
VOLUME /home/appuser
ARG JAVA_OPTS_ARGS="\
 -Djava.net.preferIPv4Stack=true \
 -XX:+UnlockExperimentalVMOptions \
 -XX:+UseCGroupMemoryLimitForHeap \
 -XshowSettings:vm "
ARG ZOOKEEPER_PORT_ARG="2181"
ARG ZOOKEEPER_DIR_ARG=/home/appuser
ARG KAFKA_PORT_ARG="9092"
ARG KAFKA_TOPICS_ARG="\
topic1,topic2,topic3"
ARG HTTP_PORT_ARG="8080"
ARG HTTP_CONTEXT_ARG="/"
ENV JAVA_OPTS="${JAVA_OPTS} ${JAVA_OPTS_ARGS}" \
    ZOOKEEPER_PORT="${ZOOKEEPER_PORT_ARG}" \
    ZOOKEEPER_DIR="${ZOOKEEPER_DIR_ARG}" \
    KAFKA_PORT="${KAFKA_PORT_ARG}" \
    KAFKA_TOPICS="${KAFKA_TOPICS_ARG}" \
    HTTP_PORT="${HTTP_PORT_ARG}" \
    HTTP_CONTEXT="${HTTP_CONTEXT_ARG}"
ENTRYPOINT java ${JAVA_OPTS} -jar ./app.jar \
                --zookeeperPort="${ZOOKEEPER_PORT}" \
                --zookeeperDir="${ZOOKEEPER_DIR}" \
                --kafkaPort="${KAFKA_PORT}" \
                --kafkaTopics="${KAFKA_TOPICS}" \
                --httpPort="${HTTP_PORT}" \
                --httpContext="${HTTP_CONTEXT}"
CMD /bin/bash
EXPOSE ${HTTP_PORT} ${KAFKA_PORT} ${ZOOKEEPER_PORT}
HEALTHCHECK --timeout=2s \
            --retries=22 \
            CMD curl -f "http://127.0.0.1:${HTTP_PORT}${HTTP_CONTEXT}" || exit 1
COPY --chown=appuser ./build/libs/*-all.jar ./app.jar
