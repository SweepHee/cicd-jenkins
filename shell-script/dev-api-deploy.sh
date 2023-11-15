#!/bin/bash

OPEN_PORT=$(docker ps --format "{{.Names}}" | awk '/dev-api/ {print $1}' | sed 's/dev-api-//')

if [[ $OPEN_PORT -eq '' ]]
then
  echo "현재 실행 중인 포트가 없음"
  RUNNING_PORT=-1
  IDLE_PORT=9003
elif [[ $OPEN_PORT -eq '9003' ]]; then
  RUNNING_PORT=9003
  IDLE_PORT=9004
elif [[ $OPEN_PORT -eq '9004' ]]; then
   RUNNING_PORT=9004
   IDLE_PORT=9003
else
    RUNNING_PORT=-1
    IDLE_PORT=9003
fi

nohup docker run --name dev-api-$IDLE_PORT -e PROFILE=ops -p $IDLE_PORT:9001 --add-host host.docker.internal:host-gateway dev/dev-api >> nohup.out 2>&1 &

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

  docker stop dev-api-$RUNNING_PORT > /dev/null
  docker rm dev-api-$RUNNING_PORT > /dev/null
else
  echo "> 실행 포트 : $IDLE_PORT"
fi


sudo sed -i "s/${RUNNING_PORT}/${IDLE_PORT}/" /etc/nginx/conf.d/dev-api-service-url.inc
sudo systemctl reload nginx
echo "Deploy Completed!!"
