#! /bin/sh

initWiki() {
	if [ ! -d ${WIKIDIR} ]
	then
		echo no wiki.  cloning
		git clone git@github.com:mongodb/morphia.wiki.git ${WIKIDIR}
	fi
}

updateIndex() {
	echo "## [Current](https://rawgithub.com/wiki/mongodb/morphia/javadoc/${TAG}/apidocs/index.html)\n" > Javadoc.md
	for i in `ls -t javadoc/`
	do
		if [ "$i" != "${TAG}" ]
		then
			echo "[$i](https://rawgithub.com/wiki/mongodb/morphia/javadoc/${i}/apidocs/index.html)\n" >> Javadoc.md
		fi
	done
}

START=`pwd`
WIKIDIR=${START}/../morphia.wiki
initWiki

select TAG in `git tag`
do
	git checkout -q $TAG
	echo Building javadoc
	mvn -q -f morphia/pom.xml javadoc:javadoc
	APIDIR=${WIKIDIR}/javadoc/${TAG}
	if [ -d ${APIDIR} ]
	then
		cd ${APIDIR}/..
		rm -r ${TAG}
	fi
	cd ${START}
	mkdir -p ${APIDIR}
	cd ${START}
	cp -r morphia/target/site/apidocs ${APIDIR}
	cd ${WIKIDIR}
	updateIndex
	git add .
	git commit -a -m "adding javadoc for the ${TAG} release"
	git push
	cd ${START}
	git checkout master
	mvn clean
	exit
done
