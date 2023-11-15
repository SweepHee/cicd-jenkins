# 젠킨스-도커 무중단 배포구현 (With NGINX)

<p align="center">
<img src="https://img.shields.io/badge/Dockerfile-blue?logo=docker&logoColor=white">
<img src="https://img.shields.io/badge/Jenkins-black?logo=jenkins&logoColor=white">
<img src="https://img.shields.io/badge/Nginx-green?logo=nginx">
</p>

## 소개

도커와 NGINX를 이용한 무중단 배포를 구현합니다. 
이 저장소는 젠킨스를 이용할것이나 shell script를 주류로 작성하므로 github actions를 사용해도 무방합니다.
사실상 완벽한 무중단 배포는 아니며 다운타임이 0.1초 아래로 예측됩니다

### 서버
- dev API 서버 (9001번 포트)
- dev 콜백 서버 (9002번 포트)


### 방법
1. 2개의 실운영 컨테이너와 2개의 스탠바이 컨테이너를 준비합니다 (9003,9004,9005,9006 포트사용)
2. nginx에서 9001번으로 요청이오면 9003 또는 9004로 연결되게끔 설정, 9002번으로 요청이오면 9005 또는 9006번으로 연결되게끔 설정
3. 젠킨스로 배포를 할때 스탠바이 컨테이너에 배포가 되고 스탠바이 포트로 설정이 변경되도록 자동화 스크립트를 작성


## 사전 작업

- openjdk version 17 이상 설치
- redis "7 Major Release" 이상 설치
- postgresql "15 Major Release" 설치
- docker version 20 이상 설치
- docker compose version 2 이상설치
- 젠킨스에 메인 파이프라인 등록 (이 과정은 생략합니다)

