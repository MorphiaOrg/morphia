#! /bin/sh

cat .travis.template.yml > .travis.yml

for MONGODB in 4.2.8 4.0.19 3.6.18 3.4.24
do
  for DRIVER in 4.0.4 3.12.5 3.11.2 3.10.2 3.9.1 3.8.2 3.7.1
  do
    echo "  - MONGODB=$MONGODB DRIVER=$DRIVER" | tee -a .travis.yml
    [ -e /tmp/mongodb-${MONGODB}.tgz ] || wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1604-${MONGODB}.tgz -O /tmp/mongodb-${MONGODB}.tgz
  done
done
