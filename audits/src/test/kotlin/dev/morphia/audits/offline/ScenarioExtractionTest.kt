package dev.morphia.audits.offline

import dev.morphia.audits.coverage.*
import dev.morphia.audits.documents.*
import dev.morphia.audits.naming.*
import dev.morphia.audits.scenarios.*
import dev.morphia.audits.semantics.ResultOrder
import dev.morphia.audits.shell.*
import dev.morphia.audits.sources.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ScenarioExtractionTest {
    @TempDir lateinit var temporary: Path
    private val bundleRoots = java.util.IdentityHashMap<VerifiedSourceBundle, Path>()
    private val bytes = "Synthetic owned AST fixture, not upstream source evidence.\n".toByteArray()

    private fun sha(bytes: ByteArray) =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private val origin
        get() =
            DocumentOrigin(
                "manual/source/index.txt",
                sha(bytes),
                1,
                DocumentPrecision.PARSER_REPORTED,
                listOf("manual/source/index.txt"),
            )

    private fun node(
        kind: String,
        text: String? = null,
        name: String? = null,
        children: List<DocumentNode> = emptyList(),
        arguments: List<DocumentNode> = emptyList(),
        options: Map<String, String> = emptyMap(),
        attributes: Map<String, String> = emptyMap(),
    ) =
        DocumentNode(
            kind,
            origin,
            text,
            name,
            null,
            emptyList(),
            options,
            attributes,
            arguments,
            children,
        )

    private fun section(title: String, anchor: String, vararg children: DocumentNode) =
        node(
            "section",
            children =
                listOf(
                    node(
                        "heading",
                        children = listOf(node("text", text = title)),
                        attributes = mapOf("id" to AuditJson.write(anchor).trim()),
                    )
                ) + children,
        )

    private fun code(source: String, language: String = "javascript") =
        node("code", text = source, attributes = mapOf("lang" to AuditJson.write(language).trim()))

    private fun io(
        input: String = "db.items.find({v:1});",
        output: String = "[{v:NumberInt(1)}]",
        reference: String? = null,
    ) =
        node(
            "directive",
            name = "io-code-block",
            children =
                listOf(
                    node(
                        "directive",
                        name = "input",
                        children = listOf(code(input)),
                        arguments = reference?.let { listOf(node("text", text = it)) }.orEmpty(),
                    ),
                    node("directive", name = "output", children = listOf(code(output))),
                ),
        )

    private fun root(vararg groups: DocumentNode) =
        node(
            "root",
            children =
                listOf(section("Operator", "operator", section("Examples", "examples", *groups))),
        )

    private fun rule() =
        ScenarioPageRule(
            "query",
            "index.txt",
            ScenarioScope(
                CapabilityRef(
                    CapabilityKey("mongodb-server", OperatorContext.QUERY_PREDICATE, "${'$'}eq"),
                    null,
                ),
                "mongosh",
            ),
            ExampleDestination("fixture.GeneratedEq", "dev/morphia/test/generated/eq"),
            listOf("examples"),
            emptyList(),
            1,
            listOf("javascript", "shell"),
            listOf("shell"),
            listOf("csharp", "python", "sql"),
            listOf("nodejs", "csharp"),
            ShellDialect.MONGOSH,
            ResultOrder.ORDERED,
        )

    private fun plan(rule: ScenarioPageRule = rule()) =
        ScenarioExtractionPlan(catalogSha256 = "a".repeat(64), pages = listOf(rule))

    private fun fixture(
        tree: DocumentNode,
        extra: Map<String, String> = emptyMap(),
        complete: Boolean = true,
        links: Map<String, String> = emptyMap(),
    ): Pair<VerifiedSourceBundle, DocumentReport> {
        val directory = Files.createTempDirectory(temporary, "bundle-")
        Files.createDirectory(directory.resolve("blobs"))
        val resources =
            (mapOf(
                    "manual/source/index.txt" to bytes.decodeToString(),
                    "manual/snooty.toml" to "name='fixture'\n",
                ) + extra + links)
                .map { (path, text) ->
                    val content = text.toByteArray()
                    val digest = sha(content)
                    Files.write(directory.resolve("blobs/$digest"), content)
                    val gitHash = MessageDigest.getInstance("SHA-1")
                    gitHash.update("blob ${content.size}\u0000".toByteArray(Charsets.US_ASCII))
                    gitHash.update(content)
                    GitResource(
                        path,
                        if (path in links) GitResourceKind.SYMLINK else GitResourceKind.FILE,
                        HexFormat.of().formatHex(gitHash.digest()),
                        digest,
                        content.size.toLong(),
                        links[path],
                    )
                }
        val request =
            GitSourceRequest(
                id = "fixture",
                uri = "https://example.invalid/docs.git",
                revision = "0".repeat(40),
                projectRoot = "manual",
                label = "8.0",
                role = SourceRole.CURRENT,
                channel = SourceChannel.RELEASED,
                roots = listOf("manual"),
                allowedSymlinkRoots =
                    if (links.isEmpty()) emptyList() else listOf("manual/source/code"),
                maximumFiles = 100,
                maximumFileBytes = 262144,
                maximumTotalBytes = 1048576,
                timeoutSeconds = 10,
            )
        val bundle =
            VerifiedSourceBundle(
                directory,
                GitSourceManifest(
                    request = request,
                    tree = "0".repeat(40),
                    gitVersion = "fixture",
                    contentSha256 = GitSnapshot.contentHash(request, "0".repeat(40), resources),
                    state = SourceState.READY,
                    resources = resources,
                    problems = emptyList(),
                ),
            )
        Files.writeString(
            directory.resolve(SourceBundle.MANIFEST),
            AuditJson.write(bundle.manifest),
        )
        SourceBundle.open(directory, bundle.manifest.contentSha256)
        bundleRoots[bundle] = directory
        val dependencies =
            extra.keys.map { path ->
                DocumentDependency(
                    path.removePrefix("manual/source/"),
                    origin.copy(
                        path = path,
                        sha256 = resources.single { it.path == path }.sha256,
                        reportedLine = null,
                        precision = DocumentPrecision.GENERATED_OR_UNKNOWN,
                    ),
                    "c".repeat(128),
                )
            }
        val report =
            DocumentReport(
                sourceSha256 = bundle.manifest.contentSha256,
                inputSha256 = AuditJson.hash(DocumentParsing.export(bundle, listOf("index.txt"))),
                parser =
                    DocumentParser(
                        "snooty",
                        DocumentParsing.PARSER_REVISION,
                        "fixture",
                        "3.12.16",
                        "d".repeat(64),
                        "e".repeat(64),
                        emptyMap(),
                    ),
                state = if (complete) DocumentState.READY else DocumentState.PARTIAL,
                pages = listOf(DocumentPage("index.txt", tree, tree, dependencies, complete)),
                diagnostics = emptyList(),
                unresolvedUrls = emptyList(),
            )
        return bundle to report
    }

    private fun extract(
        tree: DocumentNode,
        rule: ScenarioPageRule = rule(),
    ): ScenarioExtractionReport {
        val (bundle, report) = fixture(tree)
        return ScenarioExtraction.extract(bundle, report, plan(rule))
    }

    @Test
    fun foreignLanguagePoliciesCannotHideDataFormatsWithCaseOrSpellingVariants() {
        val tree = root(section("Find Values", "values", io()))
        for (format in
            listOf(
                "JSON",
                "Json",
                "xml",
                "csv",
                "text/plain",
                "application/json",
                "future-format",
                "sh",
                "bash",
                "powershell",
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                extract(tree, rule().copy(foreignLanguages = listOf(format)))
            }
        }
        val setup =
            extract(
                root(
                    code("mongoimport --db test --collection orders --file orders.json", "bash"),
                    section("Find Values", "values", io()),
                )
            )
        assertEquals(ScenarioExtractionState.PARTIAL, setup.state)
        assertEquals(1, setup.pages.single().unassignedReferenceBlocks)
        val report = extract(tree, rule().copy(foreignLanguages = listOf("Python", "C#", "SQL")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
    }

    @Test
    fun nativeEmptyDefaultDirectiveNamespacesRemainSupported() {
        fun native(node: DocumentNode): DocumentNode =
            node.copy(
                domain = if (node.kind == "directive") "" else node.domain,
                children = node.children.map(::native),
                arguments = node.arguments.map(::native),
            )
        val report = extract(native(root(section("Find Values", "values", io()))))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(ScenarioVariant.REFERENCE, report.scenarios.single().variant)
        assertEquals(ShellLiteralState.NORMALIZED, report.scenarios.single().program!!.state)
    }

    @Test
    fun snippetTitlesFollowTheAuthoredAliasRatherThanItsPhysicalTarget() {
        val tree = root(section("Example", "example", io(reference = "/code/authored-median.js")))
        val (bundle, document) =
            fixture(
                tree,
                extra = mapOf("manual/source/code/physical.js" to "db.items.find({v:1});"),
                links = mapOf("manual/source/code/authored-median.js" to "physical.js"),
            )
        val report = ScenarioExtraction.extract(bundle, document, plan())
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        assertEquals("authored-median", scenario.naming!!.trace.displayTitle)
        assertEquals(
            "manual/source/code/physical.js",
            scenario.blocks.first().snippets.single().canonicalPath,
        )
    }

    @Test
    fun serializedAncestorAmplificationIsStoppedBeforeAllocatingTheReport() {
        val large =
            node(
                "directive",
                name = "tabs",
                options = mapOf("description" to AuditJson.write("x".repeat(4000)).trim()),
                children = List(20) { code("1") },
            )
        val (bundle, document) = fixture(root(section("Find Values", "values", io()), large))
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                ScenarioExtraction.extract(
                    bundle,
                    document,
                    plan(),
                    ScenarioExtractionLimits(
                        maximumMetadataCharacters = 8192,
                        maximumReportBytes = 16384,
                    ),
                )
            }
        assertTrue(failure.message.orEmpty().contains("Serialized artifact"), failure.toString())
        val value = mapOf("unicode" to "é \uD83D\uDE80", "nested" to listOf(1, "x"))
        val bytes = AuditJson.write(value).toByteArray(Charsets.UTF_8)
        AuditJson.requireMaximumSize(value, bytes.size)
        assertThrows(IllegalArgumentException::class.java) {
            AuditJson.requireMaximumSize(value, bytes.size - 1)
        }
    }

    @Test
    fun unknownDirectiveSelectorsAndDataFormatLabelsCannotElectVariants() {
        for (name in listOf("only", "selected-content", "new-selector")) {
            val report =
                extract(
                    root(
                        section(
                            "Find Values",
                            "values",
                            node("directive", name = name, children = listOf(io())),
                        )
                    )
                )
            assertEquals(ScenarioVariant.UNRESOLVED, report.scenarios.single().variant)
            assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        }
        val input =
            io()
                .copy(
                    children =
                        io().children.map {
                            if (it.name == "input")
                                it.copy(children = listOf(code("db.items.find({});", "json")))
                            else it
                        }
                )
        assertEquals(
            ScenarioVariant.UNRESOLVED,
            extract(root(section("Find Values", "values", input))).scenarios.single().variant,
        )
        assertThrows(IllegalArgumentException::class.java) {
            extract(
                root(section("Find Values", "values", io())),
                rule().copy(foreignLanguages = listOf("json")),
            )
        }
    }

    @Test
    fun labelOnlyTopLevelIdentityIsAppliedBeforeMissingIdentityChecks() {
        val label =
            node(
                    "target",
                    name = "label",
                    children =
                        listOf(node("target_identifier").copy(identifiers = listOf("stable-find"))),
                )
                .copy(domain = "std")
        val tree = node("root", children = listOf(section("Find Values", "find", label, io())))
        val report = extract(tree, rule().copy(sections = listOf("find")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(
            listOf("stable-find"),
            report.scenarios.single().naming!!.trace.location.explicitLabels,
        )
        assertNull(report.scenarios.single().naming!!.trace.location.enclosingContext)
        val proposal =
            NameReconciliation.propose(
                ExampleNameRegistry(entries = emptyList()),
                ScenarioExtraction.namingRequest(report, emptyList()),
            )
        assertEquals(NameProposalState.READY, proposal.state, proposal.problems.toString())
    }

    @Test
    fun sectionLabelsNeverNameOneDirectGroupWhenTheSectionAlsoOwnsSubsections() {
        val label =
            node(
                    "target",
                    name = "label",
                    children =
                        listOf(
                            node("target_identifier").copy(identifiers = listOf("whole-comparison"))
                        ),
                )
                .copy(domain = "std")
        val report =
            extract(
                root(
                    label,
                    section(
                        "Compare Values",
                        "compare",
                        io(),
                        section("Compare Arrays", "arrays", io(input = "db.items.find({v:[1]});")),
                    ),
                )
            )
        val direct = report.scenarios.single { it.sections.last().anchor == "compare" }
        assertFalse(direct.sections.last().leaf)
        assertTrue(direct.naming!!.trace.location.explicitLabels.isEmpty())
        assertTrue(
            report.scenarios.all {
                "whole-comparison" !in it.naming!!.trace.location.explicitLabels
            }
        )
    }

    @Test
    fun positionalSelectorsNeverBecomeStableSnippetIdentities() {
        val file = "manual/source/code/queries.js"
        fun selected(options: Map<String, String>) =
            io(reference = "/code/queries.js").let { group ->
                group.copy(
                    children =
                        group.children.map {
                            if (it.name == "input") it.copy(options = options) else it
                        }
                )
            }
        fun capture(options: Map<String, String>): ExtractedScenario {
            val (bundle, document) =
                fixture(
                    root(section("Find Values", "values", selected(options))),
                    mapOf(file to "db.items.find({v:1});"),
                )
            return ScenarioExtraction.extract(bundle, document, plan()).scenarios.single()
        }
        val positional = capture(mapOf("lines" to "\"1-2\""))
        assertNull(positional.blocks.first().snippets.single().identity)
        assertEquals(file, positional.blocks.first().snippets.single().canonicalPath)
        assertTrue(positional.naming!!.trace.location.snippetIds.isEmpty())
        val first = capture(mapOf("start-after" to "\"scalar-start\"", "dedent" to "0"))
        val cosmetic = capture(mapOf("start-after" to "\"scalar-start\"", "dedent" to "4"))
        assertNotNull(first.blocks.first().snippets.single().identity)
        assertEquals(
            first.blocks.first().snippets.single().identity,
            cosmetic.blocks.first().snippets.single().identity,
        )
    }

    @Test
    fun incompleteSemanticContextDoesNotAdvertiseAScenarioFingerprint() {
        val report = extract(root(section("Find Values", "values", io())))
        assertEquals(ShellLiteralState.NORMALIZED, report.scenarios.single().program!!.state)
        assertNotNull(report.scenarios.single().program!!.semanticSha256)
        assertNull(report.scenarios.single().naming!!.trace.location.fingerprint)
    }

    @Test
    fun cliReportsFailedStdoutInsteadOfSuccessfulArtifactCreation() {
        val (bundle, document) = fixture(root(section("Find Values", "values", io())))
        val documents = temporary.resolve("documents.json")
        Files.writeString(documents, AuditJson.write(document))
        val policy = temporary.resolve("plan.json")
        Files.writeString(policy, AuditJson.write(plan()))
        val javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java")
        val cp =
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"))
        fun run(output: Path): Pair<Int, String> {
            val error = temporary.resolve("cli-error.log")
            val process =
                ProcessBuilder(
                        javaExecutable.toString(),
                        "-cp",
                        cp,
                        "dev.morphia.audits.scenarios.ScenariosMainKt",
                        requireNotNull(bundleRoots[bundle]).toString(),
                        bundle.manifest.contentSha256,
                        documents.toString(),
                        policy.toString(),
                    )
                    .redirectOutput(output.toFile())
                    .redirectError(error.toFile())
                    .start()
            if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly()
                fail<Unit>("Scenario CLI timed out")
            }
            return process.exitValue() to Files.readString(error)
        }
        val successful = run(temporary.resolve("report.json"))
        assertEquals(0, successful.first, successful.second)
        val full = Path.of("/dev/full")
        if (Files.exists(full)) {
            val failed = run(full)
            assertEquals(2, failed.first, failed.second)
            assertTrue(failed.second.contains("stdout"), failed.second)
        }
    }

    @Test
    fun repeatedHeadingsUseDistinctAuthoredSnippetNamesWithoutOrdinalSuffixes() {
        val tree =
            root(
                section(
                    "Compare Equality",
                    "equality",
                    io(reference = "/code/scalar-equality.js"),
                    io(reference = "/code/array-equality.js", input = "db.items.find({v:[1]});"),
                )
            )
        val (bundle, document) =
            fixture(
                tree,
                mapOf(
                    "manual/source/code/scalar-equality.js" to "db.items.find({v:1});",
                    "manual/source/code/array-equality.js" to "db.items.find({v:[1]});",
                ),
            )
        val report = ScenarioExtraction.extract(bundle, document, plan())
        val proposal =
            NameReconciliation.propose(
                ExampleNameRegistry(entries = emptyList()),
                ScenarioExtraction.namingRequest(report, emptyList()),
            )
        assertEquals(NameProposalState.READY, proposal.state, proposal.problems.toString())
        assertEquals(
            setOf("scalarEquality", "arrayEquality"),
            proposal.resolutions.map { it.stem }.toSet(),
        )
        assertEquals(
            setOf("Compare Equality"),
            report.scenarios.map { it.sections.last().title }.toSet(),
        )
    }

    @Test
    fun failedForeignIncludeIsIsolatedButMixedOwnershipStillBlocksTheScope() {
        val include =
            node(
                    "directive",
                    name = "sharedinclude",
                    arguments =
                        listOf(
                            node("text", text = "foreign.rst")
                                .copy(origin = origin.copy(reportedLine = 21))
                        ),
                )
                .copy(origin = origin.copy(reportedLine = 21))
        val foreign =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to "\"nodejs\""),
                children = listOf(include),
            )
        val (bundle, document) = fixture(root(section("Find Values", "values", io()), foreign))
        val diagnostic =
            DocumentDiagnostic(
                "CannotOpenFile",
                "manual/source/index.txt",
                21,
                "ERROR",
                "Foreign shared include is unavailable",
            )
        val report =
            ScenarioExtraction.extract(
                bundle,
                document.copy(state = DocumentState.PARTIAL, diagnostics = listOf(diagnostic)),
                plan(),
            )
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(
            "DECLARED_FOREIGN_VARIANT",
            report.pages.single().sourceDiagnostics.single().reason,
        )
        assertFalse(report.pages.single().sourceDiagnostics.single().blocksNaming)
        val mixed = root(section("Find Values", "values", io()), foreign, include)
        val unresolved =
            ScenarioExtraction.extract(
                bundle,
                document.copy(
                    state = DocumentState.PARTIAL,
                    diagnostics = listOf(diagnostic),
                    pages = document.pages.map { it.copy(raw = mixed, expanded = mixed) },
                ),
                plan(),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, unresolved.state)
        assertTrue(unresolved.pages.single().sourceDiagnostics.single().blocksNaming)
    }

    @Test
    fun scenarioPlanTransportRequiresExplicitPoliciesAndRejectsUnknownFields() {
        val json = AuditJson.write(plan())
        assertEquals(plan(), AuditJson.scenarioPlan(json))
        assertThrows(Exception::class.java) {
            AuditJson.scenarioPlan(json.replaceFirst("{", "{\"unknown\":true,"))
        }
        assertThrows(Exception::class.java) {
            AuditJson.scenarioPlan(json.replace("\"resultOrder\" : \"ORDERED\",", ""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            extract(
                root(section("Find Values", "values", io())),
                rule().copy(referenceLanguages = listOf("json")),
            )
        }
    }

    @Test
    fun incompleteScopeDoesNotBlockAnIndependentCompleteNamingScope() {
        val firstRule = rule()
        val otherScope =
            firstRule.scope.copy(
                capability =
                    CapabilityRef(firstRule.scope.capability.key.copy(token = "${'$'}ne"), null)
            )
        val secondRule =
            firstRule.copy(id = "other", scope = otherScope, sections = listOf("missing"))
        val (bundle, document) = fixture(root(section("Find Values", "values", io())))
        val report =
            ScenarioExtraction.extract(
                bundle,
                document,
                plan().copy(pages = listOf(firstRule, secondRule)),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.namingRequest(report, emptyList())
        }
        val names =
            ScenarioExtraction.namingRequest(
                report,
                emptyList(),
                completeScopes = listOf(firstRule.scope),
            )
        assertEquals(listOf(firstRule.scope), names.completeScopes)
        assertEquals(1, names.scenarios.size)
    }

    @Test
    fun unknownSelectorContainersAreRetainedAndCannotElectAReferenceVariant() {
        val selector =
            node(
                "future_selector",
                children = listOf(io()),
                attributes = mapOf("selection" to "\"unknown\""),
            )
        val report = extract(root(section("Find Values", "values", selector)))
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        val scenario = report.scenarios.single()
        assertEquals(ScenarioVariant.UNRESOLVED, scenario.variant)
        assertTrue(scenario.containers.any { it.kind == "future_selector" })
        assertNull(scenario.program)
    }

    @Test
    fun explicitSectionLabelsKeepNamesThroughAnchorAndTitleChanges() {
        val label =
            node(
                    "target",
                    name = "label",
                    children =
                        listOf(node("target_identifier").copy(identifiers = listOf("stable-query"))),
                )
                .copy(domain = "std")
        val firstTree = root(label, section("Find Scalar Values", "scalar", io()))
        val (bundle, document) = fixture(firstTree)
        val first = ScenarioExtraction.extract(bundle, document, plan())
        assertEquals(
            listOf("stable-query"),
            first.scenarios.single().naming!!.trace.location.explicitLabels,
        )
        val accepted =
            NameReconciliation.propose(
                    ExampleNameRegistry(entries = emptyList()),
                    ScenarioExtraction.namingRequest(first, emptyList()),
                )
                .registry
        val changedTree = root(label, section("Compare Values", "changed", io()))
        val changed =
            ScenarioExtraction.extract(
                bundle,
                document.copy(
                    pages =
                        document.pages.map { it.copy(raw = changedTree, expanded = changedTree) }
                ),
                plan(),
            )
        val proposal =
            NameReconciliation.propose(
                accepted,
                ScenarioExtraction.namingRequest(changed, emptyList()),
            )
        assertEquals(NameProposalState.READY, proposal.state, proposal.problems.toString())
        assertEquals("findScalarValues", proposal.resolutions.single().stem)
        assertEquals(NameMatch.STABLE_ID, proposal.resolutions.single().match)
    }

    @Test
    fun parserErrorsBlockSourceCompletenessWhileCrossReferenceErrorsRemainVisible() {
        val (bundle, document) = fixture(root(section("Find Values", "values", io())))
        val error =
            DocumentDiagnostic(
                "DocUtilsParseError",
                "manual/source/index.txt",
                1,
                "ERROR",
                "Unknown directive may hide examples",
            )
        val broken =
            ScenarioExtraction.extract(
                bundle,
                document.copy(state = DocumentState.PARTIAL, diagnostics = listOf(error)),
                plan(),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, broken.state)
        assertTrue(broken.problems.any { it.code == "PARSER_SOURCE_ERROR" })
        val crossReference = error.copy(code = "TargetNotFound")
        val retained =
            ScenarioExtraction.extract(
                bundle,
                document.copy(state = DocumentState.PARTIAL, diagnostics = listOf(crossReference)),
                plan(),
            )
        assertEquals(ScenarioExtractionState.READY, retained.state)
        assertEquals(listOf(crossReference), retained.diagnostics)
    }

    @Test
    fun failedDependenciesAndUnresolvedSnippetSourcesCannotPromoteNames() {
        val (bundle, document) =
            fixture(
                root(section("Find Values", "values", io(reference = "/missing.js"))),
                complete = false,
            )
        val report = ScenarioExtraction.extract(bundle, document, plan())
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertTrue(report.problems.any { it.code == "UNRESOLVED_SNIPPET" })
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.namingRequest(report, emptyList())
        }
    }

    @Test
    fun ownedIoProducesTypedProgramAndDescriptiveNameProposal() {
        val report = extract(root(section("Match a Scalar Value", "match-scalar", io())))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        assertEquals(
            listOf(ProgramRole.ACTION, ProgramRole.EXPECTED),
            scenario.blocks.map { it.role },
        )
        assertEquals(ShellLiteralState.NORMALIZED, scenario.program!!.state)
        val proposal =
            NameReconciliation.propose(
                ExampleNameRegistry(entries = emptyList()),
                ScenarioExtraction.namingRequest(report, emptyList()),
            )
        assertEquals(NameProposalState.READY, proposal.state, proposal.problems.toString())
        assertEquals("matchAScalarValue", proposal.resolutions.single().stem)
        assertEquals("testMatchAScalarValue", proposal.resolutions.single().method)
    }

    @Test
    fun displayAmbiguityDoesNotFabricateTypesOrEraseSourceIdentity() {
        val report =
            extract(root(section("Find Integral Averages", "averages", io(output = "[{avg:2}]"))))
        assertEquals(ScenarioExtractionState.READY, report.state)
        assertEquals(ShellLiteralState.NORMALIZED, report.scenarios.single().program!!.state)
        assertNull(report.scenarios.single().program!!.expectations.single().documents)
        assertEquals(
            dev.morphia.audits.published.PublishedWholeNumber(2),
            report.scenarios
                .single()
                .program!!
                .expectations
                .single()
                .published!!
                .single()
                .fields
                .single()
                .value,
        )
        assertFalse(report.scenarios.single().program!!.commands.isEmpty())
        assertNotNull(report.scenarios.single().naming)
        assertNull(report.scenarios.single().naming!!.trace.location.fingerprint)
    }

    @Test
    fun namedSnippetRecoversGenericHeadingAndRetainsSeparatePhysicalProvenance() {
        val path = "manual/source/code/median-rating-by-year.js"
        val tree =
            root(section("Example", "example", io(reference = "/code/median-rating-by-year.js")))
        val (bundle, document) = fixture(tree, mapOf(path to "db.items.find({v:1});"))
        val report = ScenarioExtraction.extract(bundle, document, plan())
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        assertEquals("median-rating-by-year", scenario.naming!!.trace.displayTitle)
        val snippet = scenario.blocks.first().snippets.single()
        assertEquals(path, snippet.canonicalPath)
        assertEquals("manual/source/index.txt", snippet.directive.origin.path)
        assertEquals(listOf(path), scenario.naming!!.trace.location.snippetIds)
    }

    @Test
    fun sourceRearrangementAndCosmeticTitlesPreservePersistedSnippetNames() {
        val oldTree =
            root(section("Match Scalar Values", "scalar", io(reference = "/code/scalar.js")))
        val extra =
            mapOf(
                "manual/source/code/scalar.js" to "db.items.find({v:1});",
                "manual/source/code/arrays.js" to "db.items.find({v:2});",
            )
        val (bundle, oldDocument) = fixture(oldTree, extra)
        val first = ScenarioExtraction.extract(bundle, oldDocument, plan())
        val accepted =
            NameReconciliation.propose(
                    ExampleNameRegistry(entries = emptyList()),
                    ScenarioExtraction.namingRequest(first, emptyList()),
                )
                .registry
        val newTree =
            root(
                section(
                    "Match Arrays",
                    "arrays",
                    io(input = "db.items.find({v:2});", reference = "/code/arrays.js"),
                ),
                section(
                    "Compare Scalar Values",
                    "renamed-scalar",
                    io(reference = "/code/scalar.js"),
                ),
            )
        val document =
            oldDocument.copy(
                pages = oldDocument.pages.map { it.copy(raw = newTree, expanded = newTree) }
            )
        val second = ScenarioExtraction.extract(bundle, document, plan())
        val names =
            NameReconciliation.propose(
                accepted,
                ScenarioExtraction.namingRequest(second, emptyList()),
            )
        assertEquals(NameProposalState.READY, names.state, names.problems.toString())
        assertEquals(
            setOf("matchScalarValues", "matchArrays"),
            names.resolutions.map { it.stem }.toSet(),
        )
        assertEquals(
            NameMatch.STABLE_ID,
            names.resolutions.single { it.stem == "matchScalarValues" }.match,
        )
    }

    @Test
    fun unassignedSetupBlocksPreventPartialScopeNameReconciliation() {
        val report =
            extract(
                root(code("db.items.insertOne({v:1});"), section("Match Values", "values", io()))
            )
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertEquals(1, report.pages.single().unassignedReferenceBlocks)
        assertEquals("db.items.insertOne({v:1});", report.pages.single().unassigned.single().source)
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.namingRequest(report, emptyList())
        }
    }

    @Test
    fun foreignTabsRemainVisibleWithoutClaimingEquivalentScenarios() {
        val tab =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to "\"nodejs\""),
                children = listOf(section("Node variant", "node", io())),
            )
        val report =
            extract(root(section("Match Values", "values", io()), tab, code("SELECT 1", "sql")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(1, report.scenarios.count { it.variant == ScenarioVariant.FOREIGN })
        assertNull(report.scenarios.single { it.variant == ScenarioVariant.FOREIGN }.naming)
        assertNull(report.scenarios.single { it.variant == ScenarioVariant.FOREIGN }.program)
        assertEquals("SELECT 1", report.pages.single().unassigned.single().source)
        assertEquals(1, ScenarioExtraction.namingRequest(report, emptyList()).scenarios.size)
    }

    @Test
    fun unknownLanguageAndMissingGroupsCannotBecomeEmptySuccess() {
        val unknown =
            io()
                .copy(
                    children =
                        listOf(
                            node(
                                "directive",
                                name = "input",
                                children =
                                    listOf(
                                        code("db.items.find({});", "javascript\n:broken-option:")
                                    ),
                            )
                        )
                )
        val report = extract(root(section("Match Values", "values", unknown)))
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertTrue(report.problems.any { it.code == "UNRESOLVED_LANGUAGE" })
        val countDrop =
            extract(
                root(section("Match Values", "values", io())),
                rule().copy(minimumReferenceGroups = 2),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, countDrop.state)
        assertTrue(countDrop.problems.any { it.code == "GROUP_COUNT_DROP" })
        val missing =
            extract(
                root(section("Match Values", "values", io())),
                rule().copy(sections = listOf("missing")),
            )
        assertTrue(missing.problems.any { it.code == "MISSING_SECTION" })
    }

    @Test
    fun duplicateGroupsRequireReviewInsteadOfOrdinalSuffixes() {
        val report = extract(root(section("Match Values", "values", io(), io())))
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertTrue(report.problems.any { it.code == "AMBIGUOUS_GROUP" })
        assertEquals(1, report.scenarios.map { it.id }.distinct().size)
    }

    @Test
    fun outputLanguageUncertaintyIsSeparateFromExplicitSourceOwnership() {
        val group =
            io()
                .copy(
                    children =
                        io().children.map {
                            if (it.name == "output")
                                it.copy(children = listOf(code("[{v:1}]", "json")))
                            else it
                        }
                )
        val report = extract(root(section("Match Values", "values", group)))
        assertEquals(ScenarioExtractionState.READY, report.state)
        assertNull(report.scenarios.single().program)
        assertEquals(
            "UNSUPPORTED_IO_OR_OUTPUT_LANGUAGE",
            report.scenarios.single().normalizationProblem,
        )
        assertNotNull(report.scenarios.single().naming)
    }

    @Test
    fun argumentEdgesAndResourceLimitsCannotHideSelectedCode() {
        val hidden =
            node(
                "directive",
                name = "unknown",
                arguments = listOf(code("db.items.insertOne({v:1});")),
            )
        val report = extract(root(hidden, section("Match Values", "values", io())))
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertTrue(report.pages.single().unassigned.single().locator.any { it < 0 })
        val (bundle, document) = fixture(root(section("Match Values", "values", io())))
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.extract(
                bundle,
                document,
                plan(),
                ScenarioExtractionLimits(maximumNodes = 4),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.extract(bundle, document.copy(inputSha256 = "f".repeat(64)), plan())
        }
    }

    @Test
    fun oversizedPublishedProgramRetainsItsSourceGroupWithoutAbortingExtraction() {
        val input = "const x='" + "\\x01".repeat(10_000) + "'; db.items.find({});"
        val output = "[{v:[" + List(1500) { "x" }.joinToString(",") + "]}]"
        val report =
            extract(root(section("Large Published Result", "large-result", io(input, output))))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        assertEquals(ShellLiteralState.UNSUPPORTED, scenario.program!!.state)
        assertEquals("STORED_VALUE_LIMIT", scenario.program!!.problems.single().code)
        assertTrue(scenario.program!!.commands.isEmpty())
        assertNotNull(scenario.naming)
    }

    @Test
    fun twoSubcapExpandedOutputsCannotAmplifyTheWholeReport() {
        val input = "const x='" + "\\x01".repeat(10_000) + "'; db.items.find({});"
        val output = "[{v:[" + List(600) { "x" }.joinToString(",") + "]}]"
        val report =
            extract(
                root(
                    section("First Large Result", "first-large", io(input, output)),
                    section("Second Large Result", "second-large", io(input, output)),
                )
            )
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(2, report.scenarios.size)
        report.scenarios.forEach {
            assertEquals("STORED_VALUE_LIMIT", it.program!!.problems.single().code)
            assertTrue(it.program!!.commands.isEmpty())
            assertNotNull(it.naming)
        }
    }

    @Test
    fun explicitlyOwnedIoResultsNormalizePrintedDocumentStreams() {
        val report =
            extract(root(section("Two Published Rows", "two-rows", io(output = "{v:1}\n{v:2}"))))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        val program = requireNotNull(scenario.program)
        assertEquals(ShellLiteralState.NORMALIZED, program.state, program.problems.toString())
        assertEquals(ProgramFormat.SHELL_RESULTS, program.sources.last().format)
        assertEquals("{v:1}\n{v:2}", program.sources.last().source)
        assertEquals(2, program.expectations.single().published!!.size)
        assertNotNull(scenario.naming)
    }
}
