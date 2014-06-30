package org.mongodb.morphia.release

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.kohsuke.github.GHMilestone
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
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

}
