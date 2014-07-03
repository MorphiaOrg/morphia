package org.mongodb.morphia.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHMilestone
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import static org.kohsuke.github.GHIssueState.CLOSED
import static org.kohsuke.github.GHIssueState.OPEN

//this class needs you to have your credentials in ~/.github
class DraftReleaseNotesTask extends DefaultTask {
    String repositoryName = "mongodb/morphia"
    //generally we're going to expect the release milestone to be open. This is set-able for testing.
    GHIssueState expectedMilestoneState = OPEN

    @TaskAction
    void draftReleaseNotes() {
        ensureArchivesAreNotSnapshotJarFiles()
        def releaseVersion = project.release.releaseVersion
        GHRepository repository = GitHub.connect().getRepository(repositoryName)
        def milestoneNumber = getMilestoneNumber(repository, releaseVersion, expectedMilestoneState)
        def notes = createDraftReleaseNotesContent(repository,
                                                   releaseVersion,
                                                   getIssuesAsMapOfEnhancementsAndBugs(repository, milestoneNumber),
                                                   new Date())
        def githubRelease = repository.createRelease("r${releaseVersion}")
                                      .name(releaseVersion)
                                      .body(notes.toString())
                                      .draft(true)
                                      .create()
        attachJarFilesToRelease(releaseVersion, githubRelease)
    }

    private ensureArchivesAreNotSnapshotJarFiles() {
        String jarFilePath = project.getTasksByName('jar', false).toArray()[0].archivePath
        if (jarFilePath.contains('SNAPSHOT')) {
            throw new IllegalStateException('Cannot upload snapshot JAR files onto the release notes. Make sure the version number in ' +
                                            'the build file does not have the -SNAPSHOT suffix');
        }
    }

    private attachJarFilesToRelease(releaseVersion, ghRelease) {
        project.subprojects { subproject ->
            subproject.configurations.archives { archive ->
                def jar = archive.getAllArtifacts().find { artifact ->
                    artifact.getFile().getName().endsWith("-${releaseVersion}.jar")
                }

                println "Uploading ${jar.getFile()}"
                ghRelease.uploadAsset(jar.getFile(), "application/jar")
            }
        }
    }

    static createDraftReleaseNotesContent(repository, releaseVersion, issues, date) {
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

    static getIssuesAsMapOfEnhancementsAndBugs(repository, milestoneNumber) {
        def issues = [:].withDefault { [] }
        def list = repository.listIssues(CLOSED)
        list.each { issue ->
            if (issue.milestone?.number == milestoneNumber) {
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

    static getMilestoneNumber(repository, releaseVersion, status) {
        GHMilestone milestoneForRelease = repository.listMilestones(status).find { milestone ->
            milestone.title == releaseVersion
        }
        if (milestoneForRelease == null) {
            throw new IllegalArgumentException("Github milestone ${releaseVersion} either does not exist, or is already closed.")
        }
        milestoneForRelease.number
    }
}