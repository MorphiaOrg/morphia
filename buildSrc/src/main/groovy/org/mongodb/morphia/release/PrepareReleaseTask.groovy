package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PrepareReleaseTask extends DefaultTask {

    PrepareReleaseTask (){
        description = 'Update release version in the build file, commit to github and tag the release'
    }

    @TaskAction
    def prepareGitForRelease() {
        def releaseVersion = project.release.releaseVersion
        def snapshotVersion = project.release.snapshotVersion
        def buildFile = project.file('build.gradle')
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
