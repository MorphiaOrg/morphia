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

  [ -e mongodb-linux-x86_64-${MONGODB} ] && sudo rm -rf mongodb-linux-*
  tar -xvf ${FILE}
  rm -rf /tmp/data
  mkdir -p /tmp/data

  ${PWD}/mongodb-linux-x86_64-*/bin/mongod --quiet \
    --replSet morphia \
    --dbpath /tmp/data \
    --bind_ip 127.0.0.1 \
    --logpath /tmp/mongodb-${MONGODB}.log &

  for i in $(seq 1 5)
  do
    ${PWD}/mongodb-linux-x86_64-*/bin/mongo --quiet --eval "rs.initiate()" && break
    sleep 3
    echo "Reattempting replSet initiation"
  done

  cd -
}

LINUX=ubuntu1604
if [ "${MONGODB}" ]
then
  killall -9 mongod && sleep 3 || true
  download
  if [ -z "$DRIVER" ]
  then
    mvn install
    killall -9 mongod
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
      mvn clean install
      killall mongod
#    done
  done
fi
