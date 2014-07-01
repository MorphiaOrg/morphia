package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask

class ReleaseTask extends DefaultTask {
    def releaseVersion
    
    def theRelease () {
        updateVersionInBuildFile()
        prepareGitForRelease();
    }

    def prepareGitForRelease() {
        def git = Git.open(new File('.'))
        git.commit()
           .setMessage("Release ${releaseVersion}")
           .call()

        git.tag()
           .setName("r${releaseVersion}")
           .setMessage("Release ${releaseVersion}")
           .call()
    }

    def updateVersionInBuildFile() {
        def buildFile = project.file('build.gradle')
        def snapshotVersion = "${releaseVersion}-SNAPSHOT"
        project.ant.replaceregexp(file: buildFile, match: snapshotVersion, replace: releaseVersion)
    }

}
