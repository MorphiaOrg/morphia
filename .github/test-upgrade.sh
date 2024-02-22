#! /bin/bash

echo "************************"
pwd
echo "************************"
for i in *
do
  [-e $i/src/main ] && rm -rf $i/src/main
done

mvn -Dmorphia.version=3.0.0-SNAPSHOT