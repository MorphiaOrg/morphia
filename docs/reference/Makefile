.PHONY: all
.DEFAULT_GOAL := all

POM = ../../pom.xml
SETTINGS = ../../target/maven.settings.xml
CURRENT = $(shell grep "\<version\>" $(SETTINGS) | head -n 3 | tail -n 1 | cut -d\> -f2 | cut -d\< -f1 )
DRIVER = $(shell grep "\<driver.version\>" $(SETTINGS) | head -n 1 | cut -d\> -f2 | cut -d\< -f1 | sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')
MAJOR = $(shell echo ${CURRENT} | sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')
BRANCH = $(shell git branch | grep \* | cut -d ' ' -f2 )
SRC_LINK = "https://github.com/MorphiaOrg/morphia/tree/$(BRANCH)"
TEXT = Morphia $(MAJOR)

CORE = ../../morphia
JAVADOC = $(CORE)/target/site/apidocs

$(SETTINGS): $(shell find ../.. -name pom.xml)
	@echo Emitting property values
	@mvn -q -f $(POM) help:effective-pom -Doutput=target/maven.settings.xml

data/morphia.toml: $(POM) Makefile ../../target/maven.settings.xml
	@echo Updating documentation to use $(CURRENT) for the current version
	@echo with the major version of $(MAJOR) and driver version of $(DRIVER).
	@sed -e "s/currentVersion.*/currentVersion = \"$(CURRENT)\"/" \
	 	-e "s/majorVersion.*/majorVersion = \"$(MAJOR)\"/" \
		-e "s|coreApiUrl.*|coreApiUrl = \"http://mongodb.github.io/mongo-java-driver/$(DRIVER)/javadoc/\"|" \
		-e "s|gitBranch.*|gitBranch = \"$(BRANCH)\"|" \
		data/morphia.toml > data/morphia.toml.sed
	@mv data/morphia.toml.sed data/morphia.toml

	@sed -e "s|<span id=\"version-tag\">.*|<span id=\"version-tag\">$(TEXT)</span>|" \
		layouts/partials/logo.html > layouts/partials/logo.html.sed
	@mv layouts/partials/logo.html.sed layouts/partials/logo.html

$(JAVADOC)/index.html: $(shell find $(CORE)/src/main/java -name *.java)
	mvn -f ../../morphia javadoc:javadoc

public/index.html: $(POM) $(shell find . | grep -v public)
	@hugo

all: public/index.html $(JAVADOC)/index.html
	@mkdir -p public/javadoc
	@rsync -ra $(JAVADOC)/ public/javadoc

publish: all
	rsync -ra --delete public/ ../../target/gh-pages/$(MAJOR)

watch: all
	hugo server --baseUrl=http://localhost/ --buildDrafts --watch

clean:
	rm -rf public
