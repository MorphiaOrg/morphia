#! /bin/sh

mvn install -DskipTests
mvn -f morphia/pom.xml test
mvn -f legacy-tests/pom.xml test