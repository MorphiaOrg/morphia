package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy

class PublishJavadocTask extends DefaultTask {
    private static final String GROUP_WIKI = 'Github Wiki'
    final String wikiUri = 'https://github.com/mongodb/morphia.wiki.git'
    final File wikiRootDir = new File(project.buildDir, 'wiki')
    final File wikiJavadocDir = new File(wikiRootDir, 'javadoc')
    Git wiki


    PublishJavadocTask() {
        description = 'Clone, process, commit and push wiki with Javadoc for new release added to remote.'

        project.task('cleanWiki', group: GROUP_WIKI) << this.&cleanWikiDestination
        project.task('cloneWiki', group: GROUP_WIKI, dependsOn: 'cleanWiki') << this.&cloneWikiIntoWorkingDirectory
        project.task('javadocToWiki', group: GROUP_WIKI, dependsOn: ['javadoc', 'cloneWiki']) << this.&copyReleaseJavadocToWiki
        project.task('updateWiki', group: GROUP_WIKI, dependsOn: 'javadocToWiki') << this.&updateJavadocHomePage
        project.task('commitWiki', group: GROUP_WIKI, dependsOn: 'updateWiki') << this.&commitWiki
        project.task('pushWiki', group: GROUP_WIKI, dependsOn: 'commitWiki') << this.&pushWiki

        this.dependsOn('pushWiki')
    }

    def cleanWikiDestination() {
        wikiRootDir.deleteDir()
        wikiRootDir.mkdirs()
    }

    def cloneWikiIntoWorkingDirectory() {
        wiki = Git.cloneRepository()
                  .setURI(wikiUri)
                  .setDirectory(wikiRootDir)
                  .call()
    }

    def copyReleaseJavadocToWiki() {
        def destination = new File(wikiJavadocDir, project.release.releaseVersion)
        def javadocDirectory = project.release.javadocDir
        println "Copying Javadoc from $javadocDirectory to $destination"

        project.copy {
            from(javadocDirectory)
            into(destination)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    def updateJavadocHomePage() {
        def allVersions = []
        def newReleaseJavadocHome = "https://rawgithub.com/wiki/mongodb/morphia/javadoc/${project.release.releaseVersion}/index.html"

        def javadocWikiPageContent = "## [Current](${newReleaseJavadocHome})\n"
        wikiJavadocDir.eachDir {
            d -> allVersions << d
        }
        allVersions.reverse().each {
            def javadocLink = "https://rawgithub.com/wiki/mongodb/morphia/javadoc/${it.name}/index.html"
            javadocWikiPageContent += " * [${it.name}](${javadocLink})\n"
        }
        new File(wikiRootDir, "Javadoc.md").write(javadocWikiPageContent);
    }

    def commitWiki() {
        wiki = Git.open(wikiRootDir)
        wiki.add()
            .addFilepattern('.')
            .call()
        wiki.commit()
            .setMessage("Publishing Javadoc for release ${project.release.releaseVersion}")
            .call()
    }

    def pushWiki() {
        String username = project.property("github.credentials.username")
        String password = project.property("github.credentials.password")

        wiki = Git.open(wikiRootDir)
        wiki.push()
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
            .call();
    }
}
