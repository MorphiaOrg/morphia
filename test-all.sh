#! /bin/sh


DRIVERS=$( yq '.jobs.Test.strategy.matrix.include.[].driver' .github/workflows/build.yml  | sort -r | uniq )
MONGOS=$( yq '.jobs.Test.strategy.matrix.include.[].mongo' .github/workflows/build.yml  | sort -r | uniq )

mvn clean install -DskipTests

mkdir -p target

for MONGO in $MONGOS
do
   for DRIVER in $DRIVERS
   do
      echo $'\033]30;'Driver: $DRIVER -- Mongo: $MONGO'\007'
      mvn surefire:test -Dmongodb=$MONGO -Ddriver.version=$DRIVER | tee target/mvn-$MONGOS-$DRIVER.out
   done
done
