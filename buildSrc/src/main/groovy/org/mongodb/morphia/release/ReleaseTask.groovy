package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ReleaseTask extends DefaultTask {
    def releaseVersion

    @TaskAction
    def prepareGitForRelease() {
        def buildFile = project.file('build.gradle')
        def snapshotVersion = "${releaseVersion}-SNAPSHOT"
        project.ant.replaceregexp(file: buildFile, match: snapshotVersion, replace: releaseVersion)

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

    
    
}
