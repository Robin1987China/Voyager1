#!/bin/bash

#-----------------------------------------------------------
# 此脚本用于每次发布 docker 镜像
#-----------------------------------------------------------


# docker buildx create --use

# 服务端
docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:0.0.2 -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:latest -f ./modules/server/DockerfileRelease --push .

# 插件端
docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1-agent:0.0.2 -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1-agent:latest -f ./modules/agent/DockerfileRelease --push .

docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t registry.cn-chengdu.aliyuncs.com/${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:0.0.2 -t registry.cn-chengdu.aliyuncs.com/${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:latest -f ./modules/server/DockerfileRelease --push .



docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le,linux/arm64/v8 -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:2-test -f ./modules/server/DockerfileBeta --push .
#
#docker buildx build --platform linux/amd64,linux/arm64 -t ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:latest -f ./modules/server/DockerfileRelease --push .

# docker logs --tail="100" voyager1-server
# docker run -d -p 2122:2122 --name voyager1-server -v /etc/localtime:/etc/localtime:ro -v voyager1-server-vol:/usr/local/voyager1-server ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1:mac-arm-0.0.2.1
# docker run -d -p 2122:2122 --name voyager1-server -v D:/home/voyager1-server/logs:/usr/local/voyager1-server/logs -v D:/home/voyager1-server/data:/usr/local/voyager1-server/data -v D:/home/voyager1-server/conf:/usr/local/voyager1-server/conf ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1
# docker stop voyager1-server
# docker rm voyager1-server
# docker exec -it voyager1-server /bin/bash
#  docker-compose up -d --build
# docker buildx imagetools inspect ${VOYAGER1_IMAGE_REPO:-voyager1}/voyager1

