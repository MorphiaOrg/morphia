package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.*
import dev.morphia.audits.documents.*
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.util.IdentityHashMap

/** Explicit catalog contexts assign identity; a definition role alone never implies a context. */
object CatalogDiscovery {
    const val VERSION = "1"

    fun normalize(
        bundle: VerifiedSourceBundle,
        document: DocumentReport,
        plan: DiscoveryPlan,
    ): DiscoveryReport {
        val canonical = canonical(plan)
        require(document.sourceSha256 == bundle.manifest.contentSha256) {
            "Document/source pin mismatch"
        }
        DocumentParsing.validateOrigins(document, bundle)
        require(
            document.inputSha256 ==
                AuditJson.hash(DocumentParsing.export(bundle, document.pages.map { it.id }))
        ) {
            "Stored document parse-input pin mismatch"
        }
        require(
            document.parser.engine == "snooty" &&
                document.parser.revision == DocumentParsing.PARSER_REVISION
        )
        return DiscoveryRun(bundle, document, canonical).run()
    }

    internal fun canonical(plan: DiscoveryPlan): DiscoveryPlan {
        require(plan.schemaVersion == 1 && plan.product.matches(Regex("[a-z][a-z0-9-]*")))
        require(
            plan.definitionRoots.isNotEmpty() &&
                plan.definitionRoots.all { ContractValidation.relative(it) }
        )
        require(
            plan.catalogs.isNotEmpty() &&
                plan.catalogs.map { it.id }.distinct().size == plan.catalogs.size
        )
        plan.catalogs.forEach { rule ->
            require(
                rule.id.isNotBlank() &&
                    ContractValidation.relative(rule.page) &&
                    rule.page.endsWith(".txt") &&
                    rule.minimumMembers > 0
            )
            require(
                (rule.format != CatalogFormat.TABLE_FIRST_COLUMN || rule.headers.isNotEmpty()) &&
                    rule.headers.distinct().size == rule.headers.size
            )
            require(
                rule.primaryDefinitionRole in DocumentFacts.prefixes &&
                    rule.fallbackDefinitionRoles.all { it in DocumentFacts.prefixes }
            )
            require(rule.referenceRoles.all { it in DocumentFacts.prefixes })
            // Overlaps and duplicates would change the plan pin without changing its meaning.
            require(
                listOf(
                        rule.sections,
                        rule.excludedSections,
                        rule.ignoredHeaders,
                        rule.referenceRoles,
                        rule.fallbackDefinitionRoles,
                    )
                    .all { it.distinct().size == it.size }
            ) {
                "Catalog rule ${rule.id} repeats a selector or role"
            }
            require(
                rule.headers.intersect(rule.ignoredHeaders.toSet()).isEmpty() &&
                    rule.sections.intersect(rule.excludedSections.toSet()).isEmpty()
            ) {
                "Catalog rule ${rule.id} both selects and excludes a header or section"
            }
        }
        require(plan.definitionRoots.distinct().size == plan.definitionRoots.size)
        require(
            plan.catalogs
                .groupBy { it.context }
                .values
                .all { rules -> rules.map { it.primaryDefinitionRole }.distinct().size == 1 }
        ) {
            "Each context needs one consistent primary definition role"
        }
        val canonical =
            plan.copy(
                definitionRoots = plan.definitionRoots.sorted(),
                catalogs =
                    plan.catalogs
                        .map { rule ->
                            rule.copy(
                                sections = rule.sections.sorted(),
                                excludedSections = rule.excludedSections.sorted(),
                                headers = rule.headers.sorted(),
                                ignoredHeaders = rule.ignoredHeaders.sorted(),
                                referenceRoles = rule.referenceRoles.sorted(),
                                fallbackDefinitionRoles = rule.fallbackDefinitionRoles.sorted(),
                            )
                        }
                        .sortedBy { it.id },
            )
        return canonical
    }
}

