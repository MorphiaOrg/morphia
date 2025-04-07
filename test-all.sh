#! /bin/sh


BUILD=.github/workflows/build.yml
MATDRIVER=$( .github/DriverVersions.java | grep '{'  | cut -d\[ -f3 | cut -d\] -f1 | sed -e 's/,//g' )
MONGOS=$( .github/BuildMatrix.java | tr -d '[],' )

#echo MATDRIVER=$MATDRIVER
#echo MONGOS=$MONGOS
#exit

echo '\033]30;'Primary First Build'\007'
mvn install -DskipTests

mkdir -p target
rm -f target/mongo-*-driver-*.txt

for MONGO in $MONGOS
do
   for DRIVER in $MATDRIVER
   do
      echo $'\033]30;'Driver: $DRIVER -- Mongo: $MONGO'\007'
      OUTFILE="target/mongo-$MONGO-driver-$DRIVER.txt"
      mvn -U dependency:resolve -Dmongodb=$MONGO -Ddriver.version=$DRIVER | tee "$OUTFILE"
      mvn -U surefire:test -Dmongodb=$MONGO -Ddriver.version=$DRIVER | tee -a "$OUTFILE"
   done
done
