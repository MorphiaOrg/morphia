#!/bin/bash

set -o xtrace   # Write all commands first to stderr
set -o errexit  # Exit the script with error if any of the commands fail

# Supported/used environment variables:
#       MONGODB_URI             Set the suggested connection MONGODB_URI (including credentials and topology info)
#       TOPOLOGY                Allows you to modify variables and the MONGODB_URI based on test topology 
#                               Supported values: "server", "replica_set", "sharded_cluster"
#       JDK                     Set the version of java to be used.  Java versions can be set from the java toolchain /opt/java
#                               "jdk5", "jdk6", "jdk7", "jdk8"

MONGODB_URI=${MONGODB_URI:-}
JDK=${JDK:-jdk}
JAVA_HOME="/opt/java/${JDK}"
TOPOLOGY=${TOPOLOGY:-server}

############################################
#            Main Program                  #
############################################

# Test with a single mongos
if [ "$TOPOLOGY" == "sharded_cluster" ]; then
     export MONGODB_URI="mongodb://localhost:27017"
fi

echo "Running tests for $TOPOLOGY and connecting to $MONGODB_URI"

echo "Compiling with jdk8"
# We always compile with the latest version of java
export JAVA_HOME="/opt/java/jdk8"
./gradlew -version
./gradlew --info classes testClasses

echo "Running tests with ${JDK}"
JAVA_HOME="/opt/java/${JDK}"
./gradlew -version
./gradlew -DMONGO_URI=${MONGODB_URI} --stacktrace --info -x classes -x testClasses --rerun-tasks test
