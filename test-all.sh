#! /bin/bash

SKIP_BUILD=false

while getopts "s:d:m:t:n" opt; do
  case $opt in
    s) SERVERS="$OPTARG" ;;
    d) DRIVERS="$OPTARG" ;;
    m) MAPPERS="$OPTARG" ;;
    t) TEST="$OPTARG" ;;
    n) SKIP_BUILD=true ;;
    *) echo "Usage: $0 [-s server] [-d driver] [-m mapper] [-t test] [-n]" >&2; exit 1 ;;
  esac
done

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

function selectMappers() {
  PS3="Select a mapper: "

  if [ -z "$MAPPERS" ]
  then
    select MAPPER in all reflection critter
    do
      case $MAPPER in
        all)
          MAPPERS="reflection critter"
          ;;
        *)
          MAPPERS=$MAPPER
          ;;
      esac
      break
    done
  fi
}

findRoot
selectServers
selectDrivers
selectMappers

[ "$TEST" ] && TEST=$( echo -Dtest=\"$TEST\" )

$SKIP_BUILD || mvn install -DskipTests
mkdir -p target

rm -f target/mongo-*

FAILURES=""
for SERVER in $SERVERS
do
   for DRIVER in $DRIVERS
   do
      for MAPPER in $MAPPERS
      do
         echo "***"
         echo "*** testing with mongo $SERVER and driver $DRIVER and mapper $MAPPER $TEST"
         echo "***"
         OUTFILE="target/mongo-$SERVER-driver-$DRIVER-mapper-$MAPPER.txt"
         mvn -e surefire:test \
            -Dmongodb=$SERVER \
            -Ddriver.version=$DRIVER \
            -Dmorphia.mapper=$MAPPER \
            -Dsurefire.failIfNoSpecifiedTests=false \
            ${TEST} | tee "$OUTFILE"
         if [ $? -ne 0 ]
         then
           FAILURES="${FAILURES}\t--- mongo $SERVER and driver $DRIVER and mapper $MAPPER\n"
         fi
      done
   done
done

if [ ! -z "$FAILURES" ]
then
  echo -e "*** Failures:\n$FAILURES"
fi