#! /bin/sh

cat .travis.template.yml > .travis.yml

for URL in \
  https://repo.mongodb.org/apt/ubuntu/dists/xenial/mongodb-org/4.2/multiverse/binary-amd64/mongodb-org-server_4.2.8_amd64.deb \
  https://repo.mongodb.org/apt/ubuntu/dists/xenial/mongodb-org/4.0/multiverse/binary-amd64/mongodb-org-server_4.0.19_amd64.deb \
  https://repo.mongodb.org/apt/ubuntu/dists/xenial/mongodb-org/3.6/multiverse/binary-amd64/mongodb-org-server_3.6.18_amd64.deb \
  https://repo.mongodb.org/apt/ubuntu/dists/xenial/mongodb-org/3.4/multiverse/binary-amd64/mongodb-org-server_3.4.24_amd64.deb
do
  for DRIVER in 4.0.4 3.12.5 3.11.2 3.10.2 3.9.1 3.8.2 3.7.1
  do
    MONGODB=$(echo "$URL" | grep -o -E '_[0-9]+\.[0-9]+\.[0-9]+_' | sed -e s/_//g )
    echo "  - MONGODB=$MONGODB DRIVER=$DRIVER URL=$URL" >> .travis.yml
  done
done
