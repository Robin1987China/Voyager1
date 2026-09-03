#!/bin/bash

if [ $MAVEN_VERSION ]; then
	echo "MAVEN_VERSION ${MAVEN_VERSION}"
else
	echo "not found MAVEN_VERSION"
	exit 1
fi
cd /tmp
download_url="https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
wget ${download_url} -O maven.tar.gz
tar -zxf maven.tar.gz --strip-components 1 -C /opt/maven/
