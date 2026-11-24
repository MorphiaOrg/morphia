package dev.morphia.audits.scenarios

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.ContractValidation
import dev.morphia.audits.coverage.OperatorTokens
import dev.morphia.audits.documents.*
import dev.morphia.audits.naming.*
import dev.morphia.audits.shell.*
import dev.morphia.audits.sources.UnresolvedSource
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import javax.lang.model.SourceVersion

/** Reviewed native IO/section ownership; no JavaScript evaluation or source mutation. */
object ScenarioExtraction {
    const val VERSION = "native-sections:1"
    private val digest = Regex("[0-9a-f]{64}")
    private val shellLabels = setOf("javascript", "js", "shell", "mongosh")
    private val foreignLabels =
        setOf(
            "c",
            "c++",
            "cpp",
            "c#",
            "cs",
            "csharp",
            "clojure",
            "dart",
            "elixir",
            "erlang",
            "go",
            "golang",
            "groovy",
            "haskell",
            "java",
            "javascript/nodejs",
            "kotlin",
            "lua",
            "node",
            "nodejs",
            "objective-c",
            "perl",
            "php",
            "py",
            "python",
            "r",
            "ruby",
            "rust",
            "scala",
            "sql",
            "swift",
            "ts",
            "typescript",
        )

    fun extract(
        bundle: VerifiedSourceBundle,
        document: DocumentReport,
        plan: ScenarioExtractionPlan,
        limits: ScenarioExtractionLimits = ScenarioExtractionLimits(),
    ): ScenarioExtractionReport {
        validate(plan)
        require(document.sourceSha256 == bundle.manifest.contentSha256)
        // Bound the owned tree before the existing origin validator traverses it.
        var nodes = 0
        val pending = ArrayDeque<Pair<DocumentNode, Int>>()
        document.pages.forEach { page ->
            listOfNotNull(page.raw, page.expanded).forEach { pending.add(it to 0) }
        }
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            require(++nodes <= limits.maximumNodes && depth <= limits.maximumDepth) {
                "Document exceeds scenario AST work/depth budget"
            }
            (node.children + node.arguments).forEach { pending.add(it to depth + 1) }
        }
        DocumentParsing.validateOrigins(document, bundle)
        require(
            document.inputSha256 ==
                AuditJson.hash(DocumentParsing.export(bundle, document.pages.map { it.id }))
        ) {
            "Document parse-input pin mismatch"
        }
        require(
            document.parser.engine == "snooty" &&
                document.parser.revision == DocumentParsing.PARSER_REVISION
        )
        val canonical =
            plan.copy(
                pages =
                    plan.pages
                        .sortedBy { it.id }
                        .map { rule ->
                            rule.copy(
                                sections = rule.sections.sorted(),
                                excludedSections = rule.excludedSections.sorted(),
                                referenceLanguages = rule.referenceLanguages.sorted(),
                                referenceTabs = rule.referenceTabs.sorted(),
                                foreignLanguages = rule.foreignLanguages.sorted(),
                                foreignTabs = rule.foreignTabs.sorted(),
                                sectionAssembly =
                                    rule.sectionAssembly?.let {
                                        it.copy(
                                            scenarioTabs = it.scenarioTabs.sorted(),
                                            additionalShellLanguages =
                                                it.additionalShellLanguages.sorted(),
                                        )
                                    },
                            )
                        }
            )
        return Run(bundle, document, canonical, limits).run()
    }

    /** Refuse partial-scope reconciliation: it could retire names for unextracted examples. */
    fun namingRequest(
        report: ScenarioExtractionReport,
        occupied: List<OccupiedName>,
        reviewedNames: List<ReviewedName> = emptyList(),
        retirements: List<ReviewedRetirement> = emptyList(),
        completeScopes: List<ScenarioScope> = report.plan.pages.map { it.scope }.distinct(),
    ): NamingRequest {
        require(
            completeScopes.isNotEmpty() && completeScopes.distinct().size == completeScopes.size
        )
        val rules = report.plan.pages.filter { it.scope in completeScopes }
        require(completeScopes.all { scope -> rules.any { it.scope == scope } })
        require(
            rules.all { rule ->
                report.pages.singleOrNull { it.rule == rule.id }?.namingComplete == true
            }
        ) {
            "Incomplete source grouping cannot establish complete naming scopes"
        }
        return NamingRequest(
            catalogSha256 = report.plan.catalogSha256,
            completeScopes = completeScopes,
            scenarios =
                report.scenarios.mapNotNull { it.naming }.filter { it.scope in completeScopes },
            occupied = occupied,
            reviewedNames = reviewedNames,
            retirements = retirements,
        )
    }

    private fun validate(plan: ScenarioExtractionPlan) {
        require(
            plan.schemaVersion == 1 &&
                digest.matches(plan.catalogSha256) &&
                plan.pages.size in 1..1024
        )
        require(plan.pages.map { it.id }.distinct().size == plan.pages.size)
        require(plan.pages.map { it.page to it.scope }.distinct().size == plan.pages.size)
        plan.pages.forEach { p ->
            require(
                p.id.isNotBlank() &&
                    p.id.length <= 256 &&
                    ContractValidation.relative(p.page) &&
                    p.page.endsWith(".txt")
            )
            require(
                p.scope.capability.key.product.matches(Regex("[a-z][a-z0-9-]*")) &&
                    OperatorTokens.valid(p.scope.capability.key.token)
            )
            require(
                p.scope.capability.facet == null ||
                    p.scope.capability.facet.matches(Regex("[a-z][a-z0-9-]*"))
            )
            require(p.scope.language.matches(Regex("[a-z][a-z0-9-]*")))
            require(
                SourceVersion.isName(p.destination.testClass) &&
                    ContractValidation.relative(p.destination.resourceDirectory)
            )
            require(p.sections.isNotEmpty() && p.minimumReferenceGroups > 0)
            require(
                p.referenceLanguages.isNotEmpty() && p.referenceLanguages.all { it in shellLabels }
            )
            require(
                p.foreignLanguages.all { it.lowercase(java.util.Locale.ROOT) in foreignLabels }
            ) {
                "Foreign language policies require a known programming-language label"
            }
            require(p.referenceLanguages.intersect(p.foreignLanguages.toSet()).isEmpty())
            require(p.referenceTabs.intersect(p.foreignTabs.toSet()).isEmpty())
            require(
                listOf(
                        p.sections,
                        p.excludedSections,
                        p.referenceLanguages,
                        p.referenceTabs,
                        p.foreignLanguages,
                        p.foreignTabs,
                    )
                    .all { xs -> xs.distinct().size == xs.size && xs.none { it.isBlank() } }
            )
            require(p.sections.intersect(p.excludedSections.toSet()).isEmpty())
            p.sectionAssembly?.let { policy ->
                require(policy.sharedSetupSections.size <= 64 && policy.scenarioTabs.size <= 64)
                require(
                    listOf(
                            policy.sharedSetupSections,
                            policy.scenarioTabs,
                            policy.additionalShellLanguages,
                        )
                        .all { xs ->
                            xs.distinct().size == xs.size &&
                                xs.all { it.isNotBlank() && it.length <= 256 }
                        }
                )
                require(policy.additionalShellLanguages.all { it in setOf("bash", "sh") })
                require(
                    policy.unlabeledLanguage == null ||
                        policy.unlabeledLanguage in p.referenceLanguages
                )
                require(policy.scenarioTabs.all { it.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]*")) })
                require(
                    policy.scenarioTabs
                        .intersect((p.referenceTabs + p.foreignTabs).toSet())
                        .isEmpty()
                )
                require(policy.sharedSetupSections.intersect(p.excludedSections.toSet()).isEmpty())
            }
        }
    }

    // Commands that change the data a later read sees.
    private val write =
        Regex(
            """\.(?:update|replace|delete|bulkWrite|findOneAnd|findAndModify)\w*\s*\(|[{,]\s*["']?\$(?:merge|out)["']?\s*:"""
        )

    private class Run(
        val bundle: VerifiedSourceBundle,
        val document: DocumentReport,
        val plan: ScenarioExtractionPlan,
        val limits: ScenarioExtractionLimits,
    ) {
        val problems = mutableListOf<ScenarioExtractionProblem>()
        val scenarios = mutableListOf<ExtractedScenario>()
        val pages = mutableListOf<ExtractedScenarioPage>()
        var characters = 0L
        var visitedNodes = 0
        var metadataCharacters = 0L
        var programBytes = 0L

        fun visit() {
            require(++visitedNodes <= limits.maximumNodes) {
                "Scenario expansion exceeds work budget"
            }
        }

        fun metadata(size: Int) {
            metadataCharacters += size
            require(metadataCharacters <= limits.maximumMetadataCharacters) {
                "Scenario metadata exceeds character budget"
            }
        }

        var groups = 0
        val prefix =
            bundle.manifest.request.projectRoot.takeUnless { it == "." }?.let { "$it/source/" }
                ?: "source/"

        fun problem(code: String, rule: ScenarioPageRule, message: String, id: String? = null) {
            problems += ScenarioExtractionProblem(code, rule.id, id, message)
        }

        fun run(): ScenarioExtractionReport {
            for (rule in plan.pages) page(rule)
            // A section label names its whole section, not every individual IO pair inside it.
            // Use it as a scenario identity only when exactly one group owns that leaf section.
            val owners =
                scenarios
                    .filter { it.variant == ScenarioVariant.REFERENCE }
                    .groupBy { listOf(it.rule, it.sections.lastOrNull()) }
            scenarios.replaceAll { candidate ->
                val labels =
                    (candidate.explicitLabels +
                            if (
                                owners[listOf(candidate.rule, candidate.sections.lastOrNull())]
                                    ?.size == 1 && candidate.sections.lastOrNull()?.leaf == true
                            )
                                candidate.sections.lastOrNull()?.explicitLabels.orEmpty()
                            else emptyList())
                        .distinct()
                        .sorted()
                val peers =
                    owners[listOf(candidate.rule, candidate.sections.lastOrNull())].orEmpty()
                val titles =
                    peers.map { snippetTitle(it.blocks)?.takeUnless(DescriptiveNames::isGeneric) }
                val namedSubexample =
                    if (
                        peers.size > 1 &&
                            titles.none { it == null } &&
                            titles.distinct().size == titles.size
                    )
                        snippetTitle(candidate.blocks)
                    else null
                val naming =
                    candidate.naming?.let { naming ->
                        naming.copy(
                            trace =
                                naming.trace.copy(
                                    displayTitle = namedSubexample ?: naming.trace.displayTitle,
                                    location = naming.trace.location.copy(explicitLabels = labels),
                                )
                        )
                    }
                val location = naming?.trace?.location
                if (
                    location != null &&
                        location.explicitLabels.isEmpty() &&
                        location.snippetIds.isEmpty() &&
                        (location.autoAnchor == null || location.enclosingContext == null)
                ) {
                    problems +=
                        ScenarioExtractionProblem(
                            "MISSING_IDENTITY",
                            candidate.rule,
                            candidate.id,
                            "No explicit label, stable snippet or anchor/context identity",
                        )
                    candidate.copy(naming = null)
                } else candidate.copy(naming = naming)
            }
            val incomplete =
                scenarios
                    .filter { it.variant == ScenarioVariant.REFERENCE && it.naming == null }
                    .map { it.rule }
                    .toSet()
            pages.replaceAll { if (it.rule in incomplete) it.copy(namingComplete = false) else it }

            val duplicateIds = scenarios.groupBy { it.id }.filterValues { it.size > 1 }.keys
            duplicateIds.forEach { id ->
                scenarios
                    .filter { it.id == id }
                    .forEach { s ->
                        problems +=
                            ScenarioExtractionProblem(
                                "AMBIGUOUS_GROUP",
                                s.rule,
                                id,
                                "Identical group handles require explicit source identity; encounter order cannot resolve them",
                            )
                    }
            }
            if (duplicateIds.isNotEmpty()) {
                val affected = scenarios.filter { it.id in duplicateIds }.map { it.rule }.toSet()
                pages.replaceAll {
                    if (it.rule in affected) it.copy(namingComplete = false) else it
                }
            }
            val ready = pages.all { it.namingComplete }
            val report =
                ScenarioExtractionReport(
                    normalizer = VERSION,
                    sourceSha256 = document.sourceSha256,
                    documentInputSha256 = document.inputSha256,
                    parser = document.parser,
                    plan = plan,
                    planSha256 = AuditJson.hash(plan),
                    state =
                        if (ready) ScenarioExtractionState.READY
                        else ScenarioExtractionState.PARTIAL,
                    pages = pages.toList(),
                    scenarios = scenarios.sortedBy { it.id },
                    diagnostics = document.diagnostics,
                    problems =
                        problems
                            .distinct()
                            .sortedWith(
                                compareBy(
                                    { it.rule },
                                    { it.candidate },
                                    { it.code },
                                    { it.message },
                                )
                            ),
                )
            AuditJson.requireMaximumSize(report, limits.maximumReportBytes)
            return report
        }

        /** The language (or other) choices a selected-content block is shown for. */
        fun selections(container: ScenarioContainer): List<String?> =
            container.attributes["selections"]?.let {
                try {
                    AuditJson.textValues(it).ifEmpty { listOf(null) }
                } catch (_: Exception) {
                    listOf(null)
                }
            } ?: listOf(null)

        fun value(values: Map<String, String>, key: String): String? =
            values[key]?.let {
                try {
                    AuditJson.textValue(it)
                } catch (_: Exception) {
                    null
                }
            }

        fun text(root: DocumentNode): String {
            val out = StringBuilder()
            val todo = ArrayDeque<DocumentNode>()
            todo.add(root)
            var work = 0
            while (todo.isNotEmpty()) {
                val node = todo.removeLast()
                visit()
                require(++work <= limits.maximumNodes) { "Text expansion budget exceeded" }
                if (node.text != null) out.append(node.text)
                else node.children.asReversed().forEach(todo::add)
                require(out.length <= 65_536) { "Context text exceeds budget" }
            }
            metadata(out.length)
            return out.toString()
        }

        fun container(node: DocumentNode): ScenarioContainer {
            (node.options.entries + node.attributes.entries).forEach {
                metadata(it.key.length + it.value.length)
            }
            return ScenarioContainer(
                node.kind,
                node.name,
                node.domain,
                node.arguments.joinToString("") { text(it) },
                node.options.toMap(),
                node.attributes.toMap(),
                node.origin,
            )
        }

        fun labels(node: DocumentNode): List<String> =
            if (node.kind == "target" && node.domain == "std" && node.name == "label")
                (node.identifiers +
                        node.children
                            .filter { it.kind == "target_identifier" }
                            .flatMap { it.identifiers })
                    .distinct()
            else emptyList()

        fun leaf(node: DocumentNode): Boolean {
            val todo = ArrayDeque<DocumentNode>()
            (node.children + node.arguments).forEach(todo::add)
            while (todo.isNotEmpty()) {
                visit()
                val child = todo.removeLast()
                if (child.kind == "section") return false
                (child.children + child.arguments).forEach(todo::add)
            }
            return true
        }

        fun section(node: DocumentNode): ScenarioSection {
            val headings = node.children.filter { it.kind == "heading" }
            val heading = headings.singleOrNull()
            return ScenarioSection(
                heading?.let(::text).orEmpty(),
                heading?.let { value(it.attributes, "id") },
                node.origin,
                leaf = leaf(node),
            )
        }

        fun selected(rule: ScenarioPageRule, sections: List<ScenarioSection>) =
            (sections.any { it.anchor in rule.sections } ||
                sections.lastOrNull()?.anchor in
                    rule.sectionAssembly?.sharedSetupSections.orEmpty()) &&
                sections.none { it.anchor in rule.excludedSections }

        fun variant(
            rule: ScenarioPageRule,
            code: List<ScenarioCode>,
            containers: List<ScenarioContainer>,
        ): ScenarioVariant {
            val transparent =
                setOf(
                    "directive",
                    "paragraph",
                    "list",
                    "listItem",
                    "line",
                    "line_block",
                    "definitionList",
                    "definitionListItem",
                    "directive_argument",
                    "literal",
                    "reference",
                    "emphasis",
                    "strong",
                )
            val directives =
                setOf(
                    "io-code-block",
                    "input",
                    "output",
                    "tabs",
                    "tab",
                    "include",
                    "literalinclude",
                    "versionadded",
                    "versionchanged",
                    "deprecated",
                    "note",
                    "important",
                    "tip",
                    "warning",
                    "seealso",
                    "example",
                    "admonition",
                    "procedure",
                    "step",
                    "steps",
                    "list-table",
                    "composable-tutorial",
                    "selected-content",
                    // Shared content a page pulls in from an include.
                    "extract",
                )
            // A composable tutorial's selected content names its selection the way a tab does.
            val selections =
                containers
                    .filter { it.kind == "directive" && it.name == "selected-content" }
                    .flatMap { selections(it) }
            val tabs =
                containers
                    .filter {
                        it.kind == "directive" &&
                            it.name == "tab" &&
                            it.domain in setOf(null, "", "mongodb", "std")
                    }
                    .map { value(it.options, "tabid") } + selections
            if (tabs.any { it in rule.foreignTabs }) return ScenarioVariant.FOREIGN
            if (
                containers.any {
                    it.kind !in transparent ||
                        (it.kind == "directive" &&
                            (it.name !in directives ||
                                it.domain !in setOf(null, "", "mongodb", "std")))
                }
            )
                return ScenarioVariant.UNRESOLVED
            if (
                tabs.any {
                    it == null ||
                        it !in rule.referenceTabs + rule.sectionAssembly?.scenarioTabs.orEmpty()
                }
            )
                return ScenarioVariant.UNRESOLVED
            val languages =
                code
                    .filter { it.role in setOf(ProgramRole.ACTION, ProgramRole.SETUP) }
                    // JSON is data a page shows or seeds; `none` turns highlighting off.
                    .filter { it.language != "json" }
                    .map {
                        it.language?.takeUnless { l -> l == "none" }
                            ?: rule.sectionAssembly?.unlabeledLanguage
                    }
            if (languages.isEmpty() || languages.any { it == null })
                return ScenarioVariant.UNRESOLVED
            if (
                languages.all {
                    it in
                        rule.referenceLanguages +
                            rule.sectionAssembly?.additionalShellLanguages.orEmpty()
                }
            )
                return ScenarioVariant.REFERENCE
            if (languages.all { it in rule.foreignLanguages }) return ScenarioVariant.FOREIGN
            return ScenarioVariant.UNRESOLVED
        }

        fun snippet(node: ScenarioContainer, page: DocumentPage): ScenarioSnippet? {
            if (node.name !in setOf("input", "output", "literalinclude") || node.argument.isBlank())
                return null
            val raw = node.argument
            val owner = node.origin.includeChain.lastOrNull() ?: node.origin.path
            if (owner == null || raw.contains("://") || raw.any { it.code < 32 || it.code == 127 })
                return ScenarioSnippet(raw, null, null, null, null, node)
            val candidate =
                if (raw.startsWith('/')) prefix + raw.trimStart('/')
                else (Path.of(owner).parent ?: Path.of("")).resolve(raw).toString()
            val path = Path.of(candidate).normalize().joinToString("/") { it.toString() }
            if (!ContractValidation.relative(path))
                return ScenarioSnippet(raw, null, null, null, null, node)
            val source =
                try {
                    bundle.read(path)
                } catch (failure: UnresolvedSource) {
                    if (failure.problem.code == "SOURCE_DRIFT") throw failure
                    return ScenarioSnippet(raw, null, null, null, null, node)
                }
            val dependency =
                page.dependencies.filter {
                    it.origin.path == source.canonicalPath &&
                        it.origin.sha256 == source.resource.sha256
                }
            val markers =
                node.options.filterKeys {
                    it in setOf("start-after", "end-before", "start-at", "end-at")
                }
            val positional =
                node.options.keys.any { it in setOf("lines", "start-line", "end-line") }
            val identity =
                if (positional) null
                else
                    source.canonicalPath +
                        if (markers.isEmpty()) "" else "#markers:" + AuditJson.hash(markers)
            return ScenarioSnippet(
                raw,
                source.canonicalPath,
                source.resource.sha256,
                identity,
                dependency.mapNotNull { it.parserDigest }.distinct().singleOrNull(),
                node,
            )
        }

        fun code(
            node: DocumentNode,
            locator: List<Int>,
            context: List<ScenarioContainer>,
            page: DocumentPage,
        ): ScenarioCode {
            val source = requireNotNull(node.text) { "Code node lacks raw text" }
            characters += source.length
            require(characters <= limits.maximumCodeCharacters) {
                "Selected code exceeds character budget"
            }
            val roles =
                context
                    .mapNotNull {
                        when (it.name) {
                            "input" -> ProgramRole.ACTION
                            "output" -> ProgramRole.EXPECTED
                            else -> null
                        }
                    }
                    .distinct()
            return ScenarioCode(
                locator,
                source,
                sha(source),
                // The parser leaves code-block options after the language: `javascript\n:copyable:
                // true`.
                value(node.attributes, "lang")?.lineSequence()?.first()?.trim()?.ifEmpty { null },
                node.attributes,
                node.origin,
                roles.singleOrNull(),
                context,
                context.mapNotNull { snippet(it, page) },
            )
        }

        fun page(rule: ScenarioPageRule) {
            val page = document.pages.find { it.id == rule.page }
            if (page?.raw == null || page.expanded == null) {
                problem("MISSING_PAGE", rule, "Selected page needs raw and expanded owned trees")
                pages +=
                    ExtractedScenarioPage(
                        rule.id,
                        rule.page,
                        0,
                        0,
                        emptyList(),
                        0,
                        page?.dependencies.orEmpty(),
                        false,
                        emptyList(),
                        false,
                    )
                return
            }
            val before = problems.size
            val foundSections = mutableSetOf<String>()
            val unassigned = mutableListOf<ScenarioCode>()
            var unassignedReference = 0
            val start = scenarios.size
            data class Owner(
                val sections: List<ScenarioSection>,
                val containers: List<ScenarioContainer>,
                val source: DocumentOrigin,
                val blocks: MutableList<ScenarioCode> = mutableListOf(),
            )
            val owners = linkedMapOf<List<Int>, Owner>()
            val pendingIo = mutableListOf<() -> Unit>()
            // Setup that only native groups consumed, for the page-wide inheritance.
            val setupSources = mutableListOf<List<ScenarioCode>>()
            // Sections holding a native input/output group, and the setup their own code gives it.
            val ioSections = mutableSetOf<List<ScenarioSection>>()
            val sectionSetup = mutableMapOf<List<ScenarioSection>, List<ScenarioCode>>()
            val sectionSetupProblem = mutableMapOf<List<ScenarioSection>, String>()
            var sharedSetup = emptyList<ScenarioCode>()
            var sharedSetupSkipped = emptyList<SkippedCode>()
            var setupProblem: String? = null
            val ownedResources =
                (listOf(prefix + page.id) +
                        page.dependencies.flatMap {
                            listOfNotNull(it.origin.path) + it.origin.includeChain
                        })
                    .toMutableSet()
            fun walk(
                node: DocumentNode,
                locator: List<Int>,
                sections: List<ScenarioSection>,
                containers: List<ScenarioContainer>,
                attachedLabels: List<String> = emptyList(),
                owner: List<Int> = emptyList(),
            ) {
                visit()
                ownedResources += node.origin.includeChain
                node.origin.path?.let { ownedResources += it }
                val nextSections =
                    if (node.kind == "section")
                        sections + section(node).copy(explicitLabels = attachedLabels)
                    else sections
                if (node.kind == "section") nextSections.last().anchor?.let(foundSections::add)
                val nextContainers =
                    if (node.kind !in setOf("root", "section", "code")) containers + container(node)
                    else containers
                val nextOwner =
                    if (
                        node.kind == "section" ||
                            (node.kind == "directive" &&
                                node.name in setOf("tab", "selected-content"))
                    )
                        locator
                    else owner
                if (
                    node.kind == "directive" &&
                        node.name == "io-code-block" &&
                        selected(rule, nextSections)
                ) {
                    require(++groups <= limits.maximumGroups) { "Scenario group budget exceeded" }
                    if (rule.sectionAssembly == null)
                        group(
                            rule,
                            page,
                            node,
                            locator,
                            nextSections,
                            nextContainers,
                            attachedLabels,
                        )
                    else
                        pendingIo +=
                            {
                                    group(
                                        rule,
                                        page,
                                        node,
                                        locator,
                                        nextSections,
                                        nextContainers,
                                        attachedLabels,
                                        // Only the section's setup that comes before the group.
                                        sharedSetup +
                                            sectionSetup[nextSections].orEmpty().filter {
                                                locatorOrder.compare(it.locator, locator) < 0
                                            },
                                        // Setup of its own the grammar can't read leaves the
                                        // group without its data.
                                        setupProblem ?: sectionSetupProblem[nextSections],
                                    )
                                }
                                .also { ioSections += nextSections }
                    return
                }
                if (node.kind == "code" && selected(rule, nextSections)) {
                    val raw = code(node, locator, nextContainers, page)
                    if (rule.sectionAssembly != null) {
                        owners
                            .getOrPut(nextOwner) {
                                Owner(nextSections, nextContainers, node.origin)
                            }
                            .blocks += raw
                    } else unassigned += raw
                }
                var precedingLabels = emptyList<String>()
                node.children.forEachIndexed { index, child ->
                    val identifiers = labels(child)
                    if (identifiers.isNotEmpty()) precedingLabels = precedingLabels + identifiers
                    else {
                        walk(
                            child,
                            locator + index,
                            nextSections,
                            nextContainers,
                            precedingLabels,
                            nextOwner,
                        )
                        if (child.kind != "comment") precedingLabels = emptyList()
                    }
                }
                node.arguments.forEachIndexed { index, child ->
                    walk(
                        child,
                        locator + -(index + 1),
                        nextSections,
                        nextContainers,
                        owner = nextOwner,
                    )
                }
            }
            walk(page.expanded, emptyList(), emptyList(), emptyList())
            rule.sectionAssembly?.let { policy ->
                val selectedSetup = mutableSetOf<Owner>()
                val rawSetup = mutableListOf<ScenarioCode>()
                policy.sharedSetupSections.forEach { anchor ->
                    val all = owners.values.filter { it.sections.lastOrNull()?.anchor == anchor }
                    // A section with one selected-content branch per language shares its shell
                    // one; when only other languages show code, it shares nothing.
                    val matches =
                        all.filter {
                            ownVariant(rule, it.blocks, it.containers, true) !=
                                ScenarioVariant.FOREIGN
                        }
                    if (all.isNotEmpty() && matches.isEmpty()) {
                        // The foreign branches stay ordinary foreign groups.
                    } else if (matches.size != 1) {
                        setupProblem = "MISSING_OR_AMBIGUOUS_SHARED_SETUP"
                        problem(
                            requireNotNull(setupProblem),
                            rule,
                            "Shared setup section $anchor must own exactly one direct source group",
                        )
                    } else {
                        selectedSetup += matches.single()
                        rawSetup += matches.single().blocks
                    }
                }
                if (rawSetup.isNotEmpty()) {
                    val assignment = SectionPrograms.shared(rawSetup, rule.dialect)
                    sharedSetupSkipped = assignment.skipped
                    // Sample documents that seed data are data, whatever their highlighting.
                    val knownReference =
                        assignment.blocks
                            .filter { it.format == null }
                            .all {
                                variant(
                                    rule,
                                    listOf(it.copy(role = ProgramRole.ACTION)),
                                    it.containers,
                                ) == ScenarioVariant.REFERENCE
                            }
                    if (assignment.problem != null || !knownReference) {
                        setupProblem = assignment.problem ?: "UNRESOLVED_SHARED_SETUP_LANGUAGE"
                        problem(
                            requireNotNull(setupProblem),
                            rule,
                            "Declared shared setup is not a closed setup-only program",
                        )
                    } else sharedSetup = assignment.blocks
                }
                if (setupProblem != null) selectedSetup.forEach { unassigned += it.blocks }
                owners.values
                    .filter { it !in selectedSetup }
                    .forEach { owner ->
                        val ownVariant = ownVariant(rule, owner.blocks, owner.containers, true)
                        val setup =
                            if (ownVariant == ScenarioVariant.REFERENCE) sharedSetup
                            else emptyList()
                        // A section that inserts in plain code and queries in an input/output
                        // group gives its setup to that group, rather than running nothing.
                        if (
                            ownVariant == ScenarioVariant.REFERENCE &&
                                policy.layout == SectionLayout.COMMANDS_THEN_RESULT &&
                                owner.sections in ioSections &&
                                SectionPrograms.assign(setup, owner.blocks, rule.dialect).all {
                                    it.problem == "MISSING_COMMAND_OR_RESULT"
                                }
                        ) {
                            val local = SectionPrograms.shared(owner.blocks, rule.dialect)
                            sectionSetup[owner.sections] = local.blocks
                            local.problem?.let { sectionSetupProblem[owner.sections] = it }
                            sharedSetupSkipped = sharedSetupSkipped + local.skipped
                            // Its inserts still reach the sections after it.
                            setupSources += owner.blocks
                            return@forEach
                        }
                        val assignments =
                            when {
                                policy.layout == SectionLayout.SOURCE_ONLY ->
                                    listOf(
                                        SectionPrograms.Assignment(
                                            setup + owner.blocks,
                                            "SOURCE_ONLY_SECTION",
                                        )
                                    )
                                ownVariant == ScenarioVariant.FOREIGN ->
                                    listOf(
                                        SectionPrograms.Assignment(owner.blocks, "FOREIGN_SECTION")
                                    )
                                ownVariant == ScenarioVariant.UNRESOLVED ->
                                    listOf(
                                        SectionPrograms.Assignment(
                                            owner.blocks,
                                            "UNRESOLVED_SECTION_LANGUAGE",
                                        )
                                    )
                                else -> SectionPrograms.assign(setup, owner.blocks, rule.dialect)
                            }
                        assignments.forEach { assignment ->
                            // Each action a section splits into is a group of its own.
                            require(++groups <= limits.maximumGroups) {
                                "Scenario group budget exceeded"
                            }
                            chargeSetup(setup)
                            emit(
                                rule,
                                page,
                                owner.sections,
                                owner.containers,
                                owner.source,
                                emptyList(),
                                assignment.blocks,
                                false,
                                setupProblem ?: assignment.problem,
                                sectionOwned = true,
                                sharedSetupBlocks = setup.size,
                                skipped = assignment.skipped,
                            )
                        }
                    }
                pendingIo.forEach { it() }
            }
            unassignedReference =
                unassigned.count {
                    variant(rule, listOf(it.copy(role = ProgramRole.ACTION)), it.containers) !=
                        ScenarioVariant.FOREIGN
                }
            rule.sections
                .filter { it !in foundSections }
                .forEach { problem("MISSING_SECTION", rule, "Declared section $it is absent") }
            inheritSetup(rule, start, setupSources)
            afterWrites(start)
            val emitted = scenarios.drop(start)
            val reference = emitted.count { it.variant == ScenarioVariant.REFERENCE }
            if (reference < rule.minimumReferenceGroups)
                problem(
                    "GROUP_COUNT_DROP",
                    rule,
                    "Observed $reference reference groups, expected at least ${rule.minimumReferenceGroups}",
                )
            if (unassignedReference > 0)
                problem(
                    "UNASSIGNED_CODE",
                    rule,
                    "${unassignedReference} selected reference/unknown blocks have no explicit io owner; declare grouping/setup before promotion",
                )
            if (!page.dependenciesComplete)
                problem("INCOMPLETE_DEPENDENCIES", rule, "Source dependency closure is incomplete")
            val sourceDiagnostics =
                ScenarioSourceHealth.inspect(
                    bundle,
                    page,
                    rule,
                    document.diagnostics,
                    ownedResources,
                    ::visit,
                )
            sourceDiagnostics
                .filter { it.blocksNaming }
                .forEach { item ->
                    problem(
                        "PARSER_SOURCE_ERROR",
                        rule,
                        "${item.diagnostic.code}: ${item.diagnostic.message}",
                    )
                }
            val complete =
                before == problems.size &&
                    emitted
                        .filter { it.variant == ScenarioVariant.REFERENCE }
                        .all { it.naming != null }
            pages +=
                ExtractedScenarioPage(
                    rule.id,
                    rule.page,
                    reference,
                    emitted.count { it.variant == ScenarioVariant.FOREIGN },
                    unassigned,
                    unassignedReference,
                    page.dependencies,
                    page.dependenciesComplete,
                    sourceDiagnostics,
                    complete,
                    sharedSetup,
                    sharedSetupSkipped,
                )
        }

        fun group(
            rule: ScenarioPageRule,
            page: DocumentPage,
            node: DocumentNode,
            locator: List<Int>,
            sections: List<ScenarioSection>,
            containers: List<ScenarioContainer>,
            attachedLabels: List<String>,
            sharedSetup: List<ScenarioCode> = emptyList(),
            setupProblem: String? = null,
        ) {
            val blocks = sharedSetup.toMutableList()
            var nested = false
            fun collect(current: DocumentNode, path: List<Int>, context: List<ScenarioContainer>) {
                visit()
                if (
                    current !== node &&
                        current.kind == "directive" &&
                        current.name == "io-code-block"
                )
                    nested = true
                val next =
                    if (current !== node && current.kind !in setOf("root", "section", "code"))
                        context + container(current)
                    else context
                if (current.kind == "code") blocks += code(current, path, next, page)
                current.children.forEachIndexed { i, child -> collect(child, path + i, next) }
                current.arguments.forEachIndexed { i, child ->
                    collect(child, path + -(i + 1), next)
                }
            }
            collect(node, locator, containers)
            emit(
                rule,
                page,
                sections,
                containers,
                node.origin,
                attachedLabels,
                blocks,
                nested,
                setupProblem,
                sharedSetupBlocks = sharedSetup.size,
            )
        }

        /**
         * A read that follows a write in the same section sees the write's effect, which its own
         * program doesn't run, whether the write sits in its section's code or in another input/
         * output group. Such a read keeps its source and reports `AFTER_EARLIER_WRITE`.
         */
        fun afterWrites(start: Int) {
            fun action(scenario: ExtractedScenario) =
                scenario.blocks.firstOrNull { it.role == ProgramRole.ACTION }
            fun writes(scenario: ExtractedScenario) =
                scenario.program
                    ?.commands
                    ?.filter { it.role == ProgramRole.ACTION }
                    ?.takeIf { it.isNotEmpty() }
                    ?.any { writes(it.operation) }
                    ?: (action(scenario)?.source?.let(write::containsMatchIn) == true)
            fun position(scenario: ExtractedScenario) =
                (action(scenario) ?: scenario.blocks.firstOrNull())?.locator.orEmpty()
            val order =
                Comparator<List<Int>> { a, b ->
                    a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) }
                        ?: a.size.compareTo(b.size)
                }
            val written = mutableSetOf<List<String?>>()
            (start until scenarios.size)
                .filter { scenarios[it].variant == ScenarioVariant.REFERENCE }
                .sortedWith(compareBy(order) { position(scenarios[it]) })
                .forEach { index ->
                    val scenario = scenarios[index]
                    val section = scenario.sections.map { it.anchor ?: it.title }
                    // A write the page shows but no scenario runs, such as a bulkWrite in a try
                    // block, still changes what the read after it sees.
                    val skippedWrite =
                        scenario.skipped.any {
                            write.containsMatchIn(it.code.source) &&
                                order.compare(it.code.locator, position(scenario)) < 0
                        }
                    if (
                        (section in written || skippedWrite) &&
                            !writes(scenario) &&
                            action(scenario) != null
                    )
                        scenarios[index] =
                            scenario.copy(
                                program = null,
                                normalizationProblem = "AFTER_EARLIER_WRITE",
                            )
                    if (writes(scenario)) written += section
                }
        }

        fun program(rule: ScenarioPageRule, blocks: List<ScenarioCode>) =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = rule.dialect,
                    blocks =
                        blocks.mapIndexed { i, b ->
                            ProgramBlock(
                                "block-$i",
                                b.source,
                                requireNotNull(b.role),
                                format =
                                    b.format
                                        ?: if (b.role == ProgramRole.EXPECTED)
                                            ProgramFormat.SHELL_RESULTS
                                        else ProgramFormat.SHELL,
                                namespace = b.namespace,
                            )
                        },
                    resultOrder = rule.resultOrder,
                )
            )

        /**
         * A scenario that inserts nothing into its action's collection runs against the documents
         * the page inserted there most recently: a later section's query on "the survey collection
         * above" gets that section's inserts. Only inserts carry over, never another example's
         * action.
         */
        fun inheritSetup(
            rule: ScenarioPageRule,
            start: Int,
            sources: List<List<ScenarioCode>> = emptyList(),
        ) {
            fun position(blocks: List<ScenarioCode>) =
                (blocks.firstOrNull { it.role == ProgramRole.ACTION } ?: blocks.firstOrNull())
                    ?.locator
                    .orEmpty()
            val inserted = mutableMapOf<LogicalNamespace, List<ScenarioCode>>()
            // Setup the page loads but no example runs: its inserts carry on, and one the grammar
            // can't read leaves no older data to inherit, since it may have replaced it.
            fun load(blocks: List<ScenarioCode>) {
                if (SectionPrograms.shared(blocks, rule.dialect).problem != null) inserted.clear()
                else inserted.putAll(SectionPrograms.inserts(blocks, rule.dialect))
            }
            val events =
                (start until scenarios.size)
                    .filter { scenarios[it].variant == ScenarioVariant.REFERENCE }
                    .map { index -> position(scenarios[index].blocks) to index } +
                    sources.mapIndexed { i, source -> position(source) to -1 - i }
            events.sortedWith(compareBy(locatorOrder) { it.first }).forEach { (_, event) ->
                if (event < 0) return@forEach load(sources[-1 - event])
                val index = event
                val scenario = scenarios[index]
                if (scenario.normalizationProblem == "MISSING_COMMAND_OR_RESULT")
                    return@forEach load(scenario.blocks)
                if (scenario.normalizationProblem == "UNSUPPORTED_SETUP")
                    return@forEach inserted.clear()
                val program =
                    scenario.program?.takeIf { it.state == ShellLiteralState.NORMALIZED }
                        ?: return@forEach
                // The setup through the last insert into each collection: an insert's
                // declarations come before it.
                val inserts =
                    program.commands
                        .filter { it.operation.kind == ShellOperationKind.INSERT }
                        .groupBy({ it.namespace }) { block(it.id.block) }
                        .mapValues { (_, indexes) ->
                            scenario.blocks.take(indexes.max() + 1).filter {
                                it.role == ProgramRole.SETUP
                            }
                        }
                val namespace =
                    program.commands.lastOrNull { it.role == ProgramRole.ACTION }?.namespace
                val inherited = namespace?.let { inserted[it] }
                if (namespace != null && namespace !in inserts && inherited != null) {
                    // The copied setup counts against the budgets like any other.
                    chargeSetup(inherited)
                    val blocks = inherited + scenario.blocks
                    val replaced = program(rule, blocks)
                    programBytes +=
                        AuditJson.requireMaximumSize(replaced, limits.maximumReportBytes)
                    require(programBytes <= limits.maximumReportBytes) {
                        "Embedded programs exceed scenario report budget"
                    }
                    scenarios[index] =
                        if (replaced.state == ShellLiteralState.NORMALIZED)
                            scenario.copy(blocks = blocks, program = replaced)
                        // Without its data the example can't run as documented.
                        else
                            scenario.copy(
                                program = null,
                                normalizationProblem = "UNSUPPORTED_INHERITED_SETUP",
                            )
                }
                inserted.putAll(inserts)
            }
        }

        private fun block(id: String) = id.removePrefix("block-").toInt()

        /** An update, or a pipeline whose last stage writes its output. */
        private fun writes(operation: ShellOperation) =
            when (operation) {
                is UpdateOperation -> true
                is AggregateOperation ->
                    operation.pipeline.value().asArray().lastOrNull()?.let { stage ->
                        stage.isDocument &&
                            stage.asDocument().keys.firstOrNull() in setOf("\$merge", "\$out")
                    } == true
                else -> false
            }

        /** Page order of two locators. */
        val locatorOrder =
            Comparator<List<Int>> { a, b ->
                a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) }
                    ?: a.size.compareTo(b.size)
            }

        fun chargeSetup(blocks: List<ScenarioCode>) {
            characters += blocks.sumOf { it.source.length.toLong() }
            require(characters <= limits.maximumCodeCharacters) {
                "Expanded setup exceeds character budget"
            }
        }

        fun ownVariant(
            rule: ScenarioPageRule,
            blocks: List<ScenarioCode>,
            containers: List<ScenarioContainer>,
            sectionOwned: Boolean,
        ): ScenarioVariant {
            // Documents are data: the code around them decides the language, wherever they sit.
            val inputs =
                if (
                    sectionOwned &&
                        rule.sectionAssembly?.layout == SectionLayout.COMMANDS_THEN_RESULT
                )
                    blocks
                        .filter {
                            it.role != ProgramRole.EXPECTED && !SectionPrograms.isDocument(it)
                        }
                        .ifEmpty { blocks }
                else blocks
            return variant(
                rule,
                inputs.map { if (it.role == null) it.copy(role = ProgramRole.ACTION) else it },
                if (rule.sectionAssembly != null) containers + blocks.flatMap { it.containers }
                else containers,
            )
        }

        fun emit(
            rule: ScenarioPageRule,
            page: DocumentPage,
            sections: List<ScenarioSection>,
            containers: List<ScenarioContainer>,
            origin: DocumentOrigin,
            attachedLabels: List<String>,
            assembledBlocks: List<ScenarioCode>,
            nested: Boolean,
            ownershipProblem: String?,
            sectionOwned: Boolean = false,
            sharedSetupBlocks: Int = 0,
            skipped: List<SkippedCode> = emptyList(),
        ) {
            val scenarioTabs =
                containers.filter {
                    it.name == "tab" &&
                        value(it.options, "tabid") in rule.sectionAssembly?.scenarioTabs.orEmpty()
                }
            val tabTitles = scenarioTabs.map { it.argument }.filterNot(DescriptiveNames::isGeneric)
            val tabTitle = tabTitles.lastOrNull()
            val titleOverride =
                if (rule.sectionAssembly == null) null
                else
                    sections.lastOrNull()?.title?.takeUnless(DescriptiveNames::isGeneric)
                        ?: tabTitle
                        ?: sections
                            .asReversed()
                            .firstOrNull { !DescriptiveNames.isGeneric(it.title) }
                            ?.title
            val scenarioTab =
                scenarioTabs
                    .mapNotNull { value(it.options, "tabid") }
                    .joinToString("/tab:")
                    .takeIf { it.isNotBlank() }
            val ownBlocks = assembledBlocks.drop(sharedSetupBlocks)
            val variant = ownVariant(rule, ownBlocks, containers, sectionOwned)
            var blocks = if (variant == ScenarioVariant.REFERENCE) assembledBlocks else ownBlocks
            var skippedCode = skipped
            if (variant == ScenarioVariant.REFERENCE) {
                val (seeded, unused) = SectionPrograms.resolveSeeds(blocks, rule.dialect)
                blocks = seeded
                skippedCode = skippedCode + unused
            }
            if (!sectionOwned && variant == ScenarioVariant.REFERENCE)
                chargeSetup(assembledBlocks.take(sharedSetupBlocks))
            val id =
                "scenario-" +
                    AuditJson.hash(
                        listOf(
                            rule.id,
                            sections.map { listOf(it.anchor, it.explicitLabels) },
                            attachedLabels,
                            // Selections tell apart branches that show the same code.
                            containers.map {
                                listOf(
                                    it.name,
                                    it.argument,
                                    it.options,
                                    it.attributes["selections"],
                                )
                            },
                            blocks.map {
                                listOf(it.role, it.sha256, it.snippets.map { s -> s.identity })
                            },
                        )
                    )
            if (variant == ScenarioVariant.UNRESOLVED)
                problem(
                    "UNRESOLVED_LANGUAGE",
                    rule,
                    "Input language/tab is missing, conflicting or outside the declared policies",
                    id,
                )
            if (nested || blocks.isEmpty() || (blocks.any { it.role == null } && !sectionOwned))
                problem(
                    "UNRESOLVED_IO",
                    rule,
                    "IO groups require unambiguous input/output ownership and no nested groups",
                    id,
                )
            if (
                blocks.any { b ->
                    b.snippets.any {
                        it.canonicalPath == null || it.sha256 == null || it.parserDigest == null
                    }
                }
            )
                problem(
                    "UNRESOLVED_SNIPPET",
                    rule,
                    "Referenced source lacks a verified resource/dependency receipt",
                    id,
                )
            if (
                blocks.any {
                    it.origin.path == null &&
                        it.snippets.none { s -> s.identity != null && s.parserDigest != null }
                }
            )
                problem(
                    "UNRESOLVED_CODE_ORIGIN",
                    rule,
                    "Code lacks a pinned physical origin or verified snippet receipt",
                    id,
                )
            val canNormalize =
                ownershipProblem == null &&
                    variant == ScenarioVariant.REFERENCE &&
                    !nested &&
                    blocks.isNotEmpty() &&
                    blocks.all { it.role != null } &&
                    blocks
                        .filter { it.role == ProgramRole.EXPECTED }
                        .all {
                            // Pages label printed results as JSON, or not at all.
                            it.language in
                                rule.referenceLanguages +
                                    rule.sectionAssembly?.additionalShellLanguages.orEmpty() +
                                    listOf(null, "json", "none")
                        }
            fun normalize(blocks: List<ScenarioCode>) = program(rule, blocks)
            var program = if (canNormalize) normalize(blocks) else null
            // A printed result the grammar can't read (a truncated `...` listing) leaves the
            // example serialization-only rather than losing it. What an update prints, its
            // acknowledgement or a later read's documents, isn't a read result either.
            val result = blocks.lastOrNull()?.takeIf { it.role == ProgramRole.EXPECTED }
            if (
                result != null && program != null && program.state != ShellLiteralState.NORMALIZED
            ) {
                val update = program.problems.any { it.code == "PUBLISHED_PROFILE" }
                if (sectionOwned || update) {
                    val without = normalize(blocks.dropLast(1))
                    if (without.state == ShellLiteralState.NORMALIZED) {
                        program = without
                        blocks = blocks.dropLast(1)
                        skippedCode =
                            skippedCode +
                                SkippedCode(
                                    result,
                                    if (update) "UPDATE_RESULT" else "UNREADABLE_RESULT",
                                )
                    }
                }
            }
            program?.let {
                programBytes += AuditJson.requireMaximumSize(it, limits.maximumReportBytes)
                require(programBytes <= limits.maximumReportBytes) {
                    "Embedded programs exceed scenario report budget"
                }
            }
            val naming =
                if (
                    variant == ScenarioVariant.REFERENCE &&
                        !nested &&
                        blocks.isNotEmpty() &&
                        (blocks.all { it.role != null } || sectionOwned) &&
                        ownershipProblem !in
                            setOf(
                                "MISSING_OR_AMBIGUOUS_SHARED_SETUP",
                                "UNSUPPORTED_SHARED_SETUP",
                                "UNRESOLVED_SHARED_SETUP_LANGUAGE",
                            )
                )
                    naming(
                        rule,
                        page,
                        id,
                        sections,
                        blocks,
                        program,
                        titleOverride,
                        scenarioTab,
                        tabTitles.asReversed(),
                    )
                else null
            scenarios +=
                ExtractedScenario(
                    id,
                    rule.id,
                    rule.page,
                    variant,
                    sections,
                    containers,
                    origin,
                    attachedLabels,
                    blocks,
                    program,
                    ownershipProblem
                        ?: if (program == null && variant == ScenarioVariant.REFERENCE)
                            "UNSUPPORTED_IO_OR_OUTPUT_LANGUAGE"
                        else null,
                    naming,
                    skippedCode,
                )
        }

        fun snippetTitle(blocks: List<ScenarioCode>): String? =
            blocks
                .filter { it.role == ProgramRole.ACTION }
                .flatMap { it.snippets }
                .filter { it.canonicalPath != null }
                .map { it.reference }
                .distinct()
                .singleOrNull()
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.let { it.substringAfter(".snippet.", it) }

        fun naming(
            rule: ScenarioPageRule,
            page: DocumentPage,
            id: String,
            sections: List<ScenarioSection>,
            blocks: List<ScenarioCode>,
            program: ShellProgramResult?,
            titleOverride: String? = null,
            scenarioTab: String? = null,
            tabTitles: List<String> = emptyList(),
        ): NamingScenario? {
            val snippets =
                blocks
                    .filter { it.role == ProgramRole.ACTION }
                    .flatMap { it.snippets }
                    .mapNotNull { it.identity }
                    .distinct()
                    .sorted()
            val nearest = sections.lastOrNull()
            val sourceTitle = nearest?.title?.takeUnless(DescriptiveNames::isGeneric)
            val snippetTitle = snippetTitle(blocks)
            val title =
                titleOverride
                    ?: sourceTitle
                    ?: snippetTitle?.takeUnless(DescriptiveNames::isGeneric)
            if (title == null) {
                problem(
                    "MISSING_TITLE",
                    rule,
                    "No descriptive heading or unique named input snippet",
                    id,
                )
                return null
            }
            val context =
                sections
                    .dropLast(1)
                    .mapNotNull { it.anchor }
                    .let { anchors -> anchors + listOfNotNull(scenarioTab?.let { "tab:$it" }) }
                    .joinToString("/")
                    .takeIf { it.isNotBlank() }
            // Program meaning remains in program.semanticSha256. Do not advertise a complete
            // scenario/dependency fingerprint until setup/dataset/constraint closure is known.
            val fingerprint: String? = null
            val evidence =
                blocks
                    .flatMap { b ->
                        listOfNotNull(
                            "code:${b.sha256}",
                            b.origin.path?.let { path ->
                                b.origin.sha256?.let { "source:$path:$it" }
                            },
                        ) +
                            b.snippets.mapNotNull { snippet ->
                                snippet.canonicalPath?.let { path ->
                                    snippet.sha256?.let { "snippet:$path:$it" }
                                }
                            }
                    }
                    .distinct()
                    .sorted()
            return NamingScenario(
                id,
                rule.scope,
                rule.destination,
                ScenarioTrace(
                    plan.catalogSha256,
                    ScenarioLocation(
                        prefix + rule.page,
                        emptyList(),
                        snippets,
                        nearest?.anchor,
                        context,
                        fingerprint,
                    ),
                    title,
                    evidence,
                ),
                tabTitles.filter { it != title } +
                    sections.dropLast(1).map { it.title }.filter { it.isNotBlank() }.asReversed(),
            )
        }

        fun sha(source: String): String {
            val bytes =
                Charsets.UTF_8.newEncoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(source))
            val hash = MessageDigest.getInstance("SHA-256")
            hash.update(bytes)
            return HexFormat.of().formatHex(hash.digest())
        }
    }
}
