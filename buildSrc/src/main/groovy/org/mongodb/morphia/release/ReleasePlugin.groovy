package org.mongodb.morphia.release

import org.gradle.api.Plugin
import org.gradle.api.Project

//requires nexus plugin
class ReleasePlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.extensions.create('release', ReleasePluginExtension)
        project.task('prepareRelease', type: PrepareReleaseTask)
        // upload to nexus
        project.task('draftReleaseNotes', type: DraftReleaseNotesTask, dependsOn: 'prepareRelease')
//        project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        project.task('updateToNextVersion', type: UpdateToNextVersionTask, dependsOn: 'draftReleaseNotes')
        project.task('publishJavadoc', type: PublishJavadocTask, dependsOn: 'updateToNextVersion')
        project.task('release', dependsOn: 'publishJavadoc')
    }
}

