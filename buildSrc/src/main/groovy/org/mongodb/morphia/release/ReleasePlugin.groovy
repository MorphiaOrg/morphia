package org.mongodb.morphia.release

import org.gradle.api.Plugin
import org.gradle.api.Project

class ReleasePlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.extensions.create('release', ReleasePluginExtension)
        project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        project.task('prepareRelease', type: PrepareReleaseTask)
    }
}

