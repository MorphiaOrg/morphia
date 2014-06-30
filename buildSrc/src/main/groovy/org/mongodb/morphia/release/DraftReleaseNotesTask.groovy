package org.mongodb.morphia.release

import org.gradle.api.DefaultTask

import static org.kohsuke.github.GHIssueState.CLOSED
import static org.kohsuke.github.GHIssueState.OPEN

class DraftReleaseNotesTask extends DefaultTask {

    static createDraftReleaseNotes(repository, releaseVersion) {
        def milestone = getMilestone(repository, releaseVersion)
        def issues = getIssuesAsMapOfEnhancementsAndBugs(repository, milestone)

        def javadoc = "https://rawgithub.com/wiki/${repository.owner.name}/${repository.name}/javadoc/${releaseVersion}/apidocs/index.html";

        def notes = """
## Version ${releaseVersion} (${new Date().format("MMM dd, yyyy")})

### Downloads
Below and on maven central.

### Docs
${javadoc}

### Issues Resolved
"""
        issues.keySet().each { entry ->
            notes += "#### ${entry.toUpperCase()}\n"
            issues[entry].each { issue ->
                notes += "* [Issue ${issue.number}](${issue.html_url}): ${issue.title}\n"
            }
            notes += "\n"
        }

        notes
    }
    
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
        if (milestoneForRelease == null) {
            throw new IllegalArgumentException("Github milestone ${releaseVersion} either does not exist, or is already closed.")
        }
        milestoneForRelease
    }
}