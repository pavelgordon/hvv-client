FROM oracle/graalvm-ce:19.2.1
VOLUME /tmp
COPY build/libs/hvv-client-0.0.1-SNAPSHOT.jar hvv-client.jar
EXPOSE 8080
