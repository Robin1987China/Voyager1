
FROM maven:3.9.6-eclipse-temurin-8 as builder
WORKDIR /target/dependency
COPY . .

VOLUME ["/root/.m2"]
# 多次 builder 不同的版本号
ARG TEMP_VERSION=""
ARG VOYAGER1_VERSION
ENV USE_VOYAGER1_VERSION=${VOYAGER1_VERSION}${TEMP_VERSION}
RUN --mount=type=cache,target=/root/.m2 sh ./script/replaceVersion.sh ${USE_VOYAGER1_VERSION} "release"

RUN --mount=type=cache,target=/root/.m2 mvn -B -e -T 1C clean package -pl modules/agent -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM openjdk:8
ARG BUILD_DATE
ARG VOYAGER1_VERSION
ARG TEMP_VERSION=""
ARG RUN_ARG=""
ARG DEPENDENCY=/target/dependency


ENV VOYAGER1_HOME	/usr/local/voyager1-agent
ENV VOYAGER1_PKG_VERSION	${VOYAGER1_VERSION}${TEMP_VERSION}
ENV VOYAGER1_PKG    agent-${VOYAGER1_PKG_VERSION}-release
ENV RUN_ARG ${RUN_ARG}

WORKDIR $VOYAGER1_HOME

COPY --from=builder ${DEPENDENCY}/modules/agent/target/${VOYAGER1_PKG} ${VOYAGER1_HOME}

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

VOLUME $VOYAGER1_HOME
EXPOSE 2123

ENTRYPOINT ["/bin/bash", "./bin/Agent.sh", "start"]

