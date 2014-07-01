package org.mongodb.morphia.release

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.spockframework.util.Assert.fail

class ReleaseTaskSpecification extends Specification {
    def 'should be able to add the release task to the project'() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        def task = project.task('release', type: ReleaseTask)

        then:
        task instanceof ReleaseTask
    }

    def 'should update the gradle build file from the snapshot release version to the real one'() {
        // uses real file manipulation, and currently requires a rollback to it as well
        given:
        def locationOfTestBuildFile = new File('src/test/resources')
        Project project = ProjectBuilder.builder().withProjectDir(locationOfTestBuildFile).build()
        ReleaseTask task = project.task('release', type: ReleaseTask)
        task.releaseVersion = '0.109'

        when:
        task.updateVersionInBuildFile()

        then:
        def buildFilePattern = ~/build\.gradle/
        boolean matched = false
        locationOfTestBuildFile.eachFileMatch(buildFilePattern) { file ->
            file.eachLine {
                ln -> if ( ln =~ '-SNAPSHOT\'$' ) {
                    fail("Did not remove the SNAPSHOT suffix")
                } else if ( ln =~ '0.109\'$' ) {
                    matched = true
                }
            }
        }
        matched == true
    }

//    def 'should commit the changes to git'() {
//        given:
//        def locationOfTestBuildFile = new File('src/test/resources')
//        Project project = ProjectBuilder.builder().withProjectDir(locationOfTestBuildFile).build()
//        ReleaseTask task = project.task('release', type: ReleaseTask)
//        task.releaseVersion = 'Test'
//
//        when:
//        task.prepareGitForRelease();
//
//        then:
//        fail 'Not implemented yet'
//
//    }
}
