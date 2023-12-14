#! /bin/bash


function sanitize() {
  echo $* | sed -e "s|[',]||g"| sed -e "s|\[||g"| sed -e "s|\]||g"
}

BUILD=.github/workflows/build.yml
[ -z "$DRIVERS" ] && MATDRIVER=`sanitize $( ./.github/DriverVersions.java  all )`
[ -z "$DRIVERS" ] && SNAPSHOT=`sanitize $( ./.github/DriverSnapshot.java )`
[ -z "$MONGOS" ] && MONGOS=`sanitize $( ./.github/BuildMatrix.java )`

[ -z "$DRIVERS" ] && DRIVERS=$( echo $MATDRIVER $SNAPSHOT | sort -r | uniq )

echo DRIVERS=$DRIVERS
echo MONGOS=$MONGOS

# exit

./mvnw install -DskipTests

mkdir -p target
rm -f target/mongo-*

for MONGO in $MONGOS
do
   for DRIVER in $DRIVERS
   do
      echo testing with mongo $MONGO and driver $DRIVER
      OUTFILE="target/mongo-$MONGO-driver-$DRIVER.txt"
      ./mvnw surefire:test -Dmongodb=$MONGO -Ddriver.version=$DRIVER | tee "$OUTFILE" && rm "$OUTFILE"
   done
done
