package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

// requires you to populate github.credentials.username & github.credentials.password in ~/.gradle/gradle.properties
// uses http://wiki.eclipse.org/JGit/ 
class PushToRemoteTask extends DefaultTask {

    @TaskAction
    def pushToRemote() {
        String username = project.property("github.credentials.username")
        String password = project.property("github.credentials.password")

        Git git = Git.open(new File('.'))
        git.push()
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
           .call()
    }
}
