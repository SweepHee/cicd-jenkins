#!/bin/bash

# 컨테이너 실행시 dev-callback-9005 or dev-callback-9006으로 실행되므로 9005, 9006을 찾기위함
OPEN_PORT=$(docker ps --format "{{.Names}}" | awk '/dev-callback/ {print $1}' | sed 's/dev-callback-//')

# 현재 동작하고 있는 포트를 찾고 다른 포트에 배포하기 위함
if [[ $OPEN_PORT -eq '' ]]
then
  echo "현재 실행 중인 포트가 없음"
  RUNNING_PORT=-1
  IDLE_PORT=9005
elif [[ $OPEN_PORT -eq '9005' ]]; then
  RUNNING_PORT=9005
  IDLE_PORT=9006
elif [[ $OPEN_PORT -eq '9006' ]]; then
   RUNNING_PORT=9006
   IDLE_PORT=9005
else
    RUNNING_PORT=-1
    IDLE_PORT=9005
fi

# 노헙으로 도커실행 - 로그 남기는 목적?
nohup docker run --name dev-callback-$IDLE_PORT -e PROFILE=ops -p $IDLE_PORT:9002 --add-host host.docker.internal:host-gateway dev/dev-callback >> nohup.out 2>&1 &

# 도커 실행 후 해당 포트가 정상 동작하는지 체크
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

  docker stop dev-callback-$RUNNING_PORT > /dev/null
  docker rm dev-callback-$RUNNING_PORT > /dev/null
else
  echo "> 실행 포트 : $IDLE_PORT"
fi


sudo sed -i "s/${RUNNING_PORT}/${IDLE_PORT}/" /etc/nginx/conf.d/dev-callback-service-url.inc
sudo systemctl reload nginx
echo "Deploy Completed!!"
