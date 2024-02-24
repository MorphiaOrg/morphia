#!/usr/bin/env bash

echo exiting for now
exit

function sync() {
  if [ -e $1 -a -e ../$1 ]
  then
    echo "*** syncing $1"
    rsync --delete -ra "../$1/" $1
  fi
}

function syncFromCore() {
  NEW=../../core/$1
  if [ -e $NEW ]
  then
    rsync -ra --delete $NEW $1
  else
    echo $NEW does not exist.  not syncing
  fi
}
function handleProblematicSources() {
  rm -f src/main/java/dev/morphia/AbstractEntityInterceptor.java
  syncFromCore src/main/java/dev/morphia/ #mapping #/codec/ #pojo/EntityModel.java
#  syncFromCore src/main/java/dev/morphia/mapping/lifecycle/EntityListenerAdapter.java
#  syncFromCore src/main/java/dev/morphia/mapping/lifecycle/OnEntityListenerAdapter.java
#  syncFromCore src/main/java/dev/morphia/mapping/lifecycle/UntypedEntityListenerAdapter.java
#  syncFromCore src/main/java/dev/morphia/aggregation/ #codecs/stages/GraphLookupCodec.java
#  rm -f src/test/java/dev/morphia/test/BuildConfigTest.java
#  rm -f src/test/java/dev/morphia/test/MorphiaVersionTest.java
  #  rm -f src/test/java/dev/morphia/test/Versions.java
#  rm -f src/test/java/dev/morphia/test/VersionsTest.java
#  rm -f src/test/java/dev/morphia/test/mapping/codec/TestDocumentWriter.java
}

git add .
git reset --hard
git checkout . #--quiet
echo building project
mvn -q install -DskipTests

for i in *
do
  sync $i/src/main/java
  sync $i/src/main/resources
  sync $i/src/main/kotlin
  sync $i/target/generated-sources
done

#cp ../.github/upgrade-test-pom.xml core/pom.xml
cd core

echo updating projecting via openrewrite
#handleProblematicSources
MVN_OPTS=" -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Dmaven.compiler.release=17"
mvn ${MVN_OPTS} -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=dev.morphia.morphia:rewrite:3.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=dev.morphia.UpgradeToMorphia30 2>&1 \
  -Drewrite.exclusions="**/*.json" \
  2>&1 | tee rewrite.out

mvn test -Dmorphia.version=3.0.0-SNAPSHOT ${MVN_OPTS}

git ls-files -m | grep "src/test"

