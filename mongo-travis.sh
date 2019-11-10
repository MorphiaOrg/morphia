#! /bin/sh

set -e
download() {
    
  BASE_VERSION=$(echo ${MONGODB} | cut -d\. -f1-2)
  FILE=/tmp/mongodb-${MONGODB}.tgz

  if [ "$BASE_VERSION" != "4.2" ]
  then
    URL=http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-${MONGODB}.tgz
  else
    URL=https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-${LINUX}-${MONGODB}.tgz
  fi
  [ -e ${FILE} ] || wget ${URL} -O ${FILE}
  
  rm -rf mongodb-linux-*
  tar -xvf ${FILE}
  rm -rf /tmp/data
  mkdir -p /tmp/data
  ${PWD}/mongodb-linux-x86_64-*/bin/mongod --quiet \
    --dbpath /tmp/data \
    --bind_ip 127.0.0.1 \
    --logpath /tmp/mongodb-${MONGODB}.log &> /dev/null &
}

LINUX=ubuntu1604
if [ "${MONGODB}" ]
then
  download
else
  sudo service mongodb stop
  killall -9  mongod || true
  LINUX=ubuntu1804
  grep MONGODB .travis.yml | grep -v \# | while read LINE
  do
    eval $(echo $LINE | cut -d" " -f2)
    eval $(echo $LINE | cut -d" " -f3)

    echo SERVER = ${MONGODB}    DRIVER = ${DRIVER}
    download
    mvn -f morphia/pom.xml test
    killall mongod
  done
  sudo service mongodb start
fi
