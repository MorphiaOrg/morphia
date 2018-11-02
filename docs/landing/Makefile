.PHONY: all
.DEFAULT_GOAL := all

CURRENT = $(shell mvn -f ../pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO)
DRIVER = $(shell mvn -f ../pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=driver.version | grep -v INFO \
	| sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')
MAJOR = $(shell echo ${CURRENT} | sed -e 's/-SNAPSHOT//' -e 's/.[0-9]*$$//')

CORE = ../../morphia

public/index.html: $(shell find . | grep -v public)
	@hugo

all: public/index.html

publish: all
	rsync -ra public/ ../../target/gh-pages/

watch: all
	hugo server --baseUrl=http://localhost/ --buildDrafts --watch

clean:
	rm -rf public