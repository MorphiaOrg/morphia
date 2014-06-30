package org.mongodb.morphia.release

import org.gradle.api.DefaultTask

import static org.kohsuke.github.GHIssueState.CLOSED
import static org.kohsuke.github.GHIssueState.OPEN

class DraftReleaseNotesTask extends DefaultTask {

    static getIssuesAsMapOfEnhancementsAndBugs(repository, milestone) {
        def issues = [:].withDefault { [] }
        def list = repository.listIssues(CLOSED)
        list.each { issue ->
            if (issue.milestone?.number == milestone.number) {
                if (issue.labels) {
                    issue.labels.each { label ->
                        issues[label.name] << issue
                    }
                } else {
                    issues['Uncategorized'] << issue
                }
            }
        }
        issues
    }

    static getMilestone(repository, releaseVersion) {
        def milestoneForRelease = repository.listMilestones(OPEN).find { milestone ->
            milestone.title == releaseVersion
        }
        milestoneForRelease
    }
}