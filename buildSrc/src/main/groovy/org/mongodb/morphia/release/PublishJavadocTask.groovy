package org.mongodb.morphia.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PublishJavadocTask extends DefaultTask {
    final File wikiRootDir = new File(project.buildDir, 'wiki')
    final File wikiJavadocDir = new File(wikiRootDir, 'javadoc')
    final String wikiGithubUri = 'https://github.com/mongodb/morphia.wiki.git'

    PublishJavadocTask() {
        description = 'Update the wiki with the Javadoc for this release.'
    }

    @TaskAction
    def publishJavadoc() {
        def username = project.property("github.credentials.username")
        def password = project.property("github.credentials.password")

        getLog().info 'Cleaning wiki destination directory'
        wikiRootDir.deleteDir()
        wikiRootDir.mkdirs()

        getLog().info "Cloning wiki from $wikiGithubUri"
        Git wiki = Git.cloneRepository()
                  .setURI(wikiGithubUri)
                  .setDirectory(wikiRootDir)
                  .call()

        copyReleaseJavadocToWiki()
        updateJavadocHomePage()

        getLog().info 'Committing the wiki changes to Github'
        wiki.add()
            .addFilepattern('.')
            .call()
        wiki.commit()
            .setMessage("Publishing Javadoc for release ${project.release.releaseVersion}")
            .call()
        wiki.push()
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
            .call()
    }

    def copyReleaseJavadocToWiki() {
        def destination = new File(wikiJavadocDir, project.release.releaseVersion)
        def javadocDirectory = project.release.javadocDir
        getLog().info "Copying Javadoc from $javadocDirectory to $destination"

        project.copy {
            from(javadocDirectory)
            into(destination)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    def updateJavadocHomePage() {
        def allVersions = []
        def newReleaseJavadocHome = "https://rawgithub.com/wiki/mongodb/morphia/javadoc/${project.release.releaseVersion}/index.html"
        def javadocHomePage = new File(wikiRootDir, "Javadoc.md")
        getLog().info "Updating $javadocHomePage"

        def javadocWikiPageContent = "## [Current](${newReleaseJavadocHome})\n"
        wikiJavadocDir.eachDir {
            d -> allVersions << d
        }
        allVersions.reverse().each {
            def javadocLink = "https://rawgithub.com/wiki/mongodb/morphia/javadoc/${it.name}/index.html"
            javadocWikiPageContent += " * [${it.name}](${javadocLink})\n"
        }
        javadocHomePage.write(javadocWikiPageContent);
    }

    private Logger getLog() { project?.logger ?: LoggerFactory.getLogger(this.class) }
}
