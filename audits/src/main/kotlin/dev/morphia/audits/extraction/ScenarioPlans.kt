package dev.morphia.audits.extraction

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.CapabilityKey
import dev.morphia.audits.coverage.CapabilityRef
import dev.morphia.audits.coverage.OperatorContext
import dev.morphia.audits.documents.DocumentNode
import dev.morphia.audits.documents.DocumentPage
import dev.morphia.audits.documents.DocumentReport
import dev.morphia.audits.naming.ExampleDestination
import dev.morphia.audits.naming.ScenarioScope
import dev.morphia.audits.scenarios.ScenarioExtractionPlan
import dev.morphia.audits.scenarios.ScenarioPageRule
import dev.morphia.audits.scenarios.SectionAssemblyPolicy
import dev.morphia.audits.scenarios.SectionLayout
import dev.morphia.audits.semantics.ResultOrder
import dev.morphia.audits.shell.ShellDialect

/**
 * An operator page as its parsed source declares it: the `.. expression:: $add` target gives the
 * operator's [token] and [kind] (`expression`, `group`, `pipeline`, `query` or `update`). [context]
 * is the legacy package Morphia's tests keep it in, and [operator] the page's file name.
 */
data class OperatorPage(val page: DocumentPage, val kind: String, val token: String) {
    val context: String = CONTEXTS.getValue(kind)
    val operator: String = page.id.substringAfterLast('/').removeSuffix(".txt")
    val id: String = "$context/$operator"

    companion object {
        val CONTEXTS =
            mapOf(
                "expression" to "aggregation/expressions",
                // Accumulator and window operator pages; Morphia's tests keep them with the
                // expressions.
                "group" to "aggregation/expressions",
                "pipeline" to "aggregation/stages",
                "query" to "query/filters",
                "update" to "query/updates",
            )
    }
}

/**
 * Drafts the scenario extraction plan, `audits/catalog/scenario-plan.json`, from the parsed
 * operator pages. The plan is reviewed policy: a draft is committed only after its diff is read,
 * and the inventory extracts only the pages it names.
 */
object ScenarioPlans {
    const val PATH = "audits/catalog/scenario-plan.json"

    private val SCOPES =
        mapOf(
            "expression" to OperatorContext.AGGREGATION_EXPRESSION,
            "group" to OperatorContext.GROUP_ACCUMULATOR,
            "pipeline" to OperatorContext.PIPELINE_STAGE,
            "query" to OperatorContext.QUERY_PREDICATE,
            "update" to OperatorContext.UPDATE_OPERATOR,
        )
    private val REFERENCE_LANGUAGES = listOf("javascript", "shell")
    private val REFERENCE_TABS = listOf("shell")
    private val FOREIGN_LANGUAGES =
        listOf(
            "csharp",
            "python",
            "java",
            "go",
            "rust",
            "kotlin",
            "php",
            "ruby",
            "swift",
            "c",
            "cpp",
            // Pages show a query's SQL equivalent beside the shell.
            "sql",
        )
    private val FOREIGN_TABS =
        listOf(
            "csharp",
            "nodejs",
            "python",
            "java-sync",
            "go",
            "php",
            "ruby",
            "rust",
            "kotlin",
            "swift",
            "c",
            "cpp",
            "motor",
            "scala",
            "perl",
            "compass",
        )

    /** The pages of [document] that declare one operator, with the declaration. */
    fun operatorPages(document: DocumentReport): List<OperatorPage> =
        document.pages.mapNotNull { page ->
            val tree = page.expanded ?: return@mapNotNull null
            val target =
                descendants(tree).firstOrNull {
                    it.kind == "target" && it.domain == "mongodb" && it.name in SCOPES
                } ?: return@mapNotNull null
            val token =
                descendants(target)
                    .firstOrNull { it.kind == "target_identifier" }
                    ?.let(::text)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            OperatorPage(page, target.name!!, token)
        }

    /** A rule for each operator page with an examples section; the others have nothing to run. */
    fun draft(document: DocumentReport, catalogSha256: String): ScenarioExtractionPlan =
        ScenarioExtractionPlan(
            catalogSha256 = catalogSha256,
            pages = operatorPages(document).sortedBy { it.id }.mapNotNull { rule(it) },
        )

    private fun rule(operator: OperatorPage): ScenarioPageRule? {
        val tree = operator.page.expanded!!
        val examples =
            sections(tree).firstOrNull { section ->
                anchor(section).let {
                    it == "examples" ||
                        it == "example" ||
                        it.startsWith("example") && it.length < 12
                }
            } ?: return null
        val anchor = anchor(examples)
        val codes = descendants(tree).filter { it.kind == "code" }
        val tabs =
            descendants(tree)
                .filter { it.kind == "directive" && it.name == "tab" }
                .mapNotNull { tab -> tab.options["tabid"]?.let(::textValue) }
                .toSet()
        val languages = codes.mapNotNull { it.attributes["lang"]?.let(::textValue) }.toSet()
        return ScenarioPageRule(
            id = operator.id,
            page = operator.page.id,
            scope =
                ScenarioScope(
                    CapabilityRef(
                        CapabilityKey("mongodb", SCOPES.getValue(operator.kind), operator.token),
                        null,
                    ),
                    "mongosh",
                ),
            destination =
                ExampleDestination(
                    "Test" +
                        operator.operator.split('-').joinToString("") { part ->
                            part.replaceFirstChar { it.uppercase() }
                        },
                    "dev/morphia/test/generated/${operator.id}",
                ),
            sections = listOf(anchor),
            excludedSections = emptyList(),
            minimumReferenceGroups = 1,
            referenceLanguages = REFERENCE_LANGUAGES,
            referenceTabs = REFERENCE_TABS,
            foreignLanguages = FOREIGN_LANGUAGES,
            foreignTabs = FOREIGN_TABS,
            dialect = ShellDialect.MONGOSH,
            resultOrder = ResultOrder.ORDERED,
            sectionAssembly =
                SectionAssemblyPolicy(
                    SectionLayout.COMMANDS_THEN_RESULT,
                    // An examples section with its own code ahead of its subsections shares it;
                    // without subsections, its code is the example.
                    sharedSetupSections =
                        if (ownCode(examples) && sections(examples).count() > 1) listOf(anchor)
                        else emptyList(),
                    // Tabs other than the shell and the drivers are variants of one example, such
                    // as radians and degrees.
                    scenarioTabs = (tabs - FOREIGN_TABS.toSet() - REFERENCE_TABS.toSet()).sorted(),
                    // Pages that highlight shell code as bash or sh.
                    additionalShellLanguages = (languages intersect setOf("bash", "sh")).sorted(),
                    unlabeledLanguage = "javascript",
                ),
        )
    }

    private fun descendants(node: DocumentNode): Sequence<DocumentNode> = sequence {
        yield(node)
        node.children.forEach { yieldAll(descendants(it)) }
    }

    private fun sections(node: DocumentNode) = descendants(node).filter { it.kind == "section" }

    private fun anchor(section: DocumentNode) =
        section.children
            .firstOrNull { it.kind == "heading" }
            ?.attributes
            ?.get("id")
            ?.let(::textValue)
            .orEmpty()

    /** Code a section shows itself, outside its subsections, tabs and input/output groups. */
    private fun ownCode(section: DocumentNode): Boolean {
        fun own(node: DocumentNode): Boolean =
            node.kind == "code" ||
                node.kind != "section" &&
                    !(node.kind == "directive" &&
                        node.name in setOf("io-code-block", "tabs", "tab")) &&
                    node.children.any(::own)
        return section.children.any(::own)
    }

    private fun text(node: DocumentNode): String =
        node.text ?: node.children.joinToString("") { text(it) }

    private fun textValue(json: String): String? =
        try {
            AuditJson.textValue(json)
        } catch (_: Exception) {
            null
        }
}
