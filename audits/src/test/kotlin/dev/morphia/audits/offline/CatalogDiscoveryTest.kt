package dev.morphia.audits.offline

import dev.morphia.audits.coverage.*
import dev.morphia.audits.discovery.*
import dev.morphia.audits.documents.*
import dev.morphia.audits.sources.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CatalogDiscoveryTest {
    private fun hash(bytes: ByteArray, algorithm: String = "SHA-256") =
        HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes))

    private inner class Source(val root: Path, val bodies: Map<String, String>) {
        val bundle: VerifiedSourceBundle
        private val resources: Map<String, GitResource>

        init {
            Files.createDirectories(root.resolve("blobs"))
            val records =
                bodies.map { (path, text) ->
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    val digest = hash(bytes)
                    Files.write(root.resolve("blobs/$digest"), bytes)
                    GitResource(
                        "manual/source/$path",
                        GitResourceKind.FILE,
                        hash("blob ${bytes.size}\u0000".toByteArray() + bytes, "SHA-1"),
                        digest,
                        bytes.size.toLong(),
                        null,
                    )
                }
            val config = "name = 'morphia-discovery-fixture'\n".toByteArray()
            val configHash = hash(config)
            Files.write(root.resolve("blobs/$configHash"), config)
            val configRecord =
                GitResource(
                    "manual/snooty.toml",
                    GitResourceKind.FILE,
                    hash("blob ${config.size}\u0000".toByteArray() + config, "SHA-1"),
                    configHash,
                    config.size.toLong(),
                    null,
                )
            val captured = records + configRecord
            resources = captured.associateBy { it.path }
            val request =
                GitSourceRequest(
                    id = "manual",
                    uri = "https://example.invalid/docs.git",
                    revision = "b".repeat(40),
                    projectRoot = "manual",
                    label = "current",
                    role = SourceRole.CURRENT,
                    channel = SourceChannel.UNKNOWN,
                    roots = listOf("manual"),
                    allowedSymlinkRoots = emptyList(),
                    maximumFiles = 100,
                    maximumFileBytes = 1024 * 1024,
                    maximumTotalBytes = 1024 * 1024,
                    timeoutSeconds = 10,
                )
            bundle =
                VerifiedSourceBundle(
                    root,
                    GitSourceManifest(
                        request = request,
                        tree = "c".repeat(40),
                        gitVersion = "fixture",
                        contentSha256 = GitSnapshot.contentHash(request, "c".repeat(40), captured),
                        state = SourceState.READY,
                        resources = captured,
                        problems = emptyList(),
                    ),
                )
            Files.writeString(root.resolve(SourceBundle.MANIFEST), AuditJson.write(bundle.manifest))
        }

        fun line(page: String, text: String) =
            bodies
                .getValue(page)
                .lines()
                .indexOfFirst { it.trim() == text }
                .also { require(it >= 0) } + 1

        fun node(
            kind: String,
            page: String,
            line: Int = 1,
            text: String? = null,
            name: String? = null,
            domain: String? = null,
            ids: List<String> = emptyList(),
            attributes: Map<String, String> = emptyMap(),
            options: Map<String, String> = emptyMap(),
            arguments: List<DocumentNode> = emptyList(),
            children: List<DocumentNode> = emptyList(),
        ): DocumentNode {
            val path = "manual/source/$page"
            return DocumentNode(
                kind,
                DocumentOrigin(
                    path,
                    resources.getValue(path).sha256,
                    line,
                    DocumentPrecision.PARSER_REPORTED,
                    listOf(path),
                ),
                text,
                name,
                domain,
                ids,
                options,
                attributes,
                arguments,
                children,
            )
        }

        /** Snooty reports a heading at its underline; fall back to line 1 for bodiless pages. */
        fun heading(page: String, title: String, anchor: String) =
            node(
                "heading",
                page,
                bodies.getValue(page).lines().let { lines ->
                    val title = lines.indexOfFirst { it.trim() == title }
                    if (
                        title >= 0 &&
                            lines.getOrNull(title + 1)?.matches(Regex("([-=~])\\1+")) == true
                    )
                        title + 2
                    else 1
                },
                text = title,
                attributes = mapOf("id" to AuditJson.write(anchor).trim()),
            )

        fun section(page: String, title: String, anchor: String, children: List<DocumentNode>) =
            node("section", page, children = listOf(heading(page, title, anchor)) + children)

        fun root(page: String, children: List<DocumentNode>) =
            node("root", page, children = children)

        fun reference(page: String, role: String, token: String) =
            node(
                "ref_role",
                page,
                bodies
                    .getValue(page)
                    .lines()
                    .indexOfFirst { it.contains(":$role:`$token") }
                    .let { if (it >= 0) it + 1 else 1 },
                name = role,
                domain = "mongodb",
                attributes = mapOf("target" to AuditJson.write(prefix(role) + token).trim()),
                children = listOf(node("text", page, text = token)),
            )

        fun definition(
            page: String,
            role: String,
            token: String,
            children: List<DocumentNode> = emptyList(),
        ) =
            node(
                "target",
                page,
                line(page, ".. $role:: $token"),
                name = role,
                domain = "mongodb",
                children =
                    listOf(node("target_identifier", page, ids = listOf(prefix(role) + token))) +
                        children,
            )

        fun version(page: String, value: String) =
            node(
                "directive",
                page,
                line(page, ".. versionadded:: $value"),
                name = "versionadded",
                arguments = listOf(node("text", page, text = value)),
            )

        fun table(
            page: String,
            role: String,
            tokens: List<String>,
            header: String = "Name",
            description: List<DocumentNode> = emptyList(),
        ): DocumentNode {
            fun row(first: DocumentNode) =
                node(
                    "listItem",
                    page,
                    children =
                        listOf(
                            node(
                                "list",
                                page,
                                children =
                                    listOf(
                                        node("listItem", page, children = listOf(first)),
                                        node("listItem", page, children = description),
                                    ),
                            )
                        ),
                )
            return node(
                "directive",
                page,
                name = "list-table",
                options = mapOf("header-rows" to "1"),
                children =
                    listOf(
                        node(
                            "list",
                            page,
                            children =
                                listOf(row(node("text", page, text = header))) +
                                    tokens.map { row(reference(page, role, it)) },
                        )
                    ),
            )
        }

        fun report(
            roots: Map<String, DocumentNode>,
            diagnostics: List<DocumentDiagnostic> = emptyList(),
        ): DocumentReport =
            DocumentReport(
                sourceSha256 = bundle.manifest.contentSha256,
                inputSha256 = AuditJson.hash(DocumentParsing.export(bundle, roots.keys.toList())),
                parser =
                    DocumentParser(
                        "snooty",
                        DocumentParsing.PARSER_REVISION,
                        "fixture",
                        "3.12.16",
                        "e".repeat(64),
                        "f".repeat(64),
                        mapOf("snooty" to "fixture"),
                    ),
                state = if (diagnostics.isEmpty()) DocumentState.READY else DocumentState.PARTIAL,
                pages = roots.map { (id, node) -> DocumentPage(id, node, node, emptyList(), true) },
                diagnostics = diagnostics,
                unresolvedUrls = emptyList(),
            )
    }

    private fun prefix(role: String) =
        mapOf(
                "group" to "grp.",
                "expression" to "exp.",
                "update" to "up.",
                "query" to "op.",
                "projection" to "proj.",
                "window" to "window.",
                "pipeline" to "pipe.",
            )
            .getValue(role)

    private fun rule(
        context: OperatorContext,
        primary: String,
        roles: List<String> = listOf(primary),
        fallback: List<String> = emptyList(),
        sections: List<String> = emptyList(),
        id: String = context.name,
    ) =
        CatalogRule(
            id,
            context,
            CatalogFormat.TABLE_FIRST_COLUMN,
            "reference/catalog.txt",
            false,
            sections,
            emptyList(),
            listOf("Name"),
            emptyList(),
            roles,
            primary,
            fallback,
            1,
        )

    private data class Case(
        val source: Source,
        val document: DocumentReport,
        val plan: DiscoveryPlan,
    )

    private fun median(
        root: Path,
        context: OperatorContext = OperatorContext.GROUP_ACCUMULATOR,
        catalogRole: String = "group",
        missingInput: Boolean = false,
    ): Case {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val input = if (missingInput) "\n.. literalinclude:: /missing.js\n" else ""
        val source =
            Source(
                root,
                mapOf(
                    page to
                        "Median\n======\n\nDefinition\n----------\n\n.. group:: ${'$'}median\n\n   .. versionadded:: 7.0\n\nFeature\n-------\n\n.. versionadded:: 8.3\n$input",
                    catalog to
                        "Catalog\n=======\n\n.. list-table::\n   :header-rows: 1\n\n   * - Name\n     - Description\n   * - :$catalogRole:`${'$'}median`\n     - Includes :group:`${'$'}sum` in explanatory prose.\n",
                ),
            )
        val target =
            source.definition(page, "group", "${'$'}median", listOf(source.version(page, "7.0")))
        val extra =
            if (missingInput)
                listOf(
                    source.node(
                        "directive",
                        page,
                        source.line(page, ".. literalinclude:: /missing.js"),
                        name = "literalinclude",
                    )
                )
            else emptyList()
        val documentRoot =
            source.root(
                page,
                listOf(
                    source.section(
                        page,
                        "Median",
                        "median",
                        listOf(
                            source.section(page, "Definition", "definition", listOf(target)),
                            source.section(
                                page,
                                "Feature",
                                "feature",
                                listOf(source.version(page, "8.3")),
                            ),
                        ),
                    )
                ) + extra,
            )
        val catalogRoot =
            source.root(
                catalog,
                listOf(
                    source.section(
                        catalog,
                        "Catalog",
                        "catalog",
                        listOf(
                            source.table(
                                catalog,
                                catalogRole,
                                listOf("${'$'}median"),
                                description =
                                    listOf(source.reference(catalog, "group", "${'$'}sum")),
                            )
                        ),
                    )
                ),
            )
        val errors =
            if (missingInput)
                listOf(
                    DocumentDiagnostic(
                        "CannotOpenFile",
                        "manual/source/$page",
                        source.line(page, ".. literalinclude:: /missing.js"),
                        "ERROR",
                        "Missing example input",
                    )
                )
            else emptyList()
        val selectedRule =
            rule(
                context,
                if (context == OperatorContext.AGGREGATION_EXPRESSION) "expression" else "group",
                listOf(catalogRole),
                if (context == OperatorContext.AGGREGATION_EXPRESSION) listOf("group")
                else emptyList(),
            )
        return Case(
            source,
            source.report(mapOf(page to documentRoot, catalog to catalogRoot), errors),
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs = listOf(selectedRule),
            ),
        )
    }

    @Test
    fun keepsRootIntroductionsSeparateFromFeatureVersions(@TempDir root: Path) {
        val fixture = median(root)
        val result =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        val capability = result.catalog.capabilities.single()
        assertEquals("${'$'}median", capability.key.token)
        assertTrue(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertEquals(
            listOf("7.0"),
            capability.versions.filter { it.facet == null }.map { it.rawVersion },
        )
        assertEquals(
            EvidenceConfidence.EXPLICIT,
            capability.versions.single { it.facet == null }.confidence,
        )
        assertEquals("8.3", capability.versions.single { it.facet != null }.rawVersion)
        assertEquals(1, capability.facets.size)
        val proof =
            result.catalog.evidence.single {
                it.kind == EvidenceKind.VERSION &&
                    it.startLine ==
                        fixture.source
                            .line("reference/operator/median.txt", ".. versionadded:: 7.0")
                            .toLong()
            }
        assertEquals(hash("   .. versionadded:: 7.0\n".toByteArray()), proof.textSha256)
    }

    @Test
    fun neverCopiesAccumulatorIntroductionToExpressionAlias(@TempDir root: Path) {
        val fixture = median(root, OperatorContext.AGGREGATION_EXPRESSION, "expression")
        val result =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        assertEquals(
            OperatorContext.AGGREGATION_EXPRESSION,
            result.catalog.capabilities.single().key.context,
        )
        assertTrue(
            result.catalog.capabilities.single().versions.all {
                it.confidence == EvidenceConfidence.UNKNOWN
            }
        )
        assertTrue(result.diagnostics.any { it.code == "UNCERTAIN_VERSION_SCOPE" })
    }

    @Test
    fun missingExampleDoesNotInvalidateDefinitionDiscovery(@TempDir root: Path) {
        val fixture = median(root, missingInput = true)
        val result =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        assertTrue(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertEquals(ExtractionState.UNRESOLVED, result.catalog.capabilities.single().examples)
        assertTrue(result.diagnostics.any { it.domain == DiscoveryDomain.EXAMPLE })
        assertFalse(
            result.catalog.capabilities.any { it.key.token == "${'$'}sum" },
            "Description references are not catalog names",
        )
    }

    @Test
    fun countDropsPreserveUnassignedDefinitionsAndBlockAbsenceClaims(@TempDir root: Path) {
        val fixture = median(root)
        val plan =
            fixture.plan.copy(
                catalogs =
                    listOf(
                        fixture.plan.catalogs
                            .single()
                            .copy(headers = listOf("Changed upstream header"))
                    )
            )
        val result = CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, plan)
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertTrue(result.diagnostics.any { it.code == "CATALOG_COUNT_DROP" })
        assertEquals("grp.${'$'}median", result.unassignedDefinitions.single().target)
    }

    @Test
    fun inputOrderingDoesNotChangeTheCandidate(@TempDir root: Path) {
        val fixture = median(root)
        val first =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        val second =
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document.copy(pages = fixture.document.pages.reversed()),
                fixture.plan,
            )
        assertEquals(AuditJson.write(first), AuditJson.write(second))
    }

    @Test
    fun missingDefinitionRootCannotClaimCompleteness(@TempDir root: Path) {
        val fixture = median(root)
        val result =
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document,
                fixture.plan.copy(definitionRoots = listOf("wrong-root")),
            )
        assertTrue(result.diagnostics.any { it.code == "MISSING_DEFINITION_ROOT" })
        assertTrue(result.contexts.none { it.healthy })
    }

    @Test
    fun policyOrderingIsCanonicalAndReportStateIsValidated(@TempDir root: Path) {
        val fixture = median(root)
        val extra =
            fixture.plan.catalogs
                .single()
                .copy(
                    id = "secondary",
                    context = OperatorContext.AGGREGATION_EXPRESSION,
                    primaryDefinitionRole = "expression",
                    fallbackDefinitionRoles = listOf("group"),
                )
        val plan = fixture.plan.copy(catalogs = fixture.plan.catalogs + extra)
        val one = CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, plan)
        val two =
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document,
                plan.copy(catalogs = plan.catalogs.reversed()),
            )
        assertEquals(AuditJson.write(one), AuditJson.write(two))
        assertTrue(one.diagnostics.any { it.code == "AMBIGUOUS_CATALOG_CONTEXT" })
        assertEquals(one, AuditJson.discoveryReport(AuditJson.write(one)))
        assertThrows(Exception::class.java) {
            AuditJson.discoveryReport(AuditJson.write(one).replace("\"PARTIAL\"", "\"READY\""))
        }
    }

    @Test
    fun classifiesUpdateModifiersByTheirCatalogSection(@TempDir root: Path) {
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    "reference/operator/each.txt" to ".. update:: ${'$'}each\n",
                    "reference/operator/set.txt" to ".. update:: ${'$'}set\n",
                    catalog to
                        "Modifiers\n=========\n.. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :update:`${'$'}each`\n     - Modifier\n\nOperators\n=========\n.. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :update:`${'$'}set`\n     - Operator\n",
                ),
            )
        val roots =
            mapOf(
                "reference/operator/each.txt" to
                    source.root(
                        "reference/operator/each.txt",
                        listOf(
                            source.definition("reference/operator/each.txt", "update", "${'$'}each")
                        ),
                    ),
                "reference/operator/set.txt" to
                    source.root(
                        "reference/operator/set.txt",
                        listOf(
                            source.definition("reference/operator/set.txt", "update", "${'$'}set")
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(
                            source.section(
                                catalog,
                                "Modifiers",
                                "modifiers",
                                listOf(source.table(catalog, "update", listOf("${'$'}each"))),
                            ),
                            source.section(
                                catalog,
                                "Operators",
                                "operators",
                                listOf(source.table(catalog, "update", listOf("${'$'}set"))),
                            ),
                        ),
                    ),
            )
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs =
                    listOf(
                        rule(
                            OperatorContext.UPDATE_MODIFIER,
                            "update",
                            sections = listOf("modifiers"),
                        ),
                        rule(
                            OperatorContext.UPDATE_OPERATOR,
                            "update",
                            sections = listOf("operators"),
                        ),
                    ),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), plan)
        assertEquals(
            setOf(
                OperatorContext.UPDATE_MODIFIER to "${'$'}each",
                OperatorContext.UPDATE_OPERATOR to "${'$'}set",
            ),
            result.catalog.capabilities.map { it.key.context to it.key.token }.toSet(),
        )
        assertTrue(
            result.contexts
                .filter {
                    it.context in
                        setOf(OperatorContext.UPDATE_MODIFIER, OperatorContext.UPDATE_OPERATOR)
                }
                .all { it.healthy }
        )
    }

    @Test
    fun acceptsLabeledOperatorGroupsButRejectsProseMentions(@TempDir root: Path) {
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    "reference/operator/rank.txt" to ".. group:: ${'$'}rank\n",
                    "reference/operator/shift.txt" to ".. group:: ${'$'}shift\n",
                    catalog to "Window operators: :group:`${'$'}rank`, and :group:`${'$'}shift`\n",
                ),
            )
        val paragraph =
            source.node(
                "paragraph",
                catalog,
                children =
                    listOf(
                        source.node("text", catalog, text = "Window operators: "),
                        source.reference(catalog, "group", "${'$'}rank"),
                        source.node("text", catalog, text = ", and "),
                        source.reference(catalog, "group", "${'$'}shift"),
                    ),
            )
        val item =
            source.node(
                "listItem",
                catalog,
                children =
                    listOf(
                        paragraph,
                        source.node(
                            "paragraph",
                            catalog,
                            children =
                                listOf(
                                    source.node("text", catalog, text = "See also "),
                                    source.reference(catalog, "group", "${'$'}sum"),
                                ),
                        ),
                    ),
            )
        val list = source.node("list", catalog, children = listOf(item))
        val roots =
            mapOf(
                "reference/operator/rank.txt" to
                    source.root(
                        "reference/operator/rank.txt",
                        listOf(
                            source.definition("reference/operator/rank.txt", "group", "${'$'}rank")
                        ),
                    ),
                "reference/operator/shift.txt" to
                    source.root(
                        "reference/operator/shift.txt",
                        listOf(
                            source.definition(
                                "reference/operator/shift.txt",
                                "group",
                                "${'$'}shift",
                            )
                        ),
                    ),
                catalog to source.root(catalog, listOf(list)),
            )
        val policy =
            rule(OperatorContext.WINDOW_FUNCTION, "window", listOf("group"), listOf("group"))
                .copy(format = CatalogFormat.OPERATOR_REFERENCE_LIST)
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs = listOf(policy),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), plan)
        assertEquals(
            setOf("${'$'}rank", "${'$'}shift"),
            result.catalog.capabilities.map { it.key.token }.toSet(),
        )
        assertTrue(result.contexts.single { it.context == OperatorContext.WINDOW_FUNCTION }.healthy)
        val prose =
            paragraph.copy(
                children =
                    listOf(source.node("text", catalog, text = "For background see ")) +
                        paragraph.children.drop(1)
            )
        val changed =
            roots +
                (catalog to
                    source.root(
                        catalog,
                        listOf(list.copy(children = listOf(item.copy(children = listOf(prose))))),
                    ))
        val rejected = CatalogDiscovery.normalize(source.bundle, source.report(changed), plan)
        assertFalse(
            rejected.contexts.single { it.context == OperatorContext.WINDOW_FUNCTION }.healthy
        )
        assertTrue(rejected.diagnostics.any { it.code == "UNSUPPORTED_CATALOG_LIST_ROW" })
    }

    @Test
    fun forgedCatalogNameCannotCreateAnObservedCapability(@TempDir root: Path) {
        val fixture = median(root)
        fun forge(node: DocumentNode): DocumentNode =
            node.copy(
                text = node.text?.replace("median", "invented"),
                identifiers = node.identifiers.map { it.replace("median", "invented") },
                attributes =
                    node.attributes.mapValues { (_, value) -> value.replace("median", "invented") },
                children = node.children.map(::forge),
                arguments = node.arguments.map(::forge),
            )
        val document =
            fixture.document.copy(
                pages =
                    fixture.document.pages.map {
                        if (it.id == "reference/catalog.txt")
                            it.copy(raw = forge(it.raw!!), expanded = forge(it.expanded!!))
                        else it
                    }
            )
        val result = CatalogDiscovery.normalize(fixture.source.bundle, document, fixture.plan)
        assertTrue(result.catalog.capabilities.none { it.key.token == "${'$'}invented" })
        assertTrue(result.diagnostics.any { it.code == "UNVERIFIED_CATALOG_MEMBER" })
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
    }

    @Test
    fun persistedReportsMustStillMatchRawOrigins(@TempDir root: Path) {
        val fixture = median(root)
        val first = fixture.document.pages.first()
        val forged = first.raw!!.copy(origin = first.raw.origin.copy(sha256 = "0".repeat(64)))
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document.copy(
                    pages = listOf(first.copy(raw = forged)) + fixture.document.pages.drop(1)
                ),
                fixture.plan,
            )
        }
        val generated =
            first.raw.copy(
                origin =
                    first.raw.origin.copy(
                        precision = DocumentPrecision.GENERATED_OR_UNKNOWN,
                        reportedLine = 1,
                    )
            )
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document.copy(
                    pages = listOf(first.copy(raw = generated)) + fixture.document.pages.drop(1)
                ),
                fixture.plan,
            )
        }
    }

    @Test
    fun unknownDependencyKindsCannotBeClaimedAsOnlyExampleFailures(@TempDir root: Path) {
        val fixture = median(root)
        val document =
            fixture.document.copy(
                state = DocumentState.PARTIAL,
                pages =
                    fixture.document.pages.map {
                        if (it.id.endsWith("median.txt")) it.copy(dependenciesComplete = false)
                        else it
                    },
            )
        val result = CatalogDiscovery.normalize(fixture.source.bundle, document, fixture.plan)
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertTrue(
            result.diagnostics.any {
                it.code == "UNEXPLAINED_DEPENDENCY_GRAPH" && it.domain == DiscoveryDomain.DEFINITION
            }
        )
    }

    @Test
    fun failedIncludesRemainReachableEvenWhenTheyProduceNoNodes(@TempDir root: Path) {
        val fixture = median(root)
        val page = fixture.document.pages.first { it.id.endsWith("median.txt") }
        val include =
            fixture.source.node(
                "directive",
                page.id,
                name = "include",
                arguments = listOf(fixture.source.node("text", page.id, text = "/missing.rst")),
            )
        val roots =
            fixture.document.pages.map {
                if (it.id == page.id)
                    it.copy(
                        raw = it.raw!!.copy(children = it.raw.children + include),
                        expanded = it.expanded!!.copy(children = it.expanded.children + include),
                    )
                else it
            }
        val document =
            fixture.document.copy(
                state = DocumentState.PARTIAL,
                pages = roots,
                diagnostics =
                    listOf(
                        DocumentDiagnostic(
                            "CannotOpenFile",
                            "manual/source/missing.rst",
                            1,
                            "ERROR",
                            "Include could not be parsed",
                        )
                    ),
            )
        val result = CatalogDiscovery.normalize(fixture.source.bundle, document, fixture.plan)
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertTrue(
            result.diagnostics.any {
                it.code == "CannotOpenFile" && it.domain == DiscoveryDomain.DEFINITION
            }
        )
    }

    @Test
    fun rejectsConflictingPrimaryRolePoliciesBeforeScanning(@TempDir root: Path) {
        val fixture = median(root)
        val second =
            fixture.plan.catalogs
                .single()
                .copy(id = "conflicting", primaryDefinitionRole = "expression")
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document,
                fixture.plan.copy(catalogs = fixture.plan.catalogs + second),
            )
        }
    }

    @Test
    fun observesPositionalFormsInRealCatalogRowShapes(@TempDir root: Path) {
        val catalog = "reference/catalog.txt"
        val tokens = listOf("$", "$[]", "$[<identifier>]")
        val definitions =
            tokens
                .mapIndexed { i, token ->
                    "reference/operator/position$i.txt" to ".. update:: $token\n"
                }
                .toMap()
        val source =
            Source(
                root,
                definitions +
                    (catalog to
                        (".. list-table::\n   :header-rows: 1\n\n   * - Name\n     - Description\n" +
                            tokens.joinToString("") {
                                "   * - :update:`$it`\n     - Positional operator\n"
                            })),
            )
        val roots =
            definitions.keys
                .mapIndexed { i, page ->
                    page to source.root(page, listOf(source.definition(page, "update", tokens[i])))
                }
                .toMap() +
                (catalog to source.root(catalog, listOf(source.table(catalog, "update", tokens))))
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs = listOf(rule(OperatorContext.UPDATE_OPERATOR, "update")),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), plan)
        assertEquals(tokens.toSet(), result.catalog.capabilities.map { it.key.token }.toSet())
        assertTrue(result.contexts.single { it.context == OperatorContext.UPDATE_OPERATOR }.healthy)
    }

    @Test
    fun nestedDefinitionHeadingsAndPageNoticesCannotIntroduceTheRoot(@TempDir root: Path) {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        "Median\n======\n\n.. versionadded:: 6.0\n\nDefinition\n----------\n.. group:: ${'$'}median\n\nExamples\n--------\nDefinition\n~~~~~~~~~~\n.. versionadded:: 8.3\n",
                    catalog to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :group:`${'$'}median`\n     - Description\n",
                ),
            )
        val definition =
            source.section(
                page,
                "Definition",
                "definition",
                listOf(source.definition(page, "group", "${'$'}median")),
            )
        val feature =
            source.section(
                page,
                "Examples",
                "examples",
                listOf(
                    source.section(
                        page,
                        "Definition",
                        "nested-definition",
                        listOf(source.version(page, "8.3")),
                    )
                ),
            )
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(
                            source.section(
                                page,
                                "Median",
                                "median",
                                listOf(source.version(page, "6.0"), definition, feature),
                            )
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median"))),
                    ),
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots),
                DiscoveryPlan(
                    product = "mongodb",
                    definitionRoots = listOf("reference/operator"),
                    catalogs = listOf(rule(OperatorContext.GROUP_ACCUMULATOR, "group")),
                ),
            )
        val versions = result.catalog.capabilities.single().versions
        assertEquals(
            EvidenceConfidence.UNKNOWN,
            versions.single { it.rawVersion == "6.0" }.confidence,
        )
        assertNotNull(versions.single { it.rawVersion == "8.3" }.facet)
        assertFalse(
            versions.any {
                it.kind == LifecycleEvent.INTRODUCED &&
                    it.facet == null &&
                    it.confidence == EvidenceConfidence.EXPLICIT
            }
        )
    }

    @Test
    fun nestedListVersionsAreNotAssignedToTheParentCatalogOperator(@TempDir root: Path) {
        val page = "reference/operator/rank.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to ".. group:: ${'$'}rank\n",
                    catalog to "- :group:`${'$'}rank`\n\n  - A mode:\n\n    .. versionadded:: 8.3\n",
                ),
            )
        val nested =
            source.node(
                "list",
                catalog,
                children =
                    listOf(
                        source.node(
                            "listItem",
                            catalog,
                            children = listOf(source.version(catalog, "8.3")),
                        )
                    ),
            )
        val item =
            source.node(
                "listItem",
                catalog,
                children =
                    listOf(
                        source.node(
                            "paragraph",
                            catalog,
                            children = listOf(source.reference(catalog, "group", "${'$'}rank")),
                        ),
                        nested,
                    ),
            )
        val roots =
            mapOf(
                page to source.root(page, listOf(source.definition(page, "group", "${'$'}rank"))),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.node("list", catalog, children = listOf(item))),
                    ),
            )
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs =
                    listOf(
                        rule(
                                OperatorContext.WINDOW_FUNCTION,
                                "window",
                                listOf("group"),
                                listOf("group"),
                            )
                            .copy(format = CatalogFormat.OPERATOR_REFERENCE_LIST)
                    ),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), plan)
        assertTrue(result.catalog.capabilities.single().versions.isEmpty())
        assertTrue(result.diagnostics.any { it.code == "UNSCOPED_CATALOG_VERSION" })
    }

    @Test
    fun fallbackUseDoesNotProveNativeContextCompleteness(@TempDir root: Path) {
        val expr = "reference/catalog.txt"
        val groups = "reference/groups.txt"
        val median = "reference/operator/median.txt"
        val sum = "reference/operator/sum.txt"
        val source =
            Source(
                root,
                mapOf(
                    median to ".. group:: ${'$'}median\n",
                    sum to ".. group:: ${'$'}sum\n",
                    expr to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :expression:`${'$'}median`\n     - Expression\n",
                    groups to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :group:`${'$'}sum`\n     - Accumulator\n",
                ),
            )
        val roots =
            mapOf(
                median to
                    source.root(median, listOf(source.definition(median, "group", "${'$'}median"))),
                sum to source.root(sum, listOf(source.definition(sum, "group", "${'$'}sum"))),
                expr to
                    source.root(
                        expr,
                        listOf(source.table(expr, "expression", listOf("${'$'}median"))),
                    ),
                groups to
                    source.root(groups, listOf(source.table(groups, "group", listOf("${'$'}sum")))),
            )
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs =
                    listOf(
                        rule(
                            OperatorContext.AGGREGATION_EXPRESSION,
                            "expression",
                            listOf("expression"),
                            listOf("group"),
                        ),
                        rule(OperatorContext.GROUP_ACCUMULATOR, "group").copy(page = groups),
                    ),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), plan)
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
        assertTrue(
            result.contexts.single { it.context == OperatorContext.AGGREGATION_EXPRESSION }.healthy
        )
        assertTrue(result.unassignedDefinitions.any { it.target == "grp.${'$'}median" })
    }

    @Test
    fun preservesRawSubstitutedVersionArgumentsAsUnknown(@TempDir root: Path) {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val rawVersion = "{+release+}"
        val source =
            Source(
                root,
                mapOf(
                    page to ".. group:: ${'$'}median\n\n   .. versionadded:: $rawVersion\n",
                    catalog to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :group:`${'$'}median`\n     - Description\n",
                ),
            )
        val version =
            source.node(
                "directive",
                page,
                source.line(page, ".. versionadded:: $rawVersion"),
                name = "versionadded",
                arguments = listOf(source.node("text", page, text = "7.0")),
            )
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(source.definition(page, "group", "${'$'}median", listOf(version))),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median"))),
                    ),
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots),
                DiscoveryPlan(
                    product = "mongodb",
                    definitionRoots = listOf("reference/operator"),
                    catalogs = listOf(rule(OperatorContext.GROUP_ACCUMULATOR, "group")),
                ),
            )
        val event = result.catalog.capabilities.single().versions.single()
        assertEquals(rawVersion, event.rawVersion)
        assertNull(event.version)
        assertEquals(EvidenceConfidence.UNKNOWN, event.confidence)
    }

    @Test
    fun repeatedGeneratedReferencesUseHonestWholeFileEvidence(@TempDir root: Path) {
        val file = "includes/catalog.yaml"
        val body = "  :group:`${'$'}median`\n  :group:`${'$'}median`\n"
        val source = Source(root, mapOf(file to body))
        val node =
            source.node("ref_role", file, name = "group").let {
                it.copy(
                    origin =
                        it.origin.copy(
                            reportedLine = null,
                            precision = DocumentPrecision.GENERATED_OR_UNKNOWN,
                        )
                )
            }
        val index = SourceEvidenceIndex(source.bundle)
        val proof = index.membership(node, "${'$'}median", listOf("group"), "catalog:median")!!
        assertEquals(1L, proof.startLine)
        assertEquals(2L, proof.endLine)
        assertEquals(hash(body.toByteArray()), proof.textSha256)
    }

    @Test
    fun cliEmitsPartialCandidatesAndRejectsInvalidInputs(@TempDir root: Path) {
        val fixture = median(root)
        val document = root.resolve("document.json")
        val plan = root.resolve("plan.json")
        Files.writeString(document, AuditJson.write(fixture.document))
        Files.writeString(plan, AuditJson.write(fixture.plan))
        fun invoke(args: List<String>): Pair<Int, String> {
            val output = root.resolve("stdout.json")
            val error = root.resolve("stderr.txt")
            val command =
                listOf(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    "dev.morphia.audits.discovery.DiscoveryMainKt",
                ) + args
            val process =
                ProcessBuilder(command)
                    .redirectOutput(output.toFile())
                    .redirectError(error.toFile())
                    .start()
            assertTrue(process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS))
            return process.exitValue() to
                Files.readString(if (process.exitValue() == 2) error else output)
        }
        val args =
            listOf(
                root.toString(),
                fixture.source.bundle.manifest.contentSha256,
                document.toString(),
                plan.toString(),
            )
        val valid = invoke(args)
        assertEquals(3, valid.first)
        assertEquals(DiscoveryState.PARTIAL, AuditJson.discoveryReport(valid.second).state)
        val invalid = invoke(emptyList())
        assertEquals(2, invalid.first)
        assertTrue(invalid.second.contains("DISCOVERY_INVALID"))
        val original = Files.readString(document)
        assertEquals(AuditJson.write(fixture.document), original)
    }

    @Test
    fun normalizesTheRealPinnedParserContract(@TempDir root: Path) {
        val python = System.getProperty("audits.parser.python")
        org.junit.jupiter.api.Assumptions.assumeTrue(
            python != null,
            "Explicit locked parser runtime required for integration",
        )
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        ".. default-domain:: mongodb\n\nMedian\n======\n\nDefinition\n----------\n\n.. group:: ${'$'}median\n\n   .. versionadded:: 7.0\n\nFeature\n-------\n\n.. versionadded:: 8.3\n",
                    catalog to
                        ".. default-domain:: mongodb\n\nCatalog\n=======\n\n.. list-table::\n   :header-rows: 1\n\n   * - Name\n     - Description\n   * - :group:`${'$'}median`\n     - Accumulator\n",
                ),
            )
        val bundle = SourceBundle.open(root, source.bundle.manifest.contentSha256)
        val document =
            DocumentParsing.parse(
                bundle,
                listOf(page, catalog),
                Path.of(python!!),
                Path.of(System.getProperty("audits.parser.adapter")),
            )
        val result =
            CatalogDiscovery.normalize(
                bundle,
                document,
                DiscoveryPlan(
                    product = "mongodb",
                    definitionRoots = listOf("reference/operator"),
                    catalogs = listOf(rule(OperatorContext.GROUP_ACCUMULATOR, "group")),
                ),
            )
        assertTrue(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy,
            result.diagnostics.toString(),
        )
        val cap = result.catalog.capabilities.single()
        assertEquals("7.0", cap.versions.single { it.facet == null }.rawVersion)
        assertEquals("8.3", cap.versions.single { it.facet != null }.rawVersion)
        assertEquals(
            EvidenceConfidence.EXPLICIT,
            cap.versions.single { it.facet == null }.confidence,
        )
    }

    @Test
    fun referenceAndExampleSharedIncludeFailuresDoNotHideDefinitions(@TempDir root: Path) {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        ".. group:: ${'$'}median\n\n:doc:`Uncaptured </missing>`\n\nExamples\n--------\n\n.. sharedinclude:: driver/example.rst\n",
                    catalog to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :group:`${'$'}median`\n     - Description\n",
                ),
            )
        val ref =
            source.node(
                "ref_role",
                page,
                source.line(page, ":doc:`Uncaptured </missing>`"),
                name = "doc",
                domain = "std",
                attributes = mapOf("target" to "\"/missing\""),
            )
        val shared =
            source.node(
                "directive",
                page,
                source.line(page, ".. sharedinclude:: driver/example.rst"),
                name = "sharedinclude",
                arguments = listOf(source.node("text", page, text = "driver/example.rst")),
            )
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(
                            source.definition(page, "group", "${'$'}median"),
                            ref,
                            source.section(page, "Examples", "examples", listOf(shared)),
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median"))),
                    ),
            )
        val errors =
            listOf(
                DocumentDiagnostic(
                    "CannotOpenFile",
                    "manual/source/$page",
                    ref.origin.reportedLine,
                    "ERROR",
                    "Reference target unavailable",
                ),
                DocumentDiagnostic(
                    "CannotOpenFile",
                    "manual/source/$page",
                    shared.origin.reportedLine,
                    "ERROR",
                    "Shared example unavailable",
                ),
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots, errors),
                DiscoveryPlan(
                    product = "mongodb",
                    definitionRoots = listOf("reference/operator"),
                    catalogs = listOf(rule(OperatorContext.GROUP_ACCUMULATOR, "group")),
                ),
            )
        assertTrue(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy,
            result.diagnostics.toString(),
        )
        assertTrue(
            result.diagnostics.any {
                it.domain == DiscoveryDomain.REFERENCE && it.code == "CannotOpenFile"
            }
        )
        assertTrue(
            result.diagnostics.any {
                it.domain == DiscoveryDomain.EXAMPLE && it.code == "CannotOpenFile"
            }
        )
        assertEquals(ExtractionState.UNRESOLVED, result.catalog.capabilities.single().examples)
    }

    @Test
    fun normalizedCatalogUsesExistingCoverageContracts(@TempDir root: Path) {
        val fixture = median(root)
        val discovery =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        val apiRoot = Files.createDirectory(root.resolve("api"))
        Files.writeString(
            apiRoot.resolve("Factories.java"),
            """
            package fixture;
            public class Factories {
                /**
                 * @aggregation.expression ${'$'}median
                 * @mongodb.server.release 7.0
                 */
                public static Object median(Object value) { return value; }
            }
            """
                .trimIndent(),
        )
        val api =
            dev.morphia.audits.api.MorphiaApiInventory.capture(apiRoot, "fixture", listOf("."))
        val key = discovery.catalog.capabilities.single().key
        val ref = CapabilityRef(key, null)
        val plan =
            CoveragePlan(
                product = "mongodb",
                contexts =
                    OperatorContext.entries.map {
                        ContextExpectation(
                            it,
                            if (it == key.context) 1 else 0,
                            if (it == key.context) listOf(ref) else emptyList(),
                            emptyList(),
                        )
                    },
                api = ApiExpectation(listOf("."), 2, 1),
                bindings =
                    listOf(
                        ApiBinding(
                            "median",
                            ref,
                            ApiSelector("fixture.Factories", "median"),
                            BindingKind.TYPED,
                        )
                    ),
                exclusions = emptyList(),
                omissions = emptyList(),
            )
        assertEquals(
            emptyList<AuditProblem>(),
            ContractValidation.validate(discovery.catalog, plan, api),
        )
        val report = CoverageComparison.compare(discovery.catalog, plan, api)
        assertNotEquals(ReportState.INVALID, report.state, report.problems.toString())
        assertEquals(
            ApiState.TYPED_PRESENT,
            report.capabilities.single { it.capability == ref }.api,
        )
    }

    private fun groupPlan() =
        DiscoveryPlan(
            product = "mongodb",
            definitionRoots = listOf("reference/operator"),
            catalogs = listOf(rule(OperatorContext.GROUP_ACCUMULATOR, "group")),
        )

    private val groupCatalog =
        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :group:`${'$'}median`\n     - Description\n"

    @Test
    fun multiLineDocLinksAreReferenceProblemsNotDefinitionFailures(@TempDir root: Path) {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        ".. group:: ${'$'}median\n\nSee the explanation on the\n:doc:`missing page </missing>` for details.\n",
                    catalog to groupCatalog,
                ),
            )
        // Snooty reports inline roles at the paragraph's first line, not the role's own line.
        val paragraph = source.line(page, "See the explanation on the")
        val ref =
            source.node(
                "ref_role",
                page,
                paragraph,
                name = "doc",
                domain = "std",
                attributes = mapOf("target" to "\"/missing\""),
            )
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(
                            source.definition(page, "group", "${'$'}median"),
                            source.node("paragraph", page, paragraph, children = listOf(ref)),
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median"))),
                    ),
            )
        val error =
            DocumentDiagnostic(
                "CannotOpenFile",
                "manual/source/$page",
                paragraph,
                "ERROR",
                "Reference target unavailable",
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots, listOf(error)),
                groupPlan(),
            )
        assertTrue(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy,
            result.diagnostics.toString(),
        )
        assertEquals(
            DiscoveryDomain.REFERENCE,
            result.diagnostics.single { it.code == "CannotOpenFile" }.domain,
        )
    }

    @Test
    fun sharedIncludeFailuresAreScopedPerIncludingPage(@TempDir root: Path) {
        val median = "reference/operator/median.txt"
        val sum = "reference/operator/sum.txt"
        val shared = "includes/shared.rst"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    median to ".. group:: ${'$'}median\n\nExamples\n--------\n",
                    sum to ".. group:: ${'$'}sum\n\nDefinition\n----------\n",
                    shared to ".. sharedinclude:: driver/example.rst\n",
                    catalog to groupCatalog + "   * - :group:`${'$'}sum`\n     - Description\n",
                ),
            )
        fun include() =
            source.node(
                "directive",
                shared,
                1,
                name = "sharedinclude",
                arguments = listOf(source.node("text", shared, text = "driver/example.rst")),
            )
        val roots =
            mapOf(
                median to
                    source.root(
                        median,
                        listOf(
                            source.definition(median, "group", "${'$'}median"),
                            source.section(median, "Examples", "examples", listOf(include())),
                        ),
                    ),
                sum to
                    source.root(
                        sum,
                        listOf(
                            source.definition(sum, "group", "${'$'}sum"),
                            source.section(sum, "Definition", "definition", listOf(include())),
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median", "${'$'}sum"))),
                    ),
            )
        val error =
            DocumentDiagnostic(
                "CannotOpenFile",
                "manual/source/$shared",
                1,
                "ERROR",
                "Shared include unavailable",
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots, listOf(error)),
                groupPlan(),
            )
        val problems = result.diagnostics.filter { it.code == "CannotOpenFile" }
        assertTrue(problems.any { it.domain == DiscoveryDomain.EXAMPLE && it.page == median })
        assertTrue(problems.any { it.domain == DiscoveryDomain.DEFINITION && it.page == sum })
        assertFalse(
            result.contexts.single { it.context == OperatorContext.GROUP_ACCUMULATOR }.healthy
        )
    }

    @Test
    fun storedReportsMustMatchTheirParseInputPin(@TempDir root: Path) {
        val fixture = median(root)
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.normalize(
                fixture.source.bundle,
                fixture.document.copy(inputSha256 = "0".repeat(64)),
                fixture.plan,
            )
        }
    }

    @Test
    fun parserReportedHintMissDoesNotBindAnotherLine(@TempDir root: Path) {
        val file = "reference/catalog.txt"
        val body =
            "   * - :group:`${'$'}median`\n     - Unlike other operators\n\nWindow functions:\n  :group:`${'$'}median`\n"
        val source = Source(root, mapOf(file to body))
        val index = SourceEvidenceIndex(source.bundle)
        fun at(line: Int) =
            index.membership(
                source.node("ref_role", file, line, name = "group"),
                "${'$'}median",
                listOf("group"),
                "catalog:median:$line",
            )
        assertEquals(1L, at(1)!!.startLine)
        assertNull(at(2), "A match outside the reported paragraph must not stand in for it")
        assertEquals(5L, at(4)!!.startLine, "Inline roles are reported at the paragraph start")
    }

    @Test
    fun deeplyNestedDefinitionKeepsItsOwnRootIntroduction(@TempDir root: Path) {
        val page = "reference/operator/median.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        "Page\n====\n\nOperators\n---------\n\nGroup\n~~~~~\n\n.. group:: ${'$'}median\n\n   .. versionadded:: 7.0\n",
                    catalog to groupCatalog,
                ),
            )
        val target =
            source.definition(page, "group", "${'$'}median", listOf(source.version(page, "7.0")))
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(
                            source.section(
                                page,
                                "Page",
                                "page",
                                listOf(
                                    source.section(
                                        page,
                                        "Operators",
                                        "operators",
                                        listOf(
                                            source.section(page, "Group", "group", listOf(target))
                                        ),
                                    )
                                ),
                            )
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(source.table(catalog, "group", listOf("${'$'}median"))),
                    ),
            )
        val result = CatalogDiscovery.normalize(source.bundle, source.report(roots), groupPlan())
        val event = result.catalog.capabilities.single().versions.single()
        assertNull(event.facet)
        assertEquals(EvidenceConfidence.EXPLICIT, event.confidence)
    }

    @Test
    fun featureHeadingsMustExistInRawSource(@TempDir root: Path) {
        val fixture = median(root)
        val page = fixture.document.pages.first { it.id.endsWith("median.txt") }
        fun moveHeadings(node: DocumentNode): DocumentNode =
            node.copy(
                origin =
                    if (node.kind == "heading") node.origin.copy(reportedLine = 1) else node.origin,
                children = node.children.map(::moveHeadings),
            )
        val document =
            fixture.document.copy(
                pages =
                    fixture.document.pages.map {
                        if (it.id == page.id)
                            it.copy(
                                raw = moveHeadings(it.raw!!),
                                expanded = moveHeadings(it.expanded!!),
                            )
                        else it
                    }
            )
        val result = CatalogDiscovery.normalize(fixture.source.bundle, document, fixture.plan)
        val capability = result.catalog.capabilities.single()
        assertTrue(capability.facets.isEmpty())
        assertTrue(capability.versions.none { it.facet != null })
        assertTrue(result.diagnostics.any { it.code == "UNVERIFIED_FEATURE_SCOPE" })
    }

    @Test
    fun sectionNoticesOnMultiOperatorPagesStayUnknown(@TempDir root: Path) {
        val page = "reference/operator/regex.txt"
        val catalog = "reference/catalog.txt"
        val source =
            Source(
                root,
                mapOf(
                    page to
                        "Regex\n=====\n\n.. query:: ${'$'}regex\n\n.. query:: ${'$'}options\n\nBehavior\n--------\n\n.. versionadded:: 8.3\n",
                    catalog to
                        ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :query:`${'$'}regex`\n     - Description\n   * - :query:`${'$'}options`\n     - Description\n",
                ),
            )
        val roots =
            mapOf(
                page to
                    source.root(
                        page,
                        listOf(
                            source.section(
                                page,
                                "Regex",
                                "regex",
                                listOf(
                                    source.definition(page, "query", "${'$'}regex"),
                                    source.definition(page, "query", "${'$'}options"),
                                    source.section(
                                        page,
                                        "Behavior",
                                        "behavior",
                                        listOf(source.version(page, "8.3")),
                                    ),
                                ),
                            )
                        ),
                    ),
                catalog to
                    source.root(
                        catalog,
                        listOf(
                            source.table(catalog, "query", listOf("${'$'}regex", "${'$'}options"))
                        ),
                    ),
            )
        val result =
            CatalogDiscovery.normalize(
                source.bundle,
                source.report(roots),
                DiscoveryPlan(
                    product = "mongodb",
                    definitionRoots = listOf("reference/operator"),
                    catalogs = listOf(rule(OperatorContext.QUERY_PREDICATE, "query")),
                ),
            )
        val features =
            result.catalog.capabilities.flatMap { it.versions }.filter { it.facet != null }
        assertEquals(2, features.size)
        assertTrue(features.all { it.confidence == EvidenceConfidence.UNKNOWN })
    }

    @Test
    fun rejectsOverlappingOrRepeatedCatalogSelectors(@TempDir root: Path) {
        val fixture = median(root)
        val base = fixture.plan.catalogs.single()
        listOf(
                base.copy(ignoredHeaders = listOf("Name")),
                base.copy(sections = listOf("A"), excludedSections = listOf("A")),
                base.copy(referenceRoles = listOf("group", "group")),
            )
            .forEach { rule ->
                assertThrows(IllegalArgumentException::class.java) {
                    CatalogDiscovery.normalize(
                        fixture.source.bundle,
                        fixture.document,
                        fixture.plan.copy(catalogs = listOf(rule)),
                    )
                }
            }
    }

    @Test
    fun reportDecodingRejectsForgedProvenanceAndDanglingEvidence(@TempDir root: Path) {
        val fixture = median(root)
        val report =
            CatalogDiscovery.normalize(fixture.source.bundle, fixture.document, fixture.plan)
        val json = AuditJson.write(report)
        AuditJson.discoveryReport(json)
        val forged =
            json.replace(
                ":parse-input:${report.documentInputSha256}",
                ":parse-input:" + "1".repeat(64),
            )
        assertNotEquals(json, forged)
        assertThrows(Exception::class.java) { AuditJson.discoveryReport(forged) }
        val dangling =
            report.catalog.evidence.first().id.let { id ->
                json.replaceFirst("\"id\" : \"$id\"", "\"id\" : \"evidence-forged\"")
            }
        assertNotEquals(json, dangling)
        assertThrows(Exception::class.java) { AuditJson.discoveryReport(dangling) }
    }

    @Test
    fun cliExitsZeroWhenEveryContextIsReady(@TempDir root: Path) {
        val roles =
            mapOf(
                OperatorContext.QUERY_PREDICATE to "query",
                OperatorContext.QUERY_PROJECTION to "projection",
                OperatorContext.UPDATE_OPERATOR to "update",
                OperatorContext.UPDATE_MODIFIER to "update",
                OperatorContext.PIPELINE_STAGE to "pipeline",
                OperatorContext.AGGREGATION_EXPRESSION to "expression",
                OperatorContext.GROUP_ACCUMULATOR to "group",
                OperatorContext.WINDOW_FUNCTION to "window",
            )
        val tokens = roles.keys.associateWith { "${'$'}op${it.ordinal}" }
        fun definition(context: OperatorContext) = "reference/operator/${context.key()}.txt"
        fun catalog(context: OperatorContext) = "reference/${context.key()}.txt"
        val source =
            Source(
                root,
                roles
                    .flatMap { (context, role) ->
                        listOf(
                            definition(context) to ".. $role:: ${tokens.getValue(context)}\n",
                            catalog(context) to
                                ".. list-table::\n   :header-rows: 1\n   * - Name\n     - Description\n   * - :$role:`${tokens.getValue(context)}`\n     - Description\n",
                        )
                    }
                    .toMap(),
            )
        val roots =
            roles
                .flatMap { (context, role) ->
                    val token = tokens.getValue(context)
                    listOf(
                        definition(context) to
                            source.root(
                                definition(context),
                                listOf(source.definition(definition(context), role, token)),
                            ),
                        catalog(context) to
                            source.root(
                                catalog(context),
                                listOf(source.table(catalog(context), role, listOf(token))),
                            ),
                    )
                }
                .toMap()
        val plan =
            DiscoveryPlan(
                product = "mongodb",
                definitionRoots = listOf("reference/operator"),
                catalogs =
                    roles.map { (context, role) ->
                        rule(context, role).copy(page = catalog(context))
                    },
            )
        val document = root.resolve("document.json")
        val planFile = root.resolve("plan.json")
        Files.writeString(document, AuditJson.write(source.report(roots)))
        Files.writeString(planFile, AuditJson.write(plan))
        val output = root.resolve("stdout.json")
        val process =
            ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    "dev.morphia.audits.discovery.DiscoveryMainKt",
                    root.toString(),
                    source.bundle.manifest.contentSha256,
                    document.toString(),
                    planFile.toString(),
                )
                .redirectOutput(output.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        assertTrue(process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals(0, process.exitValue())
        assertEquals(
            DiscoveryState.READY,
            AuditJson.discoveryReport(Files.readString(output)).state,
        )
    }

    @Test
    fun acceptsOnlyCanonicalPositionalIdentities() {
        listOf("$", "$[]", "$[<identifier>]", "${'$'}median").forEach {
            assertTrue(OperatorTokens.valid(it), it)
        }
        listOf("$[element]", "$[ ]", "${'$'}foo.bar", "${'$'}foo-bar", "").forEach {
            assertFalse(OperatorTokens.valid(it), it)
        }
        assertEquals("$[]", ContractValidation.operatorToken("$[] positional update"))
    }
}
