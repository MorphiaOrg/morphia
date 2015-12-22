package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class UpdateToNextVersionTask extends DefaultTask {

    UpdateToNextVersionTask() {
        description = 'Update the version in the build file to the next SNAPSHOT version and commit'
    }

    @TaskAction
    def updateToNextVersion() {
        def oldVersion = project.release.releaseVersion
        def newVersion = incrementToNextVersion(oldVersion)
        def buildFile = project.file('gradle.properties')
        project.ant.replaceregexp(file: buildFile, match: oldVersion, replace: newVersion)

        def git = Git.open(new File('.'))
        git.commit()
           .setOnly(buildFile.name)
           .setMessage("Updated to next development version: ${newVersion}")
           .call()

        String username = project.property("github.credentials.username")
        String password = project.property("github.credentials.password")

        git.push()
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
           .call()
    }

    static incrementToNextVersion(String old) {
        String[] split = old.split('\\.')
        def last = split.last()
        def next
        if(last.contains("rc")) {
            def end = last.indexOf("rc") + 2
            next = last.substring(0, end) + ((last.substring(end) as int) + 1)
        } else {
            next = (last as int) + 1
        }

        def updated = split[0..-2].join('.')
        updated += ".${next}-SNAPSHOT"
        updated
    }

}
