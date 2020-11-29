#! /bin/sh

BUILD=".github/workflows/build.yml"
cat config/build.yml > ${BUILD}

for JVM in 11 15
do
  for MONGODB in 4.4.2 4.2.11 4.0.21 3.6.21
  do
    NAME=$(echo Java-${JVM}-MongoDB-${MONGODB} | tr \. _)
    cat <<EOF >> ${BUILD}
  ${NAME}:
    runs-on: ubuntu-latest
    env:
      MONGODB: ${MONGODB}
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK ${JVM}
        uses: actions/setup-java@v1
        with:
          java-version: ${JVM}
      - name: Cache Maven packages
        uses: actions/cache@v1
        with:
          path: ~/.m2
          key: \${{ runner.os }}-m2-\${{ hashFiles('**/pom.xml') }}
          restore-keys: \${{ runner.os }}-m2
      - name: Build with Maven
        run: mvn -B verify --file pom.xml
EOF
  done
done