private class DiscoveryRun(
    val bundle: VerifiedSourceBundle,
    val document: DocumentReport,
    val plan: DiscoveryPlan,
) {
    private data class Definition(
        val role: String,
        val target: String,
        val token: String,
        val location: LocatedNode,
        val evidence: SourceEvidence,
    )

    private data class Member(
        val rule: CatalogRule,
        val token: String,
        val location: LocatedNode,
        val evidence: SourceEvidence,
    )

    private val sourcePrefix =
        bundle.manifest.request.projectRoot.takeUnless { it == "." }?.let { "$it/source/" }
            ?: "source/"
    private val evidence = SourceEvidenceIndex(bundle)
    private val diagnostics = mutableListOf<DiscoveryDiagnostic>()
    private val pages = document.pages.associateBy { it.id }
    private val definitions = mutableListOf<Definition>()
    private val members = mutableListOf<Member>()
    private val facts = mutableMapOf<String, List<LocatedNode>>()
    private val tableOwners = IdentityHashMap<DocumentNode, MutableSet<String>>()

    private fun problem(
        code: String,
        domain: DiscoveryDomain,
        context: OperatorContext?,
        page: String?,
        message: String,
    ) {
        diagnostics += DiscoveryDiagnostic(code, domain, context, page, message)
    }

    fun run(): DiscoveryReport {
        val selected =
            bundle.manifest.resources
                .map { it.path.removePrefix(sourcePrefix) }
                .filter { page ->
                    page.endsWith(".txt") &&
                        (plan.definitionRoots.any { DocumentFacts.within(page, it) } ||
                            plan.catalogs.any { DocumentFacts.catalogPage(page, it) })
                }
                .toSortedSet()
        plan.definitionRoots.forEach { root ->
            if (selected.none { DocumentFacts.within(it, root) })
                problem(
                    "MISSING_DEFINITION_ROOT",
                    DiscoveryDomain.DEFINITION,
                    null,
                    root,
                    "Declared definition root contains no captured pages",
                )
        }
        selected.forEach { id ->
            val page = pages[id]
            if (page?.raw == null || page.expanded == null)
                problem(
                    "MISSING_DOCUMENT_PAGE",
                    DiscoveryDomain.DEFINITION,
                    null,
                    id,
                    "Selected source page lacks raw/expanded trees",
                )
            else facts[id] = DocumentFacts.walk(page.expanded, id).toList()
        }
        scanDefinitions()
        plan.catalogs.forEach(::scanCatalog)
        checkTableOwnership()
        val used = mutableMapOf<Definition, MutableSet<OperatorContext>>()
        val capabilityPages = mutableMapOf<String, MutableSet<CapabilityKey>>()
        val capabilities =
            members
                .groupBy { CapabilityKey(plan.product, it.rule.context, it.token) }
                .toSortedMap(compareBy { it.id() })
                .map { (key, rows) ->
                    val preferred = rows.map { it.rule.primaryDefinitionRole }.toSet()
                    require(preferred.size == 1) {
                        "Conflicting primary definition policies for ${key.context}"
                    }
                    val primary =
                        definitions.filter { it.token == key.token && it.role in preferred }
                    val fallback = rows.flatMap { it.rule.fallbackDefinitionRoles }.toSet()
                    val chosen =
                        primary.ifEmpty {
                            definitions.filter { it.token == key.token && it.role in fallback }
                        }
                    chosen.forEach { used.getOrPut(it) { mutableSetOf() } += key.context }
                    chosen.forEach {
                        capabilityPages.getOrPut(it.location.page) { mutableSetOf() } += key
                    }
                    rows.forEach {
                        capabilityPages.getOrPut(it.location.page) { mutableSetOf() } += key
                    }
                    if (chosen.isEmpty())
                        problem(
                            "MISSING_DEFINITION_METADATA",
                            DiscoveryDomain.VERSION,
                            key.context,
                            rows.first().location.page,
                            "${key.token} is cataloged; its definition metadata is unavailable",
                        )
                    val facets = linkedMapOf<String, CapabilityFacet>()
                    val events = mutableListOf<VersionEvent>()
                    chosen.forEach { definition ->
                        events +=
                            LifecycleFacts.collect(
                                definition.location,
                                definition.role in preferred,
                                key,
                                facts[definition.location.page].orEmpty(),
                                evidence,
                                facets,
                            ) { code, message ->
                                problem(
                                    code,
                                    DiscoveryDomain.VERSION,
                                    key.context,
                                    definition.location.page,
                                    message,
                                )
                            }
                    }
                    rows.forEach { row ->
                        events +=
                            LifecycleFacts.catalog(row.location, key, evidence) { code, message ->
                                problem(
                                    code,
                                    DiscoveryDomain.VERSION,
                                    key.context,
                                    row.location.page,
                                    message,
                                )
                            }
                    }
                    val sourceEvidence =
                        (rows.map { it.evidence.id } + chosen.map { it.evidence.id })
                            .distinct()
                            .sorted()
                    Capability(
                        key,
                        listOf(CapabilityObservation(bundle.manifest.request.id, sourceEvidence)),
                        facets.values.sortedBy { it.id },
                        events
                            .distinct()
                            .sortedWith(
                                compareBy(
                                    { it.facet },
                                    { it.kind.name },
                                    { it.rawVersion },
                                    { it.evidence.joinToString() },
                                )
                            ),
                        emptyList(),
                        ExtractionState.NOT_ASSESSED,
                        emptyList(),
                    )
                }
        val unassigned =
            definitions
                .filter { definition ->
                    val nativeContexts =
                        plan.catalogs
                            .filter { it.primaryDefinitionRole == definition.role }
                            .map { it.context }
                            .toSet()
                    val assigned = used[definition].orEmpty()
                    assigned.isEmpty() ||
                        (nativeContexts.isNotEmpty() &&
                            assigned.intersect(nativeContexts).isEmpty())
                }
                .map { definition ->
                    val affected =
                        plan.catalogs
                            .filter { definition.role == it.primaryDefinitionRole }
                            .map { it.context }
                            .distinct()
                    if (affected.isEmpty())
                        problem(
                            "UNASSIGNED_DEFINITION",
                            DiscoveryDomain.DEFINITION,
                            null,
                            definition.location.page,
                            "No context policy owns ${definition.target}",
                        )
                    else
                        affected.forEach { context ->
                            problem(
                                "UNASSIGNED_DEFINITION",
                                DiscoveryDomain.DEFINITION,
                                context,
                                definition.location.page,
                                "Catalogs do not assign ${definition.target}; preserve it for review",
                            )
                        }
                    UnassignedDefinition(
                        definition.role,
                        definition.target,
                        definition.location.page,
                        listOf(definition.evidence.id),
                    )
                }
                .distinct()
                .sortedWith(compareBy({ it.role }, { it.target }, { it.page }))
        val pageReports = classifyDiagnostics(capabilityPages)
        val contexts =
            OperatorContext.entries.map { context ->
                if (plan.catalogs.none { it.context == context })
                    problem(
                        "MISSING_CONTEXT_POLICY",
                        DiscoveryDomain.CATALOG,
                        context,
                        null,
                        "Context has no catalog policy",
                    )
                val problems =
                    diagnostics
                        .filter {
                            it.domain in
                                setOf(DiscoveryDomain.CATALOG, DiscoveryDomain.DEFINITION) &&
                                (it.context == null || it.context == context)
                        }
                        .distinct()
                        .sortedWith(diagnosticOrder)
                DiscoveryContext(
                    context,
                    problems.isEmpty(),
                    members
                        .filter { it.rule.context == context }
                        .map { it.token }
                        .distinct()
                        .sorted(),
                    capabilities.count { it.key.context == context },
                    problems,
                )
            }
        val catalogProblems =
            diagnostics
                .filter { it.domain in setOf(DiscoveryDomain.CATALOG, DiscoveryDomain.DEFINITION) }
                .map { AuditProblem(it.code, it.message, it.context, it.page) }
                .distinct()
                .sortedWith(
                    compareBy({ it.context?.name }, { it.code }, { it.subject }, { it.message })
                )
        val pin = bundle.manifest.request
        val source =
            DocumentationSource(
                pin.id,
                SourceKind.GIT,
                pin.uri,
                pin.revision,
                bundle.manifest.contentSha256,
                pin.projectRoot,
                pin.label,
                pin.role,
                pin.channel,
                ParserPin(
                    document.parser.engine,
                    document.parser.revision,
                    AuditJson.hash(document.parser),
                ),
                CatalogDiscovery.VERSION +
                    ":plan:" +
                    AuditJson.hash(plan) +
                    ":parse-input:" +
                    document.inputSha256,
            )
        val affected =
            pageReports
                .flatMap { p -> p.capabilities.map { it to p.exampleProblems } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, lists) -> lists.flatten().distinct().sorted() }
        val finalCapabilities =
            capabilities.map { capability ->
                val errors = affected[capability.key].orEmpty()
                if (errors.isEmpty()) capability
                else
                    capability.copy(examples = ExtractionState.UNRESOLVED, exampleProblems = errors)
            }
        val catalog =
            CapabilityCatalog(
                product = plan.product,
                sources = listOf(source),
                evidence = evidence.items(),
                capabilities = finalCapabilities,
                discoveryProblems = catalogProblems,
            )
        return DiscoveryReport(
            planSha256 = AuditJson.hash(plan),
            documentInputSha256 = document.inputSha256,
            state =
                if (contexts.all { it.healthy }) DiscoveryState.READY else DiscoveryState.PARTIAL,
            sourceState = document.state,
            catalog = catalog,
            contexts = contexts,
            pages = pageReports,
            unassignedDefinitions = unassigned,
            diagnostics = diagnostics.distinct().sortedWith(diagnosticOrder),
        )
    }

    private fun scanDefinitions() {
        facts
            .filterKeys { page -> plan.definitionRoots.any { DocumentFacts.within(page, it) } }
            .forEach { (page, nodes) ->
                nodes
                    .filter { it.node.kind == "target" && it.node.domain == "mongodb" }
                    .forEach { location ->
                        val node = location.node
                        val role = node.name ?: ""
                        if (role == "data") return@forEach
                        if (role !in DocumentFacts.prefixes) {
                            problem(
                                "UNKNOWN_DEFINITION_ROLE",
                                DiscoveryDomain.DEFINITION,
                                null,
                                page,
                                "Unsupported MongoDB definition role $role",
                            )
                            return@forEach
                        }
                        val ids =
                            node.children
                                .filter { it.kind == "target_identifier" }
                                .flatMap { it.identifiers }
                        if (ids.isEmpty())
                            problem(
                                "MISSING_DEFINITION_ID",
                                DiscoveryDomain.DEFINITION,
                                null,
                                page,
                                "Definition has no target identifiers",
                            )
                        ids.forEach { target ->
                            val token = DocumentFacts.token(role, target)
                            if (token == null)
                                problem(
                                    "UNSUPPORTED_OPERATOR_ID",
                                    DiscoveryDomain.DEFINITION,
                                    null,
                                    page,
                                    "Unrecognized operator identity $target",
                                )
                            else {
                                val declaration =
                                    Regex(
                                        "^\\s*\\.\\. (?:mongodb:)?${Regex.escape(role)}::\\s+${Regex.escape(token)}(?:\\s.*)?$"
                                    )
                                val proof =
                                    evidence.node(node, EvidenceKind.DEFINITION, target) {
                                        declaration.matches(it)
                                    }
                                if (proof == null)
                                    problem(
                                        "UNVERIFIED_DEFINITION",
                                        DiscoveryDomain.DEFINITION,
                                        null,
                                        page,
                                        "No raw-source declaration supports $target",
                                    )
                                else definitions += Definition(role, target, token, location, proof)
                            }
                        }
                    }
            }
    }

    private fun scanCatalog(rule: CatalogRule) {
        if (rule.format == CatalogFormat.OPERATOR_REFERENCE_LIST) {
            scanBulletCatalog(rule)
            return
        }
        val selected = facts.keys.filter { DocumentFacts.catalogPage(it, rule) }
        if (rule.page !in selected)
            problem(
                "MISSING_CATALOG",
                DiscoveryDomain.CATALOG,
                rule.context,
                rule.page,
                "Required catalog page is missing",
            )
        val found = mutableSetOf<String>()
        selected.forEach { page ->
            facts
                .getValue(page)
                .filter { it.node.kind == "directive" && it.node.name == "list-table" }
                .forEach tableLoop@{ location ->
                    val anchors = location.sections.mapNotNull(DocumentFacts::anchor)
                    if (
                        (rule.sections.isNotEmpty() && anchors.none { it in rule.sections }) ||
                            anchors.any { it in rule.excludedSections }
                    )
                        return@tableLoop
                    val rows = runCatching { DocumentFacts.rows(location.node) }.getOrNull()
                    if (rows == null || rows.isEmpty() || rows.first().isEmpty()) {
                        problem(
                            "UNSUPPORTED_CATALOG_TABLE",
                            DiscoveryDomain.CATALOG,
                            rule.context,
                            page,
                            "Catalog table shape is unsupported",
                        )
                        return@tableLoop
                    }
                    val header =
                        DocumentFacts.text(rows.first().first()).replace(Regex("\\s+"), " ").trim()
                    if (header in rule.ignoredHeaders || header !in rule.headers) return@tableLoop
                    if (location.node.options["header-rows"] != "1") {
                        problem(
                            "UNSUPPORTED_CATALOG_HEADER",
                            DiscoveryDomain.CATALOG,
                            rule.context,
                            page,
                            "Expected exactly one header row",
                        )
                        return@tableLoop
                    }
                    tableOwners.getOrPut(location.node) { mutableSetOf() } += rule.id
                    rows.drop(1).forEach { row -> recordMember(rule, location, row, found) }
                }
        }
        if (found.size < rule.minimumMembers)
            problem(
                "CATALOG_COUNT_DROP",
                DiscoveryDomain.CATALOG,
                rule.context,
                rule.page,
                "Catalog ${rule.id} has ${found.size} distinct members; minimum ${rule.minimumMembers}",
            )
    }

    private fun scanBulletCatalog(rule: CatalogRule) {
        val selected = facts.keys.filter { DocumentFacts.catalogPage(it, rule) }
        if (rule.page !in selected)
            problem(
                "MISSING_CATALOG",
                DiscoveryDomain.CATALOG,
                rule.context,
                rule.page,
                "Required catalog page is missing",
            )
        val found = mutableSetOf<String>()
        selected.forEach { page ->
            facts
                .getValue(page)
                .filter { it.node.kind == "list" }
                .forEach listLoop@{ location ->
                    val anchors = location.sections.mapNotNull(DocumentFacts::anchor)
                    if (
                        (rule.sections.isNotEmpty() && anchors.none { it in rule.sections }) ||
                            anchors.any { it in rule.excludedSections }
                    )
                        return@listLoop
                    location.node.children.forEach itemLoop@{ item ->
                        if (item.kind != "listItem") return@itemLoop
                        val paragraph =
                            item.children.firstOrNull()?.takeIf { it.kind == "paragraph" }
                                ?: return@itemLoop
                        val leading =
                            paragraph.children.firstOrNull {
                                it.kind != "text" || !it.text.isNullOrBlank()
                            } ?: return@itemLoop
                        val operatorRefs =
                            paragraph.children.filter {
                                it.kind == "ref_role" && it.domain == "mongodb"
                            }
                        val names =
                            when {
                                leading.kind == "ref_role" && leading.domain == "mongodb" -> leading
                                operatorRefs.isNotEmpty() &&
                                    leading.kind == "text" &&
                                    leading.text?.trimEnd()?.endsWith(":") == true &&
                                    paragraph.children.drop(1).all { child ->
                                        (child.kind == "ref_role" && child.domain == "mongodb") ||
                                            (child.kind == "text" &&
                                                child.text
                                                    .orEmpty()
                                                    .matches(Regex("(?:[\\s,;.]+|and|or)*")))
                                    } ->
                                    paragraph.copy(
                                        kind = "catalog-names",
                                        children = paragraph.children.drop(1),
                                    )
                                else -> null
                            }
                        if (names != null) recordMember(rule, location, listOf(names, item), found)
                        else if (operatorRefs.isNotEmpty())
                            problem(
                                "UNSUPPORTED_CATALOG_LIST_ROW",
                                DiscoveryDomain.CATALOG,
                                rule.context,
                                page,
                                "Operator list row is neither a leading name nor a labeled reference group",
                            )
                    }
                }
        }
        if (found.size < rule.minimumMembers)
            problem(
                "CATALOG_COUNT_DROP",
                DiscoveryDomain.CATALOG,
                rule.context,
                rule.page,
                "Catalog ${rule.id} has ${found.size} members; minimum ${rule.minimumMembers}",
            )
    }

    private fun recordMember(
        rule: CatalogRule,
        location: LocatedNode,
        row: List<DocumentNode>,
        found: MutableSet<String>,
    ) {
        val name = row.firstOrNull()
        if (name == null) {
            problem(
                "EMPTY_CATALOG_ROW",
                DiscoveryDomain.CATALOG,
                rule.context,
                location.page,
                "Catalog contains an empty row",
            )
            return
        }
        val references =
            DocumentFacts.walk(name, location.page)
                .map { it.node }
                .filter { it.kind == "ref_role" && it.domain == "mongodb" }
                .toList()
        val tokens =
            if (references.isEmpty())
                listOfNotNull(DocumentFacts.text(name).trim().takeIf(OperatorTokens::valid))
            else
                references
                    .mapNotNull { ref ->
                        val role = ref.name ?: ""
                        val target = DocumentFacts.value(ref, "target") ?: ""
                        if (role !in rule.referenceRoles) {
                            problem(
                                "UNSUPPORTED_CATALOG_REFERENCE",
                                DiscoveryDomain.CATALOG,
                                rule.context,
                                location.page,
                                "Unexpected catalog role $role/$target",
                            )
                            null
                        } else
                            DocumentFacts.token(role, target).also {
                                if (it == null)
                                    problem(
                                        "UNSUPPORTED_CATALOG_ID",
                                        DiscoveryDomain.CATALOG,
                                        rule.context,
                                        location.page,
                                        "Unrecognized catalog identity $target",
                                    )
                            }
                    }
                    .distinct()
        if (tokens.isEmpty())
            problem(
                "UNRESOLVED_CATALOG_ROW",
                DiscoveryDomain.CATALOG,
                rule.context,
                location.page,
                "First column has no recognized operator: ${DocumentFacts.text(name)}",
            )
        tokens.forEach { token ->
            val proof =
                evidence.membership(
                    references.firstOrNull { ref ->
                        DocumentFacts.token(
                            ref.name.orEmpty(),
                            DocumentFacts.value(ref, "target").orEmpty(),
                        ) == token
                    } ?: name,
                    token,
                    rule.referenceRoles,
                    "catalog:${rule.id}:$token",
                )
            if (proof == null)
                problem(
                    "UNVERIFIED_CATALOG_MEMBER",
                    DiscoveryDomain.CATALOG,
                    rule.context,
                    location.page,
                    "No raw source supports catalog row $token",
                )
            else {
                found += token
                val rowNode =
                    DocumentNode(
                        "catalog-row",
                        name.origin,
                        null,
                        null,
                        null,
                        emptyList(),
                        emptyMap(),
                        mapOf("memberCount" to tokens.size.toString()),
                        emptyList(),
                        row,
                    )
                members +=
                    Member(
                        rule,
                        token,
                        LocatedNode(rowNode, location.page, location.sections, location.targets),
                        proof,
                    )
            }
        }
    }

    private fun checkTableOwnership() {
        facts
            .filterKeys { page -> plan.catalogs.any { DocumentFacts.catalogPage(page, it) } }
            .forEach { (page, nodes) ->
                nodes
                    .filter { it.node.kind == "directive" && it.node.name == "list-table" }
                    .forEach { located ->
                        val rows =
                            runCatching { DocumentFacts.rows(located.node) }.getOrNull()
                                ?: return@forEach
                        val anchors = located.sections.mapNotNull(DocumentFacts::anchor)
                        val header =
                            rows
                                .firstOrNull()
                                ?.firstOrNull()
                                ?.let(DocumentFacts::text)
                                ?.replace(Regex("\\s+"), " ")
                                ?.trim()
                        val applicable =
                            plan.catalogs.filter {
                                DocumentFacts.catalogPage(page, it) &&
                                    anchors.none { anchor -> anchor in it.excludedSections } &&
                                    header !in it.ignoredHeaders
                            }
                        if (applicable.isEmpty()) return@forEach
                        val operatorNames =
                            rows
                                .drop(1)
                                .flatMap { row ->
                                    row.firstOrNull()
                                        ?.let {
                                            DocumentFacts.walk(it, page)
                                                .map { n -> n.node }
                                                .toList()
                                        }
                                        .orEmpty()
                                }
                                .any { node ->
                                    node.kind == "ref_role" &&
                                        node.domain == "mongodb" &&
                                        node.name in DocumentFacts.prefixes
                                }
                        val owners =
                            tableOwners[located.node]
                                .orEmpty()
                                .map { id -> plan.catalogs.single { it.id == id }.context }
                                .distinct()
                        if (operatorNames && owners.isEmpty())
                            applicable
                                .map { it.context }
                                .distinct()
                                .forEach { context ->
                                    problem(
                                        "UNCLAIMED_CATALOG_TABLE",
                                        DiscoveryDomain.CATALOG,
                                        context,
                                        page,
                                        "Operator table falls outside the declared selectors",
                                    )
                                }
                        if (owners.size > 1)
                            owners.forEach {
                                problem(
                                    "AMBIGUOUS_CATALOG_CONTEXT",
                                    DiscoveryDomain.CATALOG,
                                    it,
                                    page,
                                    "Catalog table is assigned to several contexts",
                                )
                            }
                    }
            }
    }

    private fun classifyDiagnostics(
        capabilityPages: Map<String, Set<CapabilityKey>>
    ): List<DiscoveredPage> {
        val dependencies = DiscoveryDependencies(bundle, sourcePrefix, facts, document.pages)
        val resources = dependencies.resources
        val exampleProblems = mutableMapOf<String, MutableList<String>>()
        fun affectedContexts(page: String): List<OperatorContext> =
            capabilityPages[page]
                .orEmpty()
                .map { it.context }
                .ifEmpty {
                    val roles =
                        definitions.filter { it.location.page == page }.map { it.role }.toSet()
                    plan.catalogs
                        .filter {
                            DocumentFacts.catalogPage(page, it) || it.primaryDefinitionRole in roles
                        }
                        .map { it.context }
                }
                .distinct()
        fun definitionFailure(code: String, page: String, message: String) {
            val contexts = affectedContexts(page)
            if (contexts.isEmpty()) problem(code, DiscoveryDomain.DEFINITION, null, page, message)
            else contexts.forEach { problem(code, DiscoveryDomain.DEFINITION, it, page, message) }
        }
        val byPosition =
            facts.values
                .asSequence()
                .flatten()
                .filter { it.node.origin.path != null && it.node.origin.reportedLine != null }
                .groupBy { it.node.origin.path to it.node.origin.reportedLine }
        document.diagnostics.forEach { diagnostic ->
            val users = dependencies.users(diagnostic)
            val owners = byPosition[diagnostic.path to diagnostic.line].orEmpty()
            val rawLine =
                if (diagnostic.code == "CannotOpenFile") dependencies.rawLine(diagnostic) else null
            val paragraph =
                if (diagnostic.code == "CannotOpenFile") dependencies.rawParagraph(diagnostic)
                else null
            val asset =
                diagnostic.code == "CannotOpenFile" &&
                    rawLine?.matches(Regex("^\\s*\\.\\. (?:mongodb:)?(?:figure|image)::\\s+.+$")) ==
                        true &&
                    owners.any {
                        it.node.kind == "directive" && it.node.name in setOf("figure", "image")
                    }
            val reference =
                asset ||
                    (diagnostic.code == "CannotOpenFile" &&
                        paragraph?.let { it.contains(":doc:`") || it.contains(":std:doc:`") } ==
                            true &&
                        owners.any {
                            it.node.kind == "ref_role" &&
                                it.node.domain == "std" &&
                                it.node.name == "doc"
                        }) ||
                    diagnostic.code in setOf("TargetNotFound", "UnnamedPage") ||
                    (diagnostic.code == "FetchError" &&
                        diagnostic.path == sourcePrefix.removeSuffix("source/") + "snooty.toml")
            if (reference)
                problem(
                    diagnostic.code,
                    DiscoveryDomain.REFERENCE,
                    null,
                    diagnostic.path,
                    diagnostic.message,
                )
            else if (users.isNotEmpty()) {
                val literal = dependencies.literal(diagnostic, users)
                users.forEach { page ->
                    val exampleInclude =
                        diagnostic.code == "CannotOpenFile" &&
                            rawLine?.matches(
                                Regex("^\\s*\\.\\. (?:mongodb:)?sharedinclude::\\s+.+$")
                            ) == true &&
                            owners
                                .filter { it.page == page }
                                .any { located ->
                                    located.node.kind == "directive" &&
                                        located.node.name == "sharedinclude" &&
                                        located.sections.any {
                                            DocumentFacts.title(it) in setOf("Example", "Examples")
                                        }
                                }

                    if (literal || exampleInclude) {
                        exampleProblems.getOrPut(page) { mutableListOf() } += diagnostic.message
                        problem(
                            diagnostic.code,
                            DiscoveryDomain.EXAMPLE,
                            null,
                            page,
                            diagnostic.message,
                        )
                    } else {
                        definitionFailure(diagnostic.code, page, diagnostic.message)
                    }
                }
            } else
                problem(
                    diagnostic.code,
                    DiscoveryDomain.OUTSIDE_SCOPE,
                    null,
                    diagnostic.path,
                    diagnostic.message,
                )
        }
        document.pages
            .filter { it.id in facts && !it.dependenciesComplete }
            .forEach { page ->
                if (dependencies.onlyFailedLiterals(page)) {
                    val message = "Parser example dependency graph is incomplete"
                    exampleProblems.getOrPut(page.id) { mutableListOf() } += message
                    problem(
                        "INCOMPLETE_EXAMPLE_DEPENDENCIES",
                        DiscoveryDomain.EXAMPLE,
                        null,
                        page.id,
                        message,
                    )
                } else
                    definitionFailure(
                        "UNEXPLAINED_DEPENDENCY_GRAPH",
                        page.id,
                        "Incomplete dependency graph cannot be attributed solely to example files",
                    )
            }
        return facts.keys.sorted().map { page ->
            DiscoveredPage(
                page,
                capabilityPages[page].orEmpty().sortedBy { it.id() },
                resources[page].orEmpty().sorted(),
                exampleProblems[page].orEmpty().distinct().sorted(),
            )
        }
    }

    private val diagnosticOrder =
        compareBy<DiscoveryDiagnostic>(
            { it.context?.name },
            { it.domain.name },
            { it.code },
            { it.page },
            { it.message },
        )
}
