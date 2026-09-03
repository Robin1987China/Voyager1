
# syntax = docker/dockerfile:experimental

FROM maven:3.9.6-eclipse-temurin-8 as builder
WORKDIR /target/dependency
COPY . .

VOLUME ["/root/.m2","/target/dependency/web-vue/node_modules"]
# 多次 builder 不同的版本号
ARG TEMP_VERSION=""
ARG VOYAGER1_VERSION
ENV USE_VOYAGER1_VERSION=${VOYAGER1_VERSION}${TEMP_VERSION}
RUN --mount=type=cache,target=/root/.m2  bash ./script/replaceVersion.sh "${USE_VOYAGER1_VERSION}" "release"

ENV NODE_VERSION 18.19.0

RUN set -eux; \
    ARCH="$(dpkg --print-architecture)"; \
    case "${ARCH}" in \
       aarch64|arm64) \
         BINARY_ARCH='arm64'; \
         ;; \
       amd64|x86_64) \
         BINARY_ARCH='x64'; \
         ;; \
       *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
         ;; \
    esac; \
    curl -LfsSo /opt/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz https://npmmirror.com/mirrors/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz \
    && tar -zxvf /opt/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz -C /usr/local --strip-components=1  \
    && ln -s /usr/local/bin/node /usr/local/bin/nodejs \
    && npm config set registry https://registry.npmmirror.com/

RUN --mount=type=cache,target=/target/dependency/web-vue/node_modules cd web-vue && npm install && npm run build

RUN --mount=type=cache,target=/root/.m2 mvn -B -e -T 1C clean package -pl modules/server -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM maven:3.9.6-eclipse-temurin-8

ARG BUILD_DATE
ARG VOYAGER1_VERSION
ARG TEMP_VERSION=""
ARG DEPENDENCY=/target/dependency


ENV VOYAGER1_HOME   /usr/local/voyager1-server
ENV VOYAGER1_PKG_VERSION	${VOYAGER1_VERSION}${TEMP_VERSION}
ENV VOYAGER1_PKG    server-${VOYAGER1_PKG_VERSION}-release

ENV VOYAGER1_DATA_PATH ${VOYAGER1_HOME}/data
ENV VOYAGER1_LOG_PATH ${VOYAGER1_HOME}/logs
WORKDIR $VOYAGER1_HOME

COPY --from=builder ${DEPENDENCY}/modules/server/target/${VOYAGER1_PKG} ${VOYAGER1_HOME}

RUN apt-get install -y git

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 数据目录
ENV voyager1.path ${VOYAGER1_DATA_PATH}

VOLUME $VOYAGER1_DATA_PATH $VOYAGER1_LOG_PATH

EXPOSE 2122

HEALTHCHECK CMD curl -X POST -f http://localhost:2122/check-system || exit 1

ENTRYPOINT ["/bin/bash", "./bin/BlockListener.sh"]


