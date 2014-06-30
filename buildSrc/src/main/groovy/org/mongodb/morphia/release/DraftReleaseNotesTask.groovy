package org.mongodb.morphia.release

import org.gradle.api.DefaultTask

import static org.kohsuke.github.GHIssueState.OPEN

class DraftReleaseNotesTask extends DefaultTask {

    def static getMilestone(repository, releaseVersion) {
        def milestoneForRelease = repository.listMilestones(OPEN).find { milestone ->
            milestone.title == releaseVersion
        }
        milestoneForRelease
    }
}