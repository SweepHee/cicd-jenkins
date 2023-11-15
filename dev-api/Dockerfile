FROM openjdk:17-jdk
ARG JAR_FILE=dev-api-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} /dev-api/dev-api-1.0-SNAPSHOT.jar

ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime

# Docker 컨터이너 안에 서버가 정상 동작인지 체크
HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:9001/api/actuator/health || exit 1

WORKDIR /dev-api
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "dev-api-1.0-SNAPSHOT.jar","--spring.profiles.active=${PROFILE}"]

