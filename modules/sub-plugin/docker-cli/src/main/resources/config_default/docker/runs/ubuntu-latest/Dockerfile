
FROM ubuntu:latest
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG C.UTF-8
RUN sed -i.bak 's/archive.ubuntu.com/mirror.nju.edu.cn/' /etc/apt/sources.list \
    && sed -i 's/security.ubuntu.com/mirror.nju.edu.cn/' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y curl wget

ENTRYPOINT ["/bin/bash", "/tmp/build.sh"]
