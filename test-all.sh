#! /bin/sh


BUILD=.github/workflows/build.yml
MATDRIVER=$( yq '.jobs.Test.strategy.matrix.driver[]' $BUILD )
INCDRIVER=$( yq '.jobs.Test.strategy.matrix.include.[].driver' $BUILD )
DRIVERS=$( echo $MATDRIVER $INCDRIVER | sort -r | uniq )
MONGOS=$( yq '.jobs.Test.strategy.matrix.mongo[]' $BUILD  | sort | uniq )

#echo DRIVERS=$DRIVERS
echo MONGOS=$MONGOS
#exit

echo $'\033]30;'Primary First Build'\007'
mvn install -DskipTests

mkdir -p target

for MONGO in $MONGOS
do
   for DRIVER in $MATDRIVER
   do
      echo $'\033]30;'Driver: $DRIVER -- Mongo: $MONGO'\007'
      OUTFILE="target/mongo-$MONGO-driver-$DRIVER.txt"
      mvn surefire:test -Dmongodb=$MONGO -Ddriver.version=$DRIVER | tee "$OUTFILE"
   done
done
