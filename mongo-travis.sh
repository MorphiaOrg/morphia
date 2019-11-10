#! /bin/sh

download() {
    
  BASE_VERSION=$(echo ${MONGODB} | cut -d\. -f1-2)
  FILE=/tmp/mongodb-${MONGODB}.tgz
  
  if [ "$BASE_VERSION" != "4.2" ]
  then
    URL=http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-${MONGODB}.tgz
  else
    URL=https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1604-${MONGODB}.tgz
  fi
  [ -e ${FILE} ] || wget ${URL} -O ${FILE}
  
  ls ${FILE}
  rm -rf mongodb-linux-*
  tar -xvf ${FILE}
  mkdir -p /tmp/data
  ${PWD}/mongodb-linux-x86_64-*/bin/mongod --dbpath /tmp/data --bind_ip 127.0.0.1 &> /dev/null &

}

download

#MONGODB=4.2.1 download
#MONGODB=4.0.13 download
#MONGODB=3.6.15 download
#MONGODB=3.4.23 download
#MONGODB=3.2.22 download
#MONGODB=3.0.15 download
