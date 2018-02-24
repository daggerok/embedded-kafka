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
ARG KAFKA_TOPICS_ARG="\
topic1,topic2,topic3"
ENV KAFKA_TOPICS="${KAFLA_TOPICS_ARG}"
ARG ZOOKEEPER_DIR_ARG=/home/appuser
ENV ZOOKEEPER_DIR="${ZOOKEEPER_DIR_ARG}"
ARG JAVA_OPTS_ARGS="\
 -Djava.net.preferIPv4Stack=true \
 -XX:+UnlockExperimentalVMOptions \
 -XX:+UseCGroupMemoryLimitForHeap \
 -XshowSettings:vm "
ENV JAVA_OPTS="${JAVA_OPTS} ${JAVA_OPTS_ARGS}"
ENTRYPOINT java ${JAVA_OPTS} -jar ./app.jar \
                --kafkaTopics="${KAFKA_TOPICS}" \
                --zookeeperDir="${ZOOKEEPER_DIR}"
CMD /bin/bash
EXPOSE 8080
HEALTHCHECK --timeout=2s \
            --retries=22 \
            CMD curl -f http://127.0.0.1:8080/actuator/health || exit 1
COPY --chown=appuser ./build/libs/*-all.jar ./app.jar
