package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask

class ReleaseTask extends DefaultTask {
    def releaseVersion

    def theRelease() {
        def buildFile = project.file('build.gradle')
        updateVersionInBuildFile(buildFile)

        def git = Git.open(new File('.'))
        git.commit()
           .setOnly(buildFile.name)
           .setMessage("Release ${releaseVersion}")
           .call()

        git.tag()
           .setName("r${releaseVersion}")
           .setMessage("Release ${releaseVersion}")
           .setForceUpdate(true)
           .call()
    }

    def updateVersionInBuildFile(File buildFile) {
        def snapshotVersion = "${releaseVersion}-SNAPSHOT"
        project.ant.replaceregexp(file: buildFile, match: snapshotVersion, replace: releaseVersion)
    }

}