## 순서
- [Jenkinsfile작성](#Jenkinsfile작성)
- [shell script 작성](#shell-script 작성)
- [nginx 설정 작성](#nginx)
- [Dockerfile 작성](#Dockerfile)

## Jenkinsfile작성

* [젠킨스파일 링크](http://test.com) --- FIXME
---
### 최상단
```yaml
pipeline {
  // 쉘스크립트로 처리할 것이므로 any를 줬습니다. 
  //dockerfile로 설정해서 스크립트 명령어를 도커파일에서 실행하는 방법도 무방
  agent any
  
  // stages 시작
  stages {
    // 시작부에 빌드를 시작했다는 알림을 준다. 여기서는 슬랙에 시작 메세지를 보냄
    stage('Start') {
        steps {
          slackSend (channel: '#cicd', color: '#6DDFE9', message: "START: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
  ...
  ...
  }
}
```
### CI/CD - build, transfer
```yaml
/* main branch CI */
stage('build') {
    when {
        branch 'main'
    }
    steps {
        sh 'chmod +x gradlew'
        sh './gradlew clean'
        sh './gradlew dev-api:bootJar'
    }
}


stage('scp build file') {
  when {
    branch 'main'
  }
  steps([$class: 'BapSshPromotionPublisherPlugin']) {
     sshPublisher(
         continueOnError: false,
         failOnError: true,
         publishers: [
           sshPublisherDesc(
           configName: "dev", // 젠킨스 시스템 정보에 사전 입력한 서버 ID
           verbose: true,
           transfers: [
             sshTransfer(
                 sourceFiles: "dev-api/build/libs/*", // 전송할 파일
                 removePrefix: "dev-api/build/libs", // 파일에서 삭제할 경로
                 remoteDirectory: "/dev-api", // 배포할 위치
              )  
            ]
          )
        ]  
     )
  } 
}
```
### CI/CD - docker build, container switching

```yaml
// Dockerfile build
stage('Docker build') {
    when {
       branch 'main'
    }
    steps {
        sshPublisher(
            continueOnError: false, failOnError: true,
            publishers: [
                sshPublisherDesc(
                configName: "dev",
                    verbose: true,
                    transfers: [
                        sshTransfer(
                            execCommand: "cd dev-api && pwd && docker build --tag 도커허브아이디/dev-api ." // 배포경로의 Dockerfile을 빌드해준다
                        )
                    ]
                )
            ] 
        )
    }
}

// nginx 포트포워딩 설정을 변경하는 shell을 실행
stage('Switch docker container') {
  when {
    branch 'main'
  }
  steps {
    sshPublisher(
        continueOnError: false, failOnError: true,
        publishers: [
          sshPublisherDesc(
              configName: "dev",
              verbose: true,
              transfers: [
                sshTransfer(
                  execCommand: "cd dev-api && chmod +x dev-api-deploy.sh && ./dev-api-deploy.sh" // 서버에서 실행할 커맨드
                )
            ]
          )
        ]
    )
  }
}

```
### Post - 완료 알림
```yaml
post {
    success {
        slackSend (channel: '#cicd', color: '#6DDFE9', message: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
    failure {
        slackSend (channel: '#cicd', color: '#EC88DF', message: "FAIL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
}
```
---

## shell-script 작성
- 반드시 `dev-api-deploy.sh` 로 만들어줘야 한다 (jenkinsfile에서 해당 이름으로 작성) 
```yaml
# 현재 열려있는 api서버 컨테이너 포트가 9003, 9004인지 확인
OPEN_PORT=$(docker ps --format "{{.Names}}" | awk '/dev-api/ {print $1}' | sed 's/dev-api-//')

# 현재 포트번호
RUNNING_PORT=-1
# 바꿀 포트번호
IDLE_PORT=9003

# 현재 열려있는 포트 -> 바꿀 포트를 찾아서 저장한다 
if [[ $OPEN_PORT == '9003' ]]; then
  RUNNING_PORT=9003
  IDLE_PORT=9004
elif [[ $OPEN_PORT == '9004' ]]; then
  RUNNING_PORT=9004
fi

# Jenkinsfile에서 빌드한 이미지로 컨테이너를 생성&실행한다 (로그 남기기용 nohup으로 돌리기)
nohup docker run --name dev-api-$IDLE_PORT -e PROFILE=ops -p $IDLE_PORT:9001 --add-host host.docker.internal:host-gateway 도커허브아이디/dev-api >> nohup.out 2>&1 &

# 컨테이너 실행이 되기까지 10초 간격으로 반복문을 돌면서 정상 실행이 완료되었다면 break
for cnt in {1..10}
do
    echo "$IDLE_PORT 서버 응답 확인중(${cnt}/10)";
    UP=$(curl -s http://localhost:${IDLE_PORT}/api/actuator/health)
    if [ -z "${UP}" ]
        then
            sleep 10
            continue
        else
            break
    fi
done

if [ $cnt -eq 10 ]
then
    echo "$IDLE_PORT 서버가 정상적으로 구동되지 않았습니다."
    exit 1
fi


if [ $RUNNING_PORT -ne -1 ]
then
  echo "> 중지 포트 : $RUNNING_PORT"
  echo "> 실행 포트 : $IDLE_PORT"

  # 해당 경로에 미리 dev-api-service-url.inc 파일을 만들어야 한다. Nginx 파트에서 설명
  # 현재 돌아가는 포트를 바꿀 포트로 치환처리 Ex) 9003 -> 9004
  sudo sed -i "s/${RUNNING_PORT}/${IDLE_PORT}/" /etc/nginx/conf.d/dev-api-service-url.inc
  
  # reload를 해야 다운타임이 적다
  sudo systemctl reload nginx
  echo "==========deploy completed==========="
  
  # 기존에 돌고있던 container는 삭제한다
  docker stop dev-api-$RUNNING_PORT > /dev/null
  docker rm dev-api-$RUNNING_PORT > /dev/null
else
  echo "> 실행 포트 : $IDLE_PORT"
fi

```

## nginx 
- 환경변수 파일 `/etc/nginx/conf.d/dev-api-service-url.inc` 만들기
```text
// nginx에서는 9001로 들어온 요청을 $dev_api_service_url로 포트포워딩 처리를 해둬야 한다
// 따라서 9004 / 9005로 배포될 때마다 스위칭 될 것
set $dev_api_service_url http://127.0.0.1:9004;
```
- nginx.conf
```text
upstream dev-api {
	least_conn;
	server localhost:9003 weight=5 max_fails=3 fail_timeout=10s;
	server localhost:9004 weight=5 max_fails=3 fail_timeout=10s;
}

server {
    listen 9001;
    listen [::]:9001;
    server_name localhost;

    error_log /var/log/nginx/dev-api-error.log;
    access_log /var/log/nginx/dev-api-access.log;

    # 환경변수 파일 등록
    include /etc/nginx/conf.d/dev-api-service-url.inc;

    location / {
        # 바뀔 포트주소 변수로 등록
	    proxy_pass $dev_api_service_url;
	    proxy_set_header X-Real-IP $remote_addr;
	    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	    proxy_set_header Host $http_host;
    }
}
```

## Dockerfile
```yaml
FROM openjdk:17-jdk
ARG JAR_FILE=dev-api-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} /dev-api/dev-api-1.0-SNAPSHOT.jar

ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime

HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:9001/api/actuator/health || exit 1

WORKDIR /dev-api
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "dev-api-1.0-SNAPSHOT.jar","--spring.profiles.active=${PROFILE}"]
```