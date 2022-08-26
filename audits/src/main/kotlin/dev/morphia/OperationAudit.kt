package dev.morphia

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.MethodHolder
import org.jboss.forge.roaster.model.source.MethodSource
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import org.kohsuke.github.GHIssueState.OPEN
import org.kohsuke.github.GitHubBuilder
import java.io.File
import java.io.FileReader
import java.net.URL

private val core = File("../core/src/main/java")

class OperationAudit(var methods: Map<String, List<MethodSource<*>>>) {
    companion object {
        fun parse(taglet: String): OperationAudit {
            return OperationAudit(core.walkBottomUp()
                .filter { it.extension == "java" }
                .map { Roaster.parse(JavaType::class.java, it) }
                .filterIsInstance<MethodHolder<*>>()
                .flatMap { it.methods }
                .filterIsInstance<MethodSource<*>>()
                .filter { it.javaDoc.tagNames.contains(taglet) }
                .groupBy { it.javaDoc.getTags(taglet)[0].value.substringAfter(" ") })
        }
    }

    val github by lazy {
        GitHubBuilder
            .fromPropertyFile(System.getProperty("GITHUB_PROPERTIES") ?: "github.properties")
            .build()
            .getRepository("MorphiaOrg/Morphia")
    }
    val issues by lazy { github.getIssues(OPEN) }
    val milestone by lazy {
        val pom = MavenXpp3Reader().read(FileReader("pom.xml"))
        val releaseVersion: String = (pom.version ?: pom.parent.version)
            .replace("-SNAPSHOT", "")
        github.listMilestones(OPEN).first { it.title == releaseVersion }
    }

    fun audit(name: String, url: String, excludes: List<String> = listOf()): Pair<Int, Int> {
        val remaining = Jsoup
            .parse(URL(url), 30000)
            .select(object : Evaluator() {
                override fun matches(root: Element, element: Element) = element.normalName() == "code" &&
                    element.childrenSize() == 0 &&
                    element.text().startsWith("$") && !element.text().contains(",")
            })
            .map { it.text() }
            .ifEmpty { throw IllegalStateException("No operators found for $url.") }
            .sorted()
            .distinctBy { it }
            .filter {
                it !in excludes && methods[it] == null
            }
        val docRoot = url.replace("/index.html", "")
        var issuesCreated = 0
        if (remaining.isNotEmpty()) {
            val enhancement = github.getLabel("enhancement")
            val labels = mutableListOf(enhancement)
            val targetLabel = if (name.contains("aggregation")) {
                val label = github.getLabel("aggregation")
                labels += label
                label
            } else null

            remaining.forEach {
                val title = "Implement $it"
                val ifEmpty = issues
                    .filter { issue -> issue.title == title }
                    .filter { issue -> targetLabel == null || issue.labels.contains(targetLabel) }

                if (ifEmpty.isEmpty()) {
                    issuesCreated++
                    val body = url
                    println(
                        """
                            Creating a new issue for $name:
                               Title: '$title'
                               Milestone: ${milestone.title}
                               Labels: ${labels.joinToString { label -> label.name }}
                               Body: $body
                               """.trimIndent()
                    )
                    val builder = github.createIssue(title)
                        .milestone(milestone)
                        .body(body)

                    labels.forEach { builder.label(it.name) }
                    builder.create()
                }
            }
        }
        return remaining.size to issuesCreated
    }
}
