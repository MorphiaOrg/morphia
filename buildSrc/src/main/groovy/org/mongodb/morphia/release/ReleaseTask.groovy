package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask

class ReleaseTask extends DefaultTask {
    def releaseVersion

    def prepareGitForRelease() {
        updateVersionInBuildFile(project.version, releaseVersion)

        Git git = Git.open(new File('.'))
//        git.commit()
//           .setMessage("Release ${releaseVersion}")
//           .call()
//
//        git.tag()
//           .setName(tagName())
//           .setMessage("Release ${releaseVersion}")
//           .call()
    }

    def updateVersionInBuildFile(oldVersion, newVersion) {
        println project
        
        def buildFile = project.file('build.gradle')
        println buildFile.path
        project.ant.replaceregexp(file: buildFile, match: oldVersion, replace: newVersion)

//        Git git = Git.open(new File('.'))
//        git.add()
//           .addFilepattern(buildFile.path)
//           .call();
    }

}
