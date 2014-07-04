package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class UpdateToNextVersionTask extends DefaultTask {

    @TaskAction
    def updateToNextVersion() {
        def oldVersion = project.release.releaseVersion
        def newVersion = incrementToNextVersion(oldVersion)
        def buildFile = project.file('build.gradle')
        project.ant.replaceregexp(file: buildFile, match: oldVersion, replace: newVersion)

        def git = Git.open(new File('.'))
        git.commit()
           .setOnly(buildFile.name)
           .setMessage("Updated to next development version: ${newVersion}")
           .call()
    }

    static incrementToNextVersion(String old) {
        String[] split = old.split('\\.')
        def next = (split.last() as int) + 1

        def updated = split[0..-2].join('.')
        updated += ".${next}-SNAPSHOT"
        updated
    }

}
