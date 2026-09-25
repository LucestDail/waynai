#!/bin/bash
# waynai 백엔드 운영 실행 — .env(bash 소스) 로드 후 java -jar. systemd 에서 호출.
set -a
[ -f /home/seunghyun/Workspace/waynai/.env ] && source /home/seunghyun/Workspace/waynai/.env
set +a
export SERVER_PORT=8081
exec /usr/bin/java -XX:+UseG1GC -Xms256m -Xmx512m \
  -jar /home/seunghyun/Workspace/waynai/waynai-backend/target/waynai-backend-0.0.1-SNAPSHOT.jar
