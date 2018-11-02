.PHONY: all
.DEFAULT_GOAL := all

CURRENT = $(shell mvn -f ../pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO)
DRIVER = $(shell mvn -f ../pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=driver.version | grep -v INFO \
	| sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')
MAJOR = $(shell echo ${CURRENT} | sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')

CORE = ../../morphia
JAVADOC = $(CORE)/target/site/apidocs

$(JAVADOC)/index.html: $(shell find $(CORE)/src/main/java -name *.java)
	mvn -f ../../morphia javadoc:javadoc

public/index.html: $(shell find . | grep -v public)
	@sed -i "" \
	 	-e "s/currentVersion.*/currentVersion = \"$(CURRENT)\"/" \
	 	-e "s/majorVersion.*/majorVersion = \"$(MAJOR)\"/" \
		-e "s|coreApiUrl.*|coreApiUrl = \"http://mongodb.github.io/mongo-java-driver/$(DRIVER)/javadoc/\"|" \
		data/morphia.toml
	@hugo

all: public/index.html $(JAVADOC)/index.html
	@rsync -ra $(JAVADOC) public

watch: all
	hugo server --baseUrl=http://localhost/ --buildDrafts --watch