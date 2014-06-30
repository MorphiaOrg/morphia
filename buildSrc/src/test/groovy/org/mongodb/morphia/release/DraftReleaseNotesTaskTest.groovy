package org.mongodb.morphia.release

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.kohsuke.github.GHMilestone
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import spock.lang.Ignore
import spock.lang.Specification

class DraftReleaseNotesTaskTest extends Specification {
    def 'should be able to add the task to a project'() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        def task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)

        then:
        task instanceof DraftReleaseNotesTask
    }

    @Ignore('Functional')
    def 'should get an open milestone corresponding to the release number'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")
        def releaseVersion = 'post-1.0'

        when:
        GHMilestone milestone = task.getMilestone(repository, releaseVersion)

        then:
        milestone != null
        milestone.title == releaseVersion
    }

    @Ignore('Functional')
    def 'should throw an exception if the release does not have a milestone in github or is closed'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")

        when:
        GHMilestone milestone = task.getMilestone(repository, '0.108')

        then:
        thrown IllegalArgumentException
    }

    @Ignore('Functional')
    def 'should return all closed issues for a given milestone and partition them by bug or enhancement'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")
        GHMilestone release108Milestone = repository.getMilestone(9)

        when:
        def issues = task.getIssuesAsMapOfEnhancementsAndBugs(repository, release108Milestone)

        then:
        issues != null
        issues.enhancement.size + issues.bug.size == release108Milestone.closedIssues
    }

    @Ignore('Functional')
    def 'create a default template for release notes'() {
        //note: functional test that needs a connection to github and a real milestone
        //also: not an awesome test as it duplicates a lot of what the real method does
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")
        def releaseVersion = 'post-1.0'

        when:
        def notes = task.createDraftReleaseNotes(repository, releaseVersion)

        then:
        notes != null
        def date = new Date().format("MMM dd, yyyy")
        notes == "\n## Version post-1.0 (${date})\n\n" +
        "### Downloads\n" +
        "Below and on maven central.\n\n" +
        "### Docs\n" +
        "https://rawgithub.com/wiki/mongodb/morphia/javadoc/${releaseVersion}/apidocs/index.html\n\n" +
        "### Issues Resolved\n"
        //this would be populated if we hadn't chosen a release that currently has no closed issues
    }

}
