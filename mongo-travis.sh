#! /bin/sh

set -e
download() {

  if [ -z "$MONGODB" ]
  then
    MONGODB=$(grep "\- MONGODB" .travis.yml  | grep -o -E '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1)
  fi

  BASE_VERSION=$(echo ${MONGODB} | cut -d\. -f1-2)
  FILE=/tmp/mongodb-${MONGODB}.tgz

  URL=https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-${LINUX}-${MONGODB}.tgz
  [ -e ${FILE} ] || wget ${URL} -O ${FILE}
  
  [ -e mongodb-linux-x86_64-${MONGODB} ] && sudo rm -rf mongodb-linux-*
  rm -rf /tmp/data  mongodb-linux-x86_64* mongodb-bin
  mkdir -p mongodb-bin
  tar -C mongodb-bin -xvf ${FILE} --strip-components=1
  mkdir -p /tmp/data

  ${PWD}/mongodb-bin/bin/mongod --quiet \
    --replSet morphia \
    --dbpath /tmp/data \
    --bind_ip 127.0.0.1 \
    --logpath /tmp/mongodb-${MONGODB}.log &

  for i in $(seq 1 5)
  do
    ${PWD}/mongodb-bin/bin/mongo --quiet --eval "rs.initiate()" && break
    sleep 3
    echo "Reattempting replSet initiation"
  done
}

LINUX=${LINUX-ubuntu1804}
if [ -z "${MATRIX}" ]
then
  killall -9 mongod && sleep 3 || true
  download
  if [ -z "$DRIVER" ]
  then
    echo "press enter to shut down the server"
    read nothing
    mongodb-bin/bin/mongo admin --quiet --eval "db.shutdownServer()"
  fi
else
  killall -9 mongod || true
  LINUX=ubuntu1804
  MONGOS=$(grep MONGODB .travis.yml | cut -d" " -f4 | cut -d\= -f2 | sort -r | uniq)
  DRIVERS=$(grep DRIVER .travis.yml | cut -d" " -f5 | cut -d\= -f2 | sort -r | uniq)
  for MONGODB in ${MONGOS}
  do
#    for DRIVER in ${DRIVERS}
#    do
      echo SERVER = ${MONGODB}    DRIVER = ${DRIVER} | tee /tmp/morphia.test
      download
      mvn install
      killall mongod
#    done
  done
fi
