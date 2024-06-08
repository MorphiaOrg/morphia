package dev.morphia.audits

import dev.morphia.audits.model.Operator
import java.io.FileReader
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.kohsuke.github.GHIssueState.OPEN
import org.kohsuke.github.GHLabel
import org.kohsuke.github.GitHubBuilder

object GithubProject {

    var dryRun = System.getenv()["GITHUB_ACTION"] == null
    val github by lazy {
        GitHubBuilder.fromEnvironment().build().getRepository("MorphiaOrg/Morphia")
    }
    val issues by lazy { github.getIssues(OPEN) }
    val milestone by lazy {
        val pom = MavenXpp3Reader().read(FileReader("pom.xml"))
        val releaseVersion: String = (pom.version ?: pom.parent.version).replace("-SNAPSHOT", "")
        github.listMilestones(OPEN).first { it.title == releaseVersion }
    }

    fun updateGH(
        type: String,
        unimplemented: List<Operator>,
        labelNames: List<String>,
    ): List<Operator> {
        if (dryRun) {
            println("*********************************************************")
            println("* This is a dry run. No changes will be made on GitHub. *")
            println("*********************************************************")
            return emptyList()
        } else {
            var created = listOf<Operator>()
            if (unimplemented.isNotEmpty()) {
                val labels: List<GHLabel> = labelNames.map { github.getLabel(it) }

                unimplemented.forEach {
                    val title = "Implement $type ${it.operator}"
                    val existing =
                        issues.filter { it.title == title }.filter { it.labels.containsAll(labels) }

                    if (existing.isEmpty()) {
                        created += it
                        if (!dryRun) {
                            github
                                .createIssue(title)
                                .milestone(milestone)
                                .body(it.url)
                                .also { builder -> labels.forEach { builder.label(it.name) } }
                                .create()
                        } else {
                            println("Missing ${it.name} operator.")
                        }
                    }
                }
            }
            return created
        }
    }
}
