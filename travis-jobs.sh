#! /bin/sh

cat .travis.template.yml > .travis.yml

for MONGODB in 4.4.1 4.2.8 4.0.19 3.6.18
do
  for DRIVER in 4.1.0 4.0.5 3.12.6
  do
    echo "  - MONGODB=$MONGODB DRIVER=$DRIVER" >> .travis.yml
  done
done
