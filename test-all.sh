#! /bin/bash

function sanitize() {
  echo $* | sed -e "s|[',]||g"| sed -e "s|\[||g"| sed -e "s|\]||g"
}

function findRoot() {
  CUR=$( pwd )
  while [ ! -d .mvn ]
  do
    cd ..
  done
  ROOT=$( pwd )
  cd $CUR
}

function selectServers() {
  PS3="Select a server version: "

  if [ -z "$SERVERS" ]
  then
    LIST=`sanitize $( ${ROOT}/.github/BuildMatrix.java )`
    select SERVER in all $LIST
    do
      case $SERVER in
        all)
          SERVERS=$LIST
          ;;
        *)
          SERVERS=$SERVER
          ;;
      esac
      break
    done
  fi
}

function selectDrivers() {
  PS3="Select a driver version: "

  if [ -z "$DRIVERS" ]
  then
    LIST=$( sanitize $( ${ROOT}/.github/DriverVersions.java all ) )
    select DRIVER in all $LIST
    do
      case $DRIVER in
        all)
          DRIVERS=$LIST
          ;;
        *)
          DRIVERS=$DRIVER
          ;;
      esac
      break
    done
  fi
}

findRoot
selectServers
selectDrivers

[ "$TEST" ] && TEST=$( echo -Dtest=$TEST )

mvn install -DskipTests
mkdir -p target

rm -f target/mongo-*

FAILURES=""
for SERVER in $SERVERS
do
   for DRIVER in $DRIVERS
   do
      echo "***"
      echo "*** testing with mongo $SERVER and driver $DRIVER $TEST"
      echo "***"
      OUTFILE="target/mongo-$SERVER-driver-$DRIVER.txt"
      mvn surefire:test -Dmongodb=$SERVER -Ddriver.version=$DRIVER ${TEST} #| tee "$OUTFILE"
      if [ $? -ne 0 ]
      then
        FAILURES="${FAILURES}\t--- mongo $SERVER and driver $DRIVER\n"
      fi
   done
done

if [ ! -z "$FAILURES" ]
then
  echo -e "*** Failures:\n$FAILURES"
fi