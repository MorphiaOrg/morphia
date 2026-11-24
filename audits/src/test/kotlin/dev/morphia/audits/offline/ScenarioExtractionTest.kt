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
                                children = listOf(code("db.items.find({});", "pseudocode")),
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
                                it.copy(children = listOf(code("[{v:1}]", "python")))
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

    private fun assembledRule(
        layout: SectionLayout = SectionLayout.COMMANDS_THEN_RESULT,
        shared: List<String> = emptyList(),
        tabs: List<String> = emptyList(),
        aliases: List<String> = emptyList(),
    ) = rule().copy(sectionAssembly = SectionAssemblyPolicy(layout, shared, tabs, aliases))

    private fun standalone(title: String, anchor: String, vararg sources: String) =
        section(title, anchor, *sources.map { code(it) }.toTypedArray())

    @Test
    fun standaloneSectionAssignsTypedSetupActionAndUntouchedPrintedResults() {
        val seed = "db.items.insertMany([{v:1},{v:2}]);"
        val query = "db.items.find({});"
        val result = "{v:1}\n{v:2}"
        val tree = root(standalone("Read All Items", "read-all", seed, query, result))
        val old = extract(tree)
        assertEquals(3, old.pages.single().unassignedReferenceBlocks)
        val report = extract(tree, assembledRule())
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertTrue(report.pages.single().unassigned.isEmpty())
        val scenario = report.scenarios.single()
        assertEquals(
            listOf(ProgramRole.SETUP, ProgramRole.ACTION, ProgramRole.EXPECTED),
            scenario.blocks.map { it.role },
        )
        assertEquals(listOf(seed, query, result), scenario.blocks.map { it.source })
        val program = requireNotNull(scenario.program)
        assertEquals(ShellLiteralState.NORMALIZED, program.state, program.problems.toString())
        assertEquals(2, program.commands.size)
        assertEquals(2, program.expectations.single().published!!.size)
        assertEquals("readAllItems", DescriptiveNames.stem(scenario.naming!!.trace.displayTitle))
    }

    @Test
    fun genericExampleUsesAuthoredPageTitleAndDoesNotDependOnSiblingOrder() {
        val example = section("Example", "example", code("db.items.find({});"), code("[]"))
        val other = standalone("Find Positive Values", "positive", "db.items.find({v:1});", "[]")
        fun tree(vararg children: DocumentNode) =
            node("root", children = listOf(section("Absolute Value", "abs", *children)))
        val policy = assembledRule().copy(sections = listOf("example", "positive"))
        val first = extract(tree(example, other), policy)
        val reordered = extract(tree(other, example), policy)
        val name = first.scenarios.single { it.sections.last().anchor == "example" }.naming!!
        val second = reordered.scenarios.single { it.sections.last().anchor == "example" }.naming!!
        assertEquals("absoluteValue", DescriptiveNames.stem(name.trace.displayTitle))
        assertEquals(name, second)
    }

    @Test
    fun declaredSharedSetupIsOrderedAndNeverBorrowedFromFirstSibling() {
        val setup = standalone("Seed Items", "seed", "const n=2;", "db.items.insertOne({v:n});")
        val first = standalone("First Query", "first", "db.items.find({v:n});", "[{v:2}]")
        val second = standalone("Second Query", "second", "db.items.find({});", "[{v:2}]")
        val policy = assembledRule(shared = listOf("seed"))
        val report = extract(root(second, setup, first), policy)
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(2, report.scenarios.size)
        report.scenarios.forEach {
            assertEquals(
                ShellLiteralState.NORMALIZED,
                it.program!!.state,
                it.program!!.problems.toString(),
            )
            assertEquals(
                listOf(
                    ProgramRole.SETUP,
                    ProgramRole.SETUP,
                    ProgramRole.ACTION,
                    ProgramRole.EXPECTED,
                ),
                it.blocks.map { b -> b.role },
            )
            assertEquals("const n=2;", it.blocks.first().source)
        }
        // Without a declared share, a query inherits only the inserts an earlier section made
        // into its collection, with the declarations they read.
        val without = extract(root(setup, first, second), assembledRule())
        assertEquals(
            listOf("const n=2;", "db.items.insertOne({v:n});"),
            without.scenarios
                .single { it.sections.last().anchor == "second" }
                .blocks
                .filter { it.role == ProgramRole.SETUP }
                .map { it.source },
        )
        assertNull(without.scenarios.single { it.sections.last().anchor == "first" }.program)
    }

    @Test
    fun sharedSetupSkipsCommandsAndRejectsAnUnresolvedLanguage() {
        fun shared(setup: DocumentNode) =
            extract(
                root(
                    section("Shared", "shared", setup),
                    standalone("Read Items", "read", "db.items.find({});", "[]"),
                ),
                assembledRule(shared = listOf("shared")),
            )
        // A command the shared section shows is skipped, with its reason; the consumer still runs.
        val command = shared(code("db.items.find({});"))
        assertEquals(ScenarioExtractionState.READY, command.state, command.problems.toString())
        assertEquals(
            listOf("SHARED_SECTION_COMMAND"),
            command.pages.single().sharedSetupSkipped.map { it.reason },
        )
        assertNotNull(command.scenarios.single().program)
        val unresolved = shared(code("db.items.insertOne({v:1});", "bash"))
        assertEquals(ScenarioExtractionState.PARTIAL, unresolved.state)
        assertTrue(unresolved.pages.single().unassigned.isNotEmpty())
        assertTrue(unresolved.scenarios.all { it.naming == null && it.program == null })
        val missing =
            extract(
                root(standalone("Read Items", "read", "db.items.find({});", "[]")),
                assembledRule(shared = listOf("absent")),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, missing.state)
        assertTrue(missing.problems.any { it.code == "MISSING_OR_AMBIGUOUS_SHARED_SETUP" })
    }

    @Test
    fun sharedSetupAlsoPrecedesExplicitIoInputs() {
        val report =
            extract(
                root(
                    standalone("Shared Seed", "shared", "db.items.insertOne({v:1});"),
                    section("Read Items", "read", io()),
                ),
                assembledRule(shared = listOf("shared")),
            )
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        val scenario = report.scenarios.single()
        assertEquals(ProgramRole.SETUP, scenario.blocks.first().role)
        assertEquals(ShellLiteralState.NORMALIZED, scenario.program!!.state)
        assertEquals(2, scenario.program!!.commands.size)
    }

    @Test
    fun tabTitlesNameSourceOnlySamplesWithoutInventingSeeds() {
        fun tab(id: String, title: String) =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to AuditJson.write(id).trim()),
                arguments = listOf(node("text", text = title)),
                children =
                    listOf(
                        code("{v:1}", "bash"),
                        code("db.items.aggregate([]);", "bash"),
                        code("{v:1}", "bash"),
                    ),
            )
        val degrees = tab("degrees", "Inverse Cosine of Value in Degrees")
        val radians = tab("radians", "Inverse Cosine of Value in Radians")
        val policy =
            assembledRule(
                SectionLayout.SOURCE_ONLY,
                tabs = listOf("degrees", "radians"),
                aliases = listOf("bash"),
            )
        fun run(vararg variants: DocumentNode) =
            extract(
                root(
                    section(
                        "Example",
                        "example",
                        node("directive", name = "tabs", children = variants.toList()),
                    )
                ),
                policy,
            )
        val report = run(degrees, radians)
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(2, report.scenarios.size)
        assertTrue(
            report.scenarios.all {
                it.program == null &&
                    it.blocks.all { b -> b.role == null } &&
                    it.normalizationProblem == "SOURCE_ONLY_SECTION"
            }
        )
        val names = report.scenarios.map { it.naming!! }.sortedBy { it.id }
        assertEquals(
            setOf("inverseCosineOfValueInDegrees", "inverseCosineOfValueInRadians"),
            names.map { DescriptiveNames.stem(it.trace.displayTitle) }.toSet(),
        )
        assertEquals(names, run(radians, degrees).scenarios.map { it.naming!! }.sortedBy { it.id })
        assertEquals(2, names.map { it.trace.location.enclosingContext }.distinct().size)
    }

    @Test
    fun undeclaredTabsAndHiddenSelectorContainersCannotBecomeComplete() {
        val unknown =
            node(
                "directive",
                name = "future-selector",
                children = listOf(code("db.items.find({});")),
            )
        val report =
            extract(root(section("Read Items", "read", unknown, code("[]"))), assembledRule())
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertNull(report.scenarios.single().naming)
        val tab =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to "\"unknown\""),
                children = listOf(code("db.items.find({});"), code("[]")),
            )
        assertEquals(
            ScenarioExtractionState.PARTIAL,
            extract(root(section("Read Items", "read", tab)), assembledRule()).state,
        )
    }

    @Test
    fun splittingASectionKeepsEachActionIndependentAndCharged() {
        // Setup beside an action in one block is skipped rather than read as the action.
        val mixed =
            extract(
                    root(
                        standalone(
                            "Read Items",
                            "read",
                            "db.items.insertOne({v:1}); db.items.find({});",
                            "[{v:1}]",
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
                .single()
        assertNull(mixed.program)
        assertEquals(
            listOf("MIXED_SECTION_ROLES", "SAMPLE_DOCUMENT"),
            mixed.skipped.map { it.reason },
        )
        // Earlier actions don't count toward a later one's program limits.
        val finds = (1..70).map { "db.items.find({v:$it});" }.toTypedArray()
        val many = extract(root(standalone("Read Items", "read", *finds)), assembledRule())
        assertEquals(70, many.scenarios.count { it.program?.state == ShellLiteralState.NORMALIZED })
        // Each scenario a section splits into is charged against the group budget.
        val (bundle, document) =
            fixture(
                root(
                    standalone("Read Items", "read", "db.items.find({});", "db.items.find({v:1});")
                )
            )
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.extract(
                bundle,
                document,
                plan(assembledRule()),
                ScenarioExtractionLimits(maximumGroups = 1),
            )
        }
        // So is the shared setup each of them repeats: 26 + 18 + 21 selected, plus 26 twice.
        val seed = "db.items.insertOne({v:1});"
        val (sharedBundle, sharedDocument) =
            fixture(
                root(
                    standalone("Shared", "shared", seed),
                    standalone("Read Items", "read", "db.items.find({});", "db.items.find({v:1});"),
                )
            )
        fun charged(budget: Int) =
            ScenarioExtraction.extract(
                sharedBundle,
                sharedDocument,
                plan(assembledRule(shared = listOf("shared"))),
                ScenarioExtractionLimits(maximumCodeCharacters = budget),
            )
        assertEquals(2, charged(117).scenarios.size)
        assertThrows(IllegalArgumentException::class.java) { charged(116) }
        // A read after an update in the same section depends on it, so it isn't run alone.
        val reads =
            extract(
                    root(
                        standalone(
                            "Update Items",
                            "update",
                            "db.items.insertOne({v:1});",
                            "db.items.updateOne({}, {${'$'}set: {v: 2}});",
                            "db.items.find({});",
                            "[{v:2}]",
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
        val read = reads.single { it.blocks.any { b -> b.source == "db.items.find({});" } }
        assertEquals("AFTER_EARLIER_WRITE", read.normalizationProblem)
        assertNull(read.program)
        // So does a read in another input/output group after a write in an earlier one.
        val groups =
            extract(
                    root(
                        section(
                            "Merge Totals",
                            "merge",
                            io("db.items.aggregate([{${'$'}merge: {into: \"totals\"}}]);", "[]"),
                            io("db.totals.find({});", "[{v:1}]"),
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
        assertEquals(
            setOf(null, "AFTER_EARLIER_WRITE"),
            groups.map { it.normalizationProblem }.toSet(),
        )
    }

    @Test
    fun seedsFollowTheSetupEachScenarioRuns() {
        fun extracted(vararg nodes: DocumentNode, shared: List<String> = emptyList()) =
            extract(root(*nodes), assembledRule(shared = shared))
        fun seeds(scenario: ExtractedScenario) =
            scenario.blocks.filter { it.format == ProgramFormat.LEGACY_DOCUMENTS }

        // Shared declarations don't insert, so the section's sample still seeds.
        val declared =
            extracted(
                    standalone("Shared", "shared", "const n=1;"),
                    standalone("Read Items", "read", "{v:1}", "db.items.find({});", "[{v:1}]"),
                    shared = listOf("shared"),
                )
                .scenarios
                .single()
        assertEquals("items", seeds(declared).single().namespace!!.collection)
        assertEquals(ShellLiteralState.NORMALIZED, declared.program!!.state)
        // An insert after an action is another scenario's setup, not this one's.
        val first =
            extracted(
                    standalone(
                        "Read Items",
                        "read",
                        "{v:1}",
                        "db.items.find({});",
                        "db.items.insertOne({v:2});",
                        "db.items.find({v:2});",
                    )
                )
                .scenarios
                .single { it.blocks.any { b -> b.source == "db.items.find({});" } }
        assertEquals(1, seeds(first).size)
        // A shared sample beside a section's own insert only illustrates.
        val inserted =
            extracted(
                    standalone("Shared", "shared", "{v:9}"),
                    standalone(
                        "Read Items",
                        "read",
                        "db.items.insertOne({v:1});",
                        "db.items.find({});",
                    ),
                    shared = listOf("shared"),
                )
                .scenarios
                .single()
        assertTrue(seeds(inserted).isEmpty())
        assertEquals(listOf("SAMPLE_DOCUMENT"), inserted.skipped.map { it.reason })
        // A shared sample seeds a native input/output group's collection too.
        val native =
            extracted(
                    standalone("Shared", "shared", "{v:1}"),
                    section("Match Values", "values", io()),
                    shared = listOf("shared"),
                )
                .scenarios
                .single()
        assertEquals("items", seeds(native).single().namespace!!.collection)
        assertEquals(ShellLiteralState.NORMALIZED, native.program!!.state)
        // A section that inserts in plain code and queries in an input/output group runs its
        // insert before that query, and leaves no insert-only scenario behind.
        val inSection =
            extracted(section("Match Values", "values", code("db.items.insertOne({v:1});"), io()))
                .scenarios
                .single()
        assertEquals(
            listOf(ProgramRole.SETUP, ProgramRole.ACTION, ProgramRole.EXPECTED),
            inSection.blocks.map { it.role },
        )
        assertEquals(ShellLiteralState.NORMALIZED, inSection.program!!.state)
        // Setup the section shows after the group isn't the group's.
        val after =
            extracted(section("Match Values", "values", io(), code("db.items.insertOne({v:1});")))
                .scenarios
                .single { it.blocks.any { b -> b.role == ProgramRole.ACTION } }
        assertTrue(after.blocks.none { it.role == ProgramRole.SETUP }, after.blocks.toString())
    }

    @Test
    fun aQueryOnAnEarlierSectionsCollectionRunsAgainstItsInserts() {
        val scenarios =
            extract(
                    root(
                        standalone(
                            "Seed Items",
                            "seed",
                            "db.items.insertOne({v:1});",
                            "db.items.find({v:1});",
                        ),
                        standalone("Match Again", "again", "db.items.find({});", "[{v:1}]"),
                        standalone("Other Data", "other", "db.others.find({});"),
                    ),
                    assembledRule(),
                )
                .scenarios
        fun setup(action: String) =
            scenarios
                .single {
                    it.blocks.any { b -> b.role == ProgramRole.ACTION && b.source == action }
                }
                .let { it.blocks.filter { b -> b.role == ProgramRole.SETUP }.map { b -> b.source } }
        // The later section's query gets the earlier section's insert, but not its action.
        assertEquals(listOf("db.items.insertOne({v:1});"), setup("db.items.find({});"))
        // A query on another collection inherits nothing.
        assertEquals(emptyList<String>(), setup("db.others.find({});"))
        // A write the page shows but no scenario runs still changes what the next read sees.
        val bulk =
            extract(
                    root(
                        standalone(
                            "Bulk",
                            "bulk",
                            "db.items.insertOne({v:1});",
                            "try { db.items.bulkWrite([]) } catch (e) { print(e) }",
                            "db.items.find({});",
                            "[]",
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
                .single()
        assertEquals("AFTER_EARLIER_WRITE", bulk.normalizationProblem)
        // An inherited insert brings the declarations it reads.
        val declared =
            extract(
                    root(
                        standalone(
                            "Seed Items",
                            "seed",
                            "const docs = [{v:1}];",
                            "db.items.insertMany(docs);",
                            "db.items.find({v:1});",
                        ),
                        standalone("Match Again", "again", "db.items.find({});"),
                    ),
                    assembledRule(),
                )
                .scenarios
                .single { it.blocks.any { b -> b.source == "db.items.find({});" } }
        assertEquals(ShellLiteralState.NORMALIZED, declared.program!!.state)
        assertEquals(
            listOf("const docs = [{v:1}];", "db.items.insertMany(docs);"),
            declared.blocks.filter { it.role == ProgramRole.SETUP }.map { it.source },
        )
        // A pipeline that only mentions '$out' in a string writes nothing.
        val reads =
            extract(
                    root(
                        standalone(
                            "Project",
                            "project",
                            "db.items.insertOne({v:1});",
                            "db.items.aggregate([{${'$'}project: {v: '${'$'}out'}}]);",
                            "db.items.find({});",
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
        assertTrue(
            reads.none { it.normalizationProblem == "AFTER_EARLIER_WRITE" },
            reads.toString(),
        )
    }

    @Test
    fun inheritedDataFollowsWhatThePageLastLoaded() {
        fun query(vararg nodes: DocumentNode) =
            extract(root(*nodes), assembledRule()).scenarios.single {
                it.blocks.any { b -> b.source == "db.items.find({});" }
            }
        // An insert the grammar can't read may have replaced the older data: nothing carries over.
        val replaced =
            query(
                standalone("First", "first", "db.items.insertOne({v:1});", "db.items.find({v:1});"),
                standalone("Second", "second", "db.items.insertMany(makeDocuments());"),
                standalone("Third", "third", "db.items.find({});"),
            )
        assertTrue(
            replaced.blocks.none { it.role == ProgramRole.SETUP },
            replaced.blocks.toString(),
        )
        // An insert after a section's last input/output group still reaches the next section.
        val later =
            query(
                section(
                    "Group",
                    "group",
                    io("db.others.find({});", "[]"),
                    code("db.items.insertOne({v:1});"),
                ),
                standalone("Third", "third", "db.items.find({});"),
            )
        assertEquals(
            listOf("db.items.insertOne({v:1});"),
            later.blocks.filter { it.role == ProgramRole.SETUP }.map { it.source },
        )
        // A group after setup of its own the grammar can't read doesn't run without its data.
        val group =
            extract(
                    root(
                        section(
                            "Group",
                            "group",
                            code("db.items.insertMany(makeDocuments());"),
                            io(),
                        )
                    ),
                    assembledRule(),
                )
                .scenarios
                .single { it.blocks.any { b -> b.role == ProgramRole.ACTION } }
        assertNull(group.program)
        // Inherited setup counts against the source budget.
        val (bundle, document) =
            fixture(
                root(
                    standalone(
                        "Seed",
                        "seed",
                        "db.items.insertOne({v:1});",
                        "db.items.find({v:1});",
                    ),
                    *(1..5)
                        .map { standalone("Query $it", "q$it", "db.items.find({});") }
                        .toTypedArray(),
                )
            )
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.extract(
                bundle,
                document,
                plan(assembledRule()),
                ScenarioExtractionLimits(maximumCodeCharacters = 150),
            )
        }
    }

    @Test
    fun languagesAndTemplatesAreReadFromTheCodeNotItsPosition() {
        fun scenarios(vararg blocks: DocumentNode) =
            extract(root(section("Read Items", "read", *blocks)), assembledRule()).scenarios
        // The action's own language counts, even with no result after it.
        assertEquals(
            ScenarioVariant.UNRESOLVED,
            scenarios(code("db.items.insertOne({v:1});"), code("db.items.find({});", "pseudocode"))
                .single()
                .variant,
        )
        // A JSON sample before the action doesn't decide the language.
        assertEquals(
            ScenarioVariant.REFERENCE,
            scenarios(code("{\"v\": 1}", "json"), code("db.items.find({});")).single().variant,
        )
        // Angle brackets inside a string are a value, not a template.
        val quoted = scenarios(code("db.items.find({v:'<value>'});")).single()
        assertEquals(listOf(ProgramRole.ACTION), quoted.blocks.map { it.role })
        assertTrue(quoted.skipped.isEmpty())
        // Sibling selections are separate examples.
        fun selected(tab: String) =
            node(
                "directive",
                name = "selected-content",
                attributes = mapOf("selections" to "{\"language\":\"$tab\"}"),
                children = listOf(code("db.items.find({});")),
            )
        assertEquals(
            setOf(ScenarioVariant.REFERENCE, ScenarioVariant.FOREIGN),
            scenarios(selected("shell"), selected("nodejs")).map { it.variant }.toSet(),
        )
        // Branches that show the same code are still separate examples.
        val same =
            extract(
                root(section("Read Items", "read", selected("nodejs"), selected("csharp"))),
                assembledRule(),
            )
        assertEquals(2, same.scenarios.map { it.id }.distinct().size)
        assertTrue(same.problems.none { it.code == "AMBIGUOUS_GROUP" })
        // A displayed operator expression is neither data nor a result.
        val shown =
            scenarios(
                    code("// Specify i\n{ ${'$'}regexMatch: { input: 'a', regex: /a/i } }"),
                    code("db.items.find({});"),
                )
                .single()
        assertEquals(listOf("OPERATOR_EXPRESSION"), shown.skipped.map { it.reason })
        // A caption comment above printed results doesn't hide them.
        val captioned =
            scenarios(code("db.items.find({});"), code("// The result\n[{v:1}]")).single()
        assertEquals(
            listOf(ProgramRole.ACTION, ProgramRole.EXPECTED),
            captioned.blocks.map { it.role },
        )
        // A selection that names nothing stays unresolved.
        for (selections in listOf("{}", "[]")) {
            val empty =
                node(
                    "directive",
                    name = "selected-content",
                    attributes = mapOf("selections" to selections),
                    children = listOf(code("db.items.find({});")),
                )
            assertEquals(ScenarioVariant.UNRESOLVED, scenarios(empty).single().variant)
        }
        // A shared section that shows code only in other languages shares nothing.
        val foreignOnly =
            extract(
                root(
                    section("Shared", "shared", selected("nodejs"), selected("csharp")),
                    standalone("Read Items", "read", "db.items.find({});", "[]"),
                ),
                assembledRule(shared = listOf("shared")),
            )
        assertTrue(foreignOnly.problems.none { it.code == "MISSING_OR_AMBIGUOUS_SHARED_SETUP" })
        assertNotNull(
            foreignOnly.scenarios.single { it.variant == ScenarioVariant.REFERENCE }.program
        )
    }

    @Test
    fun sectionAssemblyReadsHowPagesPrintAndLabelTheirCode() {
        fun scenario(vararg blocks: DocumentNode, policy: SectionAssemblyPolicy? = null) =
            extract(
                    root(section("Read Items", "read", *blocks)),
                    policy?.let { rule().copy(sectionAssembly = it) } ?: assembledRule(),
                )
                .scenarios
                .single()
        // A seed may use the printed constructors the extended JSON reader rejects.
        val decimal = scenario(code("{ v: Decimal128(\"1.5\") }"), code("db.items.find({});"))
        assertEquals(ShellLiteralState.NORMALIZED, decimal.program!!.state)
        // A truncated listing leaves the example serialization-only.
        val truncated = scenario(code("db.items.find({});"), code("[ {v:1} ]\n..."))
        assertEquals(ShellLiteralState.NORMALIZED, truncated.program!!.state)
        assertEquals(listOf(ProgramRole.ACTION), truncated.blocks.map { it.role })
        assertEquals(listOf("UNREADABLE_RESULT"), truncated.skipped.map { it.reason })
        // Code the page leaves unhighlighted takes the reviewed language, when there is one.
        val unlabeled = code("db.items.find({});", "none")
        assertEquals(ScenarioVariant.UNRESOLVED, scenario(unlabeled).variant)
        val reviewed =
            SectionAssemblyPolicy(
                SectionLayout.COMMANDS_THEN_RESULT,
                unlabeledLanguage = "javascript",
            )
        assertEquals(ScenarioVariant.REFERENCE, scenario(unlabeled, policy = reviewed).variant)
        // A composable tutorial's selected content is classified like a tab.
        fun selected(tab: String) =
            node(
                "directive",
                name = "selected-content",
                attributes = mapOf("selections" to "{\"language\":\"$tab\"}"),
                children = listOf(code("db.items.find({});")),
            )
        assertEquals(ScenarioVariant.REFERENCE, scenario(selected("shell")).variant)
        assertEquals(ScenarioVariant.FOREIGN, scenario(selected("nodejs")).variant)
    }

    @Test
    fun sectionBlocksTakeTheirRolesFromTheShellGrammar() {
        fun scenarios(vararg blocks: String) =
            extract(root(standalone("Read Items", "read", *blocks)), assembledRule()).scenarios
        fun roles(scenario: ExtractedScenario) = scenario.blocks.map { it.role }
        val read = listOf(ProgramRole.SETUP, ProgramRole.ACTION, ProgramRole.EXPECTED)

        // A sample document seeds the collection where nothing inserts.
        val seeded = scenarios("{v:1}", "db.items.find({});", "[{v:1}]").single()
        assertEquals(ShellLiteralState.NORMALIZED, seeded.program!!.state)
        assertEquals(read, roles(seeded))
        assertEquals(ProgramFormat.LEGACY_DOCUMENTS, seeded.blocks.first().format)
        assertEquals("items", seeded.blocks.first().namespace!!.collection)
        // Where the section inserts, a sample document only illustrates.
        val inserted =
            scenarios("db.items.insertOne({v:1});", "{v:2}", "db.items.find({});", "[{v:1}]")
                .single()
        assertEquals(read, roles(inserted))
        assertEquals(listOf("SAMPLE_DOCUMENT"), inserted.skipped.map { it.reason })
        // Each action is its own scenario, with the result printed right after it, if any.
        val two = scenarios("db.items.find({});", "db.items.find({v:1});", "[]")
        assertEquals(
            setOf(listOf(ProgramRole.ACTION), listOf(ProgramRole.ACTION, ProgramRole.EXPECTED)),
            two.map(::roles).toSet(),
        )
        // A syntax template is skipped, and so is setup no action follows.
        val template =
            scenarios("{ ${'$'}match: { <query> } }", "db.items.find({});", "const n=1;").single()
        assertEquals(listOf(ProgramRole.ACTION), roles(template))
        assertEquals(
            listOf("SYNTAX_TEMPLATE", "TRAILING_SETUP"),
            template.skipped.map { it.reason },
        )
        // What a page prints after an update is a later read's result, kept for that read.
        val update =
            scenarios(
                    "db.items.insertOne({v:1});",
                    "db.items.updateOne({}, {${'$'}set: {v: 2}});",
                    "[{v:2}]",
                )
                .single()
        assertEquals(listOf(ProgramRole.SETUP, ProgramRole.ACTION), roles(update))
        assertEquals(listOf("UPDATE_RESULT"), update.skipped.map { it.reason })
        // A section without a command has nothing to run.
        assertEquals("MISSING_COMMAND_OR_RESULT", scenarios("{v:1}").single().normalizationProblem)
    }

    @Test
    fun repeatedSharedSourceIsChargedBeforeProgramConstruction() {
        val tree =
            root(
                standalone("Shared Seed", "shared", "db.items.insertOne({v:1});"),
                standalone("First Query", "first", "db.items.find({});", "[]"),
                standalone("Second Query", "second", "db.items.find({});", "[]"),
            )
        val (bundle, document) = fixture(tree)
        val originalCharacters =
            "db.items.insertOne({v:1});".length + 2 * ("db.items.find({});".length + 2)
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioExtraction.extract(
                bundle,
                document,
                plan(assembledRule(shared = listOf("shared"))),
                ScenarioExtractionLimits(maximumCodeCharacters = originalCharacters + 1),
            )
        }
    }

    @Test
    fun declaredSharedSectionOutsideExampleSelectionIsRetainedEvenWithoutConsumers() {
        val shared = standalone("Shared Seed", "shared", "db.items.insertOne({v:1});")
        val consumer =
            section(
                "Examples",
                "examples",
                standalone("Read Items", "read", "db.items.find({});", "[{v:1}]"),
            )
        val tree =
            node("root", children = listOf(section("Operator", "operator", shared, consumer)))
        val report = extract(tree, assembledRule(shared = listOf("shared")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(1, report.scenarios.size)
        assertEquals(
            "db.items.insertOne({v:1});",
            report.pages.single().sharedSetup.single().source,
        )
        assertEquals(2, report.scenarios.single().program!!.commands.size)
        val noConsumer =
            extract(
                node(
                    "root",
                    children =
                        listOf(
                            section("Operator", "operator", shared, section("Examples", "examples"))
                        ),
                ),
                assembledRule(shared = listOf("shared")),
            )
        assertEquals(ScenarioExtractionState.PARTIAL, noConsumer.state)
        assertEquals(1, noConsumer.pages.single().sharedSetup.size)
        assertTrue(noConsumer.scenarios.isEmpty())
    }

    @Test
    fun ownSetupLanguagesMustBeKnownReferenceLanguages() {
        for (seed in
            listOf(
                code("db.items.insertOne({v:1});", "python"),
                code("db.items.insertOne({v:1});", "bash"),
                node("code", text = "db.items.insertOne({v:1});"),
            )) {
            val report =
                extract(
                    root(
                        section("Read Items", "read", seed, code("db.items.find({});"), code("[]"))
                    ),
                    assembledRule(),
                )
            assertEquals(ScenarioExtractionState.PARTIAL, report.state)
            assertNull(report.scenarios.single().program)
            assertNull(report.scenarios.single().naming)
        }
    }

    @Test
    fun authoredScenarioTabsAlsoNameNativeIoAndQualifyNestedTitles() {
        fun tab(id: String, title: String, nested: Boolean) =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to AuditJson.write(id).trim()),
                arguments = listOf(node("text", text = title)),
                children = listOf(if (nested) section("Read Values", "read", io()) else io()),
            )
        val policy = assembledRule(tabs = listOf("degrees", "radians"))
        for (nested in listOf(false, true)) {
            val report =
                extract(
                    root(
                        section(
                            "Example",
                            "example",
                            node(
                                "directive",
                                name = "tabs",
                                children =
                                    listOf(
                                        tab("degrees", "Values In Degrees", nested),
                                        tab("radians", "Values In Radians", nested),
                                    ),
                            ),
                        )
                    ),
                    policy,
                )
            assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
            assertEquals(2, report.scenarios.size)
            val names = report.scenarios.map { it.naming!! }
            assertEquals(2, names.map { it.trace.location.enclosingContext }.distinct().size)
            if (nested)
                assertEquals(
                    setOf("Values In Degrees", "Values In Radians"),
                    names.map { it.contextTitles.first() }.toSet(),
                )
            else
                assertEquals(
                    setOf("Values In Degrees", "Values In Radians"),
                    names.map { it.trace.displayTitle }.toSet(),
                )
        }
    }

    @Test
    fun sharedDependencyOrderIsDeclaredAndNeverCanonicalizedAlphabetically() {
        val first = standalone("Initial Binding", "z-first", "const n=1;")
        val second = standalone("Seed Items", "a-second", "db.items.insertOne({v:n});")
        val consumer = standalone("Read Items", "read", "db.items.find({});", "[{v:1}]")
        val report =
            extract(
                root(second, consumer, first),
                assembledRule(shared = listOf("z-first", "a-second")),
            )
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(
            listOf("z-first", "a-second"),
            report.plan.pages.single().sectionAssembly!!.sharedSetupSections,
        )
        assertEquals(ShellLiteralState.NORMALIZED, report.scenarios.single().program!!.state)
        val reversed =
            extract(
                root(first, second, consumer),
                assembledRule(shared = listOf("a-second", "z-first")),
            )
        // In the other order the insert reads a binding not yet declared, so it isn't setup.
        assertEquals(
            listOf("db.items.insertOne({v:n});" to "UNSUPPORTED_SETUP"),
            reversed.pages.single().sharedSetupSkipped.map { it.code.source to it.reason },
        )
    }

    @Test
    fun sharedSelectionDoesNotSweepInUnselectedDescendants() {
        val shared =
            section(
                "Shared Seed",
                "shared",
                code("db.items.insertOne({v:1});"),
                standalone("Unrelated Child", "unrelated", "db.items.drop();"),
            )
        val tree =
            node(
                "root",
                children =
                    listOf(
                        section(
                            "Operator",
                            "operator",
                            shared,
                            section(
                                "Examples",
                                "examples",
                                standalone("Read Items", "read", "db.items.find({});", "[]"),
                            ),
                        )
                    ),
            )
        val report = extract(tree, assembledRule(shared = listOf("shared")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(1, report.pages.single().sharedSetup.size)
        assertTrue(report.scenarios.single().blocks.none { it.source.contains("drop") })
    }

    @Test
    fun explicitlyReviewedLegacyHighlightingRemainsClosedShellForInputAndOutput() {
        val tree =
            root(
                section(
                    "Read Items",
                    "read",
                    code("db.items.insertOne({v:1});", "bash"),
                    code("db.items.find({});", "bash"),
                    code("[{v:1}]", "bash"),
                )
            )
        val report = extract(tree, assembledRule(aliases = listOf("bash")))
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(ShellLiteralState.NORMALIZED, report.scenarios.single().program!!.state)
        val shellCommand =
            extract(
                root(standalone("Read Items", "read", "echo hello", "[]")),
                assembledRule(aliases = listOf("bash")),
            )
        assertNull(shellCommand.scenarios.single().program)
    }

    @Test
    fun nestedScenarioTabPathsAndPolicyValidationAreUnambiguous() {
        fun tab(id: String, children: List<DocumentNode>) =
            node(
                "directive",
                name = "tab",
                options = mapOf("tabid" to AuditJson.write(id).trim()),
                arguments = listOf(node("text", text = "Scenario $id")),
                children = children,
            )
        val nested = listOf("first", "second").map { tab(it, listOf(tab("inner", listOf(io())))) }
        val policy = assembledRule(tabs = listOf("first", "second", "inner"))
        val report =
            extract(
                root(
                    section(
                        "Example",
                        "example",
                        node("directive", name = "tabs", children = nested),
                    )
                ),
                policy,
            )
        assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
        assertEquals(
            2,
            report.scenarios.map { it.naming!!.trace.location.enclosingContext }.distinct().size,
        )
        assertEquals(
            setOf("Scenario first", "Scenario second"),
            report.scenarios.map { it.naming!!.contextTitles.first() }.toSet(),
        )
        for (bad in
            listOf(
                assembledRule(tabs = listOf("shell")),
                assembledRule(tabs = listOf("bad/path")),
                assembledRule(shared = listOf("seed", "seed")),
                assembledRule(aliases = listOf("python")),
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                extract(root(section("Read Items", "read", io())), bad)
            }
        }
        val unknown = node("directive", name = "future-selector", children = listOf(io()))
        assertEquals(
            ScenarioExtractionState.PARTIAL,
            extract(root(section("Read Items", "read", unknown)), policy).state,
        )
    }

    @Test
    fun nestedUnknownInputContainerCannotHideInsideAnIoOwner() {
        val input =
            node(
                "directive",
                name = "input",
                children =
                    listOf(
                        node(
                            "directive",
                            name = "future-selector",
                            children = listOf(code("db.items.find({});")),
                        )
                    ),
            )
        val output = node("directive", name = "output", children = listOf(code("[]")))
        val group = node("directive", name = "io-code-block", children = listOf(input, output))
        val report = extract(root(section("Read Items", "read", group)), assembledRule())
        assertEquals(ScenarioExtractionState.PARTIAL, report.state)
        assertNull(report.scenarios.single().program)
        assertNull(report.scenarios.single().naming)
    }

    @Test
    fun sharedShellSetupDoesNotReclassifyForeignStandaloneOrIoCandidates() {
        val seed = standalone("Shared Seed", "shared", "db.items.insertOne({v:1});")
        val reference = standalone("Read Items", "read", "db.items.find({});", "[]")
        val foreignIo =
            section(
                "Python Query",
                "foreign",
                node(
                    "directive",
                    name = "io-code-block",
                    children =
                        listOf(
                            node(
                                "directive",
                                name = "input",
                                children = listOf(code("db.items.find({});", "python")),
                            ),
                            node(
                                "directive",
                                name = "output",
                                children = listOf(code("[]", "javascript")),
                            ),
                        ),
                ),
            )
        val foreignSection =
            section(
                "Python Query",
                "foreign",
                code("db.items.find({});", "python"),
                code("[]", "json"),
            )
        val actualPython =
            section(
                "Python Query",
                "foreign",
                code("items.find({\"v\": True})", "python"),
                code("[]", "json"),
            )
        for (foreign in listOf(foreignIo, foreignSection, actualPython)) {
            val baseline = extract(root(reference, foreign), assembledRule())
            val report =
                extract(root(foreign, seed, reference), assembledRule(shared = listOf("shared")))
            assertEquals(
                ScenarioExtractionState.READY,
                baseline.state,
                baseline.problems.toString(),
            )
            assertEquals(ScenarioExtractionState.READY, report.state, report.problems.toString())
            assertEquals(1, report.pages.single().foreignGroups)
            val observed = report.scenarios.single { it.variant == ScenarioVariant.FOREIGN }
            assertEquals(
                baseline.scenarios
                    .single { it.variant == ScenarioVariant.FOREIGN }
                    .blocks
                    .map { it.source },
                observed.blocks.map { it.source },
            )
            assertEquals(2, observed.blocks.size)
            if (foreign !== foreignIo) {
                assertEquals("FOREIGN_SECTION", observed.normalizationProblem)
                assertTrue(observed.blocks.all { it.role == null })
            }
            assertNull(observed.program)
            assertNull(observed.naming)
            val executable = report.scenarios.single { it.variant == ScenarioVariant.REFERENCE }
            assertEquals(ShellLiteralState.NORMALIZED, executable.program!!.state)
            assertEquals(3, executable.blocks.size)
            assertEquals(1, report.pages.single().sharedSetup.size)
        }
    }
}
