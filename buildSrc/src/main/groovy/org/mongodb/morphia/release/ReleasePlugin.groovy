package org.mongodb.morphia.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment

//requires nexus plugin
class ReleasePlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.extensions.create('release', ReleasePluginExtension)
        project.evaluationDependsOnChildren()

        project.task('prepareRelease', type: PrepareReleaseTask, dependsOn: project.subprojects.clean)
        magicIncantationRequiredToGetNexusPluginWorkingOnSubprojects()
        project.task('draftReleaseNotes', type: DraftReleaseNotesTask, dependsOn: project.subprojects.uploadArchives)
        project.task('publishJavadoc', type: PublishJavadocTask, dependsOn: ['prepareRelease', project.subprojects.javadoc])
        project.task('updateToNextVersion', type: UpdateToNextVersionTask, dependsOn: 'publishJavadoc')
        project.task('release', dependsOn: 'updateToNextVersion')
    }

    def magicIncantationRequiredToGetNexusPluginWorkingOnSubprojects() {
        // feels like most of this shouldn't be needed as the repos etc are defaults, but they are required when you 
        // add this as a dependency to a top-level project instead of just running it alone
        project.subprojects { subproject ->
            apply plugin: 'nexus'

            project.ext.mavenDeployers = []

            def pom = { config ->
                project.mavenDeployers*.pom config
            }

            install {
                project.mavenDeployers << repositories.mavenInstaller
            }

            uploadArchives {
                project.mavenDeployers << repositories.mavenDeployer {
                    def nexusCredentials = [
                            userName: project.properties.nexusUsername,
                            password: project.properties.nexusPassword
                    ]

                    snapshotRepository(url: 'https://oss.sonatype.org/content/repositories/snapshots') {
                        authentication(nexusCredentials)
                    }
                    repository(url: 'https://oss.sonatype.org/service/local/staging/deploy/maven2/') {
                        authentication(nexusCredentials)
                    }
                }
                repositories {
                    mavenDeployer {
                        beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
                    }
                }
            }.dependsOn(this.project.prepareRelease)

            modifyPom {
                project {
                    name = 'Morphia'
                    description = 'Java Object Document Mapper for MongoDB'
                    url 'https://github.com/mongodb/morphia.git'
                    version = project.release.releaseVersion

                    scm {
                        url 'https://github.com/mongodb/morphia.git'
                        connection 'scm:git:https://github.com/mongodb/morphia.git'
                    }

                    licenses {
                        license {
                            name 'The Apache Software License, Version 2.0'
                            url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            distribution 'repo'
                        }
                    }

                    developers {
                        developer {
                            name 'Various'
                            organization = 'MongoDB'
                        }
                    }
                }

                whenConfigured { resultPom ->
                    resultPom.dependencies.removeAll { dep -> dep.scope != 'compile' }
                    resultPom.dependencies*.scope = null
                }
            }
        }
    }
}

