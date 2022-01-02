package dev.morphia

import org.asciidoctor.Asciidoctor.Factory
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.MethodHolder
import org.jboss.forge.roaster.model.source.MethodSource
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.text.NumberFormat
import kotlin.system.exitProcess

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

    fun audit(name: String, url: String, filter: List<String> = listOf()): Int {
        val doc = Jsoup.parse(URL(url), 30000)
        val operators = doc
            .select("code")
            .distinctBy { it.text() }
            .map { it.text() }
            .filter { it !in filter && it.startsWith('$') }
            .sorted()
        if (operators.isEmpty()) {
            throw IllegalStateException("No operators found for $url.")
        }
        val map = operators.map {
            it to methods[it]?.let { list -> impls(list) }
        }
        val remaining = map.filter { it.second == null }
        val done = map.filter { it.second != null }
        val percent = NumberFormat.getPercentInstance().format(1.0 * done.size / operators.size)
        var document = """
            = $url
            
            .${name}
            [cols="e,a"]
            |===
            |Operator Name ($percent of ${operators.size} complete. ${remaining.size} remain)|Implementation
            """.trimIndent()

        document += writeImpls(remaining)
        document += writeImpls(done)

        document += "\n|==="
        val asciidoctor = Factory.create()

        File("target/${name}.html").writeText(asciidoctor.convert(document, mapOf()))
        File("target/${name}.adoc").writeText(document)

        asciidoctor.shutdown()
        if (remaining.isNotEmpty()) {
            println("source: $url")
            println("missing items:  ${name}:  ${remaining.map { pair -> pair.first }}")
        }
        return remaining.size
    }

    private fun writeImpls(operators: List<Pair<String, String?>>): String {
        var document1 = ""
        for (operator in operators) {
            document1 += """
                    
                |${operator.first}
                |${operator.second ?: ""}
                
                """.trimIndent()
        }
        return document1
    }

    private fun impls(list: List<MethodSource<*>>): String {
        return list.joinToString("\n") { method ->
            method.removeJavaDoc()
            method.removeAllAnnotations()
            val signature = method.toString().substringBefore("{").trim()
            ". ${signature} [_${method.origin.name}_]"
        }
    }
}

fun main() {
    var remaining = 0

    remaining += OperationAudit
        .parse(taglet = "@query.filter")
        .audit(
            "query-filters", "https://docs.mongodb.com/manual/reference/operator/query/",
            listOf("$", "\$rand")
        )

    remaining += OperationAudit
        .parse(taglet = "@update.operator")
        .audit(
            "update-operators", "https://docs.mongodb.com/manual/reference/operator/update/",
            listOf("$", "$[]", "$[<identifier>]", "\$position", "\$slice", "\$sort")
        )

    remaining += OperationAudit
        .parse(taglet = "@aggregation.expression")
        .audit(
            "aggregation-pipeline", "https://docs.mongodb.com/manual/reference/operator/aggregation-pipeline",
            listOf(
                "$", "\$listSessions", "\$listLocalSessions",
                "\$search" /* not terribly well doc'd.  atlas only? */
            )
        )

    remaining += OperationAudit
        .parse(taglet = "@aggregation.expression")
        .audit(
            "aggregation-expressions", "https://docs.mongodb.com/manual/reference/operator/aggregation/index.html",
            listOf("$", "\$addFields", "\$group", "\$project", "\$set")
        )

    exitProcess(remaining)
}
