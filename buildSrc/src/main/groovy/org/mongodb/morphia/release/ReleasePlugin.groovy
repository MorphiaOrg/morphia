package org.mongodb.morphia.release

import org.gradle.api.Plugin
import org.gradle.api.Project

class ReleasePlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        project.extensions.create('release', ReleasePluginExtension)
    }
}

