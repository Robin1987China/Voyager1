#!/bin/bash


# 版本
voyager1_version=$1
voyager1_tag=$2

#Mirror_Host=download.fastgit.org
#Mirror_Host=hub.fastgit.xyz
#Mirror_Host=github.com

function checkItem()
{
rm -f $1-${voyager1_version}-release.$2.sha1 $1-${voyager1_version}-release.$2

curl -LfsSo $1-${voyager1_version}-release.$2.sha1 https://d.voyager1.download/${voyager1_tag}/${voyager1_version}/$1-${voyager1_version}-release.$2.sha1

ESUM=`cat $1-${voyager1_version}-release.$2.sha1`

echo "$1-${voyager1_version}-release.$2 => ${ESUM}"

curl -LfsSo $1-${voyager1_version}-release.$2 https://d.voyager1.download/${voyager1_tag}/${voyager1_version}/$1-${voyager1_version}-release.$2

echo "${ESUM} $1-${voyager1_version}-release.$2" | sha1sum -c -;

rm -f $1-${voyager1_version}-release.$2.sha1 $1-${voyager1_version}-release.$2
}

# check agent
checkItem agent tar.gz
checkItem agent zip

# check server
checkItem server tar.gz
checkItem server zip

