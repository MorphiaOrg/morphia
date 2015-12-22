package org.mongodb.morphia.release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Inspired originally by this <a href="https://github.com/trnl/github-release-gradle-plugin/">Release Plugin</a>.  This co-ordinates the
 * tasks that make up the steps of Morphia's automated release process.
 *
 * This uses the following plugins & libraries<ol>
 * <li><a href="https://github.com/bmuschko/gradle-nexus-plugin">Nexus plugin</a></li>
 * <li><a href="https://github.com/kohsuke/github-api">Github plugin</a></li>
 * <li><a href="http://www.eclipse.org/jgit/download/">JGit</a></ol>
 *
 * As such, you need to provide the configuration those tools need.  You'll need Nexus and Github credentials in 
 * <code>~/.gradle/gradle.properties</code>, and Github credentials in <code>~/.github</code> - this duplication is because the two 
 * git-related libraries get their configuration from different places.
 */
class ReleasePlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.extensions.create('release', ReleasePluginExtension)
        project.evaluationDependsOnChildren()

        project.task('prepareRelease', type: PrepareReleaseTask, dependsOn: project.subprojects.clean)
        // uploadArchives is configured in publish.gradle
        project.task('draftReleaseNotes', type: DraftReleaseNotesTask, dependsOn: ['prepareRelease', project.subprojects.uploadArchives])
        project.task('publishJavadoc', type: PublishJavadocTask, dependsOn: ['draftReleaseNotes', project.subprojects.javadoc])
        project.task('updateToNextVersion', type: UpdateToNextVersionTask, dependsOn: 'publishJavadoc')
        project.task('release', dependsOn: 'updateToNextVersion')
    }

}

