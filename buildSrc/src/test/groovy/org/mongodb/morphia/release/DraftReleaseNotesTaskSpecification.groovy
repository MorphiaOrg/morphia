package org.mongodb.morphia.release

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHMilestone
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import spock.lang.Ignore
import spock.lang.Specification

import static org.kohsuke.github.GHIssueState.CLOSED

class DraftReleaseNotesTaskSpecification extends Specification {
    def 'should be able to add the task to a project'() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        def task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)

        then:
        task instanceof DraftReleaseNotesTask
    }

    @Ignore('Functional')
    def 'should get a milestone ID corresponding to the release number'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")

        when:
        def milestoneNumber = task.getMilestoneNumber(repository, 'post-1.0', GHIssueState.OPEN)

        then:
        //this is a real milestone and this is its actual ID
        milestoneNumber == 10
    }

    @Ignore('Functional')
    def 'should throw an exception if the release does not have a milestone in github or is closed'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")

        when:
        GHMilestone milestone = task.getMilestoneNumber(repository, '0.108', GHIssueState.OPEN)

        then:
        thrown IllegalArgumentException
    }

    @Ignore('Functional')
    def 'should be able to specify that expected milestone is closed'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")

        when:
        def milestoneNumber = task.getMilestoneNumber(repository, '0.108', CLOSED)

        then:
        milestoneNumber == 9
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
        def issues = task.getIssuesAsMapOfEnhancementsAndBugs(repository, release108Milestone.number)

        then:
        issues != null
        issues.enhancement.size + issues.bug.size == release108Milestone.closedIssues
    }

    @Ignore('Functional')
    def 'create a default template for release notes'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")
        def releaseVersion = 'post-1.0'

        // add some arbitrary (real) issues to see them populated in the release notes
        def enhancement1 = repository.getIssue(620)
        def enhancement2 = repository.getIssue(609)
        def bug1 = repository.getIssue(599)
        def issues = [enhancements: [enhancement1, enhancement2], bugs: [bug1]]
        def date = new Date()

        when:
        def notes = task.createDraftReleaseNotesContent(repository, releaseVersion, issues, date)

        then:
        notes != null
        notes == "\n## Version post-1.0 (${date.format("MMM dd, yyyy")})\n\n" +
        "### Downloads\n" +
        "Below and on maven central.\n\n" +
        "### Docs\n" +
        "https://rawgithub.com/wiki/mongodb/morphia/javadoc/post-1.0/index.html\n\n" +
        "### Issues Resolved\n" +
        "#### ENHANCEMENTS\n" +
        "* [Issue 620](https://github.com/mongodb/morphia/pull/620): Moved around a chunk of tests\n" +
        "* [Issue 609](https://github.com/mongodb/morphia/issues/609): Support ID generator so users can specify the behaviour when an ID" +
        " is not supplied\n" +
        "\n" +
        "#### BUGS\n" +
        "* [Issue 599](https://github.com/mongodb/morphia/issues/599): QueryImpl usage potentially unsafe with cursor management\n\n"
    }

    @Ignore('Functional')
    def 'should save the release notes to Github as a draft'() {
        //note: functional test that needs a connection to github and a real milestone
        given:
        Project project = ProjectBuilder.builder().build()
        DraftReleaseNotesTask task = project.task('draftReleaseNotes', type: DraftReleaseNotesTask)
        task.releaseVersion = '0.108'
        task.expectedMilestoneState = CLOSED
        GHRepository repository = GitHub.connect().getRepository("mongodb/morphia")

        expect:
        task.draftReleaseNotes();

        //not a great test, there are no assertions.  It's a fully functional test, so go to
        // https://github.com/mongodb/morphia/releases
        //to see if the draft release notes have been posted with the text, the issues, and the jars for the release
    }

}
