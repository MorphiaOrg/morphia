package dev.morphia.audits.offline

import dev.morphia.audits.api.*
import dev.morphia.audits.coverage.*
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CoverageComparisonTest {
    private val key =
        CapabilityKey("mongodb-server", OperatorContext.GROUP_ACCUMULATOR, "${'$'}median")
    private val ref = CapabilityRef(key, null)
    private val hash = "a".repeat(64)

    private fun evidence(id: String, kind: EvidenceKind) =
        SourceEvidence(id, "manual", "reference/median.txt", id, 1, 2, hash, kind)

    private val source =
        DocumentationSource(
            "manual",
            SourceKind.GIT,
            "https://example.invalid/docs",
            "b".repeat(40),
            hash,
            "source",
            "7.0",
            SourceRole.CURRENT,
            SourceChannel.RELEASED,
            ParserPin("fixture", "1", hash),
            "1",
        )

    private fun catalog(): CapabilityCatalog =
        CapabilityCatalog(
            product = key.product,
            sources = listOf(source),
            evidence =
                listOf(
                    evidence("definition", EvidenceKind.DEFINITION),
                    evidence("introduced", EvidenceKind.VERSION),
                    evidence("empty", EvidenceKind.DISPOSITION),
                ),
            capabilities =
                listOf(
                    Capability(
                        key,
                        listOf(CapabilityObservation("manual", listOf("definition"))),
                        emptyList(),
                        listOf(
                            VersionEvent(
                                LifecycleEvent.INTRODUCED,
                                null,
                                "7.0",
                                VersionRange.parse("7.0"),
                                EvidenceConfidence.EXPLICIT,
                                listOf("introduced"),
                            )
                        ),
                        emptyList(),
                        ExtractionState.NOT_ASSESSED,
                        emptyList(),
                    )
                ),
            discoveryProblems = emptyList(),
        )

    private fun plan(): CoveragePlan =
        CoveragePlan(
            product = key.product,
            contexts =
                OperatorContext.entries.map { context ->
                    ContextExpectation(
                        context,
                        if (context == key.context) 1 else 0,
                        if (context == key.context) listOf(ref) else emptyList(),
                        if (context == key.context) emptyList() else listOf("empty"),
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

    private fun api(root: Path, extra: String = "", server: String = "7.0"): ApiInventory {
        root
            .resolve("Factories.java")
            .writeText(
                """
            package fixture;
            /** @since 2.0 */
            public class Factories {
                /**
                 * @aggregation.expression ${'$'}median
                 * @mongodb.server.release $server
                 * @since 3.0
                 */
                public static Object median(Object value) { return value; }
                $extra
            }
        """
                    .trimIndent()
            )
        return MorphiaApiInventory.capture(root, "fixture-revision", listOf("."))
    }

    @Test
    fun reportsApiPresenceWithoutInventingTestCoverage(@TempDir root: Path) {
        val result = CoverageComparison.compare(catalog(), plan(), api(root))
        assertEquals(ReportState.COMPARED, result.state, result.problems.toString())
        val row = result.capabilities.single()
        assertEquals(ApiState.TYPED_PRESENT, row.api)
        assertEquals(CheckState.NOT_ASSESSED, row.serialization)
        assertEquals(CheckState.NOT_ASSESSED, row.behavior)
        assertEquals(AnnotationState.MINIMUM_MATCH, row.annotations.single().state)
        assertEquals("3.0", row.annotations.single().morphiaSince.single().value)
        assertEquals("7.0", row.annotations.single().serverRequirements.single().value)
        assertFalse(row.newlyObserved)
    }

    @Test
    fun inspectsEveryMappedOverloadAndPreservesClassFallback(@TempDir root: Path) {
        val inventory =
            api(
                root,
                """
                /**
                 * @aggregation.expression ${'$'}median
                 * @mongodb.server.release 6.0
                 */
                public static Object median(Object value, Object other) { return value; }
                """
                    .trimIndent(),
            )
        val row = CoverageComparison.compare(catalog(), plan(), inventory).capabilities.single()
        assertEquals(2, row.annotations.size)
        assertEquals(
            setOf(AnnotationState.MINIMUM_MATCH, AnnotationState.INCORRECT_MINIMUM),
            row.annotations.map { it.state }.toSet(),
        )
        assertEquals(
            setOf("2.0", "3.0"),
            row.annotations.flatMap { it.morphiaSince }.map { it.value }.toSet(),
        )
    }

    @Test
    fun broadTagsCannotInferContextAndRawBsonDoesNotCountAsTyped(@TempDir root: Path) {
        val inventory = api(root)
        val ambiguous =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities +
                            catalog()
                                .capabilities
                                .single()
                                .copy(
                                    key = key.copy(context = OperatorContext.AGGREGATION_EXPRESSION)
                                )
                )
        val ambiguousPlan =
            plan()
                .copy(
                    bindings = emptyList(),
                    contexts =
                        plan().contexts.map {
                            if (it.context == OperatorContext.AGGREGATION_EXPRESSION)
                                it.copy(
                                    minimumCapabilities = 1,
                                    expected =
                                        listOf(CapabilityRef(key.copy(context = it.context), null)),
                                    intentionalEmptyEvidence = emptyList(),
                                )
                            else it
                        },
                )
        assertTrue(
            CoverageComparison.compare(ambiguous, ambiguousPlan, inventory).capabilities.all {
                it.api == ApiState.UNKNOWN_MAPPING
            }
        )
        assertEquals(
            ApiState.TYPED_PRESENT,
            CoverageComparison.compare(catalog(), plan().copy(bindings = emptyList()), inventory)
                .capabilities
                .single()
                .api,
        )
        val passthrough =
            plan().copy(bindings = plan().bindings.map { it.copy(kind = BindingKind.RAW_BSON) })
        val row =
            CoverageComparison.compare(catalog(), passthrough, inventory).capabilities.single()
        assertEquals(ApiState.RAW_BSON_ONLY, row.api)
        assertEquals(CheckState.NOT_ASSESSED, row.serialization)
    }

    @Test
    fun newCapabilitiesRemainVisibleWhenExamplesAreUnresolved(@TempDir root: Path) {
        val fresh = key.copy(token = "${'$'}newOperator")
        val candidate =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                key = fresh,
                                examples = ExtractionState.UNRESOLVED,
                                exampleProblems = listOf("Missing external snippet"),
                            )
                        }
                )
        val expected =
            plan()
                .copy(
                    bindings = emptyList(),
                    contexts =
                        plan().contexts.map {
                            if (it.context == key.context) it.copy(expected = emptyList()) else it
                        },
                )
        val report = CoverageComparison.compare(candidate, expected, api(root))
        assertEquals(ReportState.COMPARED, report.state, report.problems.toString())
        val row = report.capabilities.single()
        assertTrue(row.newlyObserved)
        assertEquals(ApiState.MISSING_TYPED_API, row.api)
        assertEquals(ExtractionState.UNRESOLVED, row.examples)
        assertEquals(listOf("Missing external snippet"), row.exampleProblems)
    }

    @Test
    fun identityCompletenessDetectsReplacementEvenWhenCountsMatch(@TempDir root: Path) {
        val replaced =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(key = key.copy(token = "${'$'}replacement"))
                        }
                )
        val report = CoverageComparison.compare(replaced, plan(), api(root))
        assertEquals(ReportState.PARTIAL, report.state)
        assertTrue(report.problems.any { it.code == "MISSING_EXPECTED_IDENTITY" })
        assertTrue(report.capabilities.all { it.api == ApiState.NOT_ASSESSED })
    }

    @Test
    fun incompleteContextsDoNotHideHealthyDomains(@TempDir root: Path) {
        val candidate =
            catalog()
                .copy(
                    discoveryProblems =
                        listOf(
                            AuditProblem(
                                "UNRECOGNIZED_DIRECTIVE",
                                "Unknown directive",
                                OperatorContext.WINDOW_FUNCTION,
                                "window.txt",
                            )
                        )
                )
        val report = CoverageComparison.compare(candidate, plan(), api(root))
        assertEquals(ReportState.PARTIAL, report.state)
        assertFalse(
            report.contexts.single { it.context == OperatorContext.WINDOW_FUNCTION }.healthy
        )
        assertEquals(ApiState.TYPED_PRESENT, report.capabilities.single().api)
    }

    @Test
    fun emptyApiAndOmittedScopeCannotProduceSuccessfulComparisons(@TempDir root: Path) {
        val inventory = api(root)
        val empty =
            CoverageComparison.compare(
                catalog(),
                plan(),
                inventory.copy(declarations = emptyList()),
            )
        assertEquals(ReportState.INVALID, empty.state)
        assertTrue(empty.problems.any { it.code == "INCOMPLETE_API_INVENTORY" })
        assertTrue(empty.capabilities.isEmpty())
        val missingContext =
            CoverageComparison.compare(
                catalog(),
                plan().copy(contexts = plan().contexts.drop(1)),
                inventory,
            )
        assertEquals(ReportState.INVALID, missingContext.state)
        assertTrue(missingContext.problems.any { it.code == "INCOMPLETE_CONTEXT_SCOPE" })
    }

    @Test
    fun emptyContextsRequireEvidenceAndCurrentInventoryCannotBeEmpty(@TempDir root: Path) {
        val expected =
            plan()
                .copy(
                    contexts =
                        plan().contexts.map {
                            if (it.context == OperatorContext.QUERY_PROJECTION)
                                it.copy(intentionalEmptyEvidence = emptyList())
                            else it
                        }
                )
        assertTrue(
            CoverageComparison.compare(catalog(), expected, api(root)).problems.any {
                it.code == "UNJUSTIFIED_EMPTY_CONTEXT"
            }
        )
        val noCurrent =
            catalog()
                .copy(
                    sources =
                        listOf(
                            source.copy(role = SourceRole.HISTORICAL),
                            source.copy(id = "current", role = SourceRole.CURRENT),
                        )
                )
        val result = CoverageComparison.compare(noCurrent, plan(), api(root))
        assertEquals(ReportState.INVALID, result.state)
        assertTrue(result.problems.any { it.code == "EMPTY_OPERATOR_INVENTORY" })
    }

    @Test
    fun exclusionsAndReviewedOmissionsAreExplicitDispositions(@TempDir root: Path) {
        val inventory = api(root)
        val excluded =
            plan().copy(exclusions = listOf(PolicyExclusion(ref, "Unsupported deployment policy")))
        val policy =
            CoverageComparison.compare(catalog(), excluded, inventory).capabilities.single()
        assertEquals(ApiState.EXCLUDED, policy.api)
        assertTrue(policy.notes.single().contains("Unsupported deployment policy"))
        val candidate =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(key = key.copy(token = "${'$'}other"))
                        }
                )
        val omitted =
            plan()
                .copy(
                    omissions =
                        listOf(
                            ExpectedOmission(
                                ref,
                                "Reviewed page disappearance; removal is unconfirmed",
                                listOf("empty"),
                            )
                        )
                )
        val report = CoverageComparison.compare(candidate, omitted, inventory)
        assertEquals(ReportState.COMPARED, report.state, report.problems.toString())
        val old = report.capabilities.single { it.capability == ref }
        assertEquals(DocumentationState.REVIEWED_OMISSION, old.documentation)
        assertEquals(ApiState.NOT_ASSESSED, old.api)
    }

    @Test
    fun staleBindingsAreVisibleWithoutInventingSupport(@TempDir root: Path) {
        val inventory = api(root)
        val stale =
            plan()
                .bindings
                .single()
                .copy(id = "old", selector = ApiSelector("fixture.Factories", "removed"))
        val partial =
            CoverageComparison.compare(
                    catalog(),
                    plan().copy(bindings = plan().bindings + stale),
                    inventory,
                )
                .capabilities
                .single()
        assertEquals(ApiState.PARTIAL, partial.api)
        assertTrue(partial.notes.single().contains("Unresolved API binding: old"))
    }

    @Test
    fun featureIntroductionsNeverReplaceTheDefinitionIntroduction(@TempDir root: Path) {
        val feature = CapabilityRef(key, "new-form")
        val candidate =
            catalog()
                .copy(
                    evidence =
                        catalog().evidence +
                            evidence("feature", EvidenceKind.FEATURE) +
                            evidence("feature-version", EvidenceKind.VERSION),
                    capabilities =
                        catalog().capabilities.map { cap ->
                            cap.copy(
                                facets =
                                    listOf(
                                        CapabilityFacet("new-form", "New form", listOf("feature"))
                                    ),
                                versions =
                                    cap.versions +
                                        VersionEvent(
                                            LifecycleEvent.INTRODUCED,
                                            "new-form",
                                            "8.3",
                                            VersionRange.parse("8.3"),
                                            EvidenceConfidence.EXPLICIT,
                                            listOf("feature-version"),
                                        ),
                            )
                        },
                )
        val expected =
            plan()
                .copy(
                    contexts =
                        plan().contexts.map {
                            if (it.context == key.context) it.copy(expected = it.expected + feature)
                            else it
                        },
                    bindings =
                        plan().bindings +
                            ApiBinding(
                                "modern",
                                feature,
                                ApiSelector("fixture.Factories", "modern"),
                                BindingKind.TYPED,
                            ),
                )
        val inventory =
            api(
                root,
                """
                /**
                 * @aggregation.expression ${'$'}median
                 * @mongodb.server.release 8.3
                 */
                public static Object modern(Object value) { return value; }
                """
                    .trimIndent(),
            )
        val rows = CoverageComparison.compare(candidate, expected, inventory).capabilities
        assertEquals(7, rows.single { it.capability.facet == null }.introduction.version!!.major)
        assertEquals(8, rows.single { it.capability.facet != null }.introduction.version!!.major)
        assertTrue(rows.all { it.annotations.single().state == AnnotationState.MINIMUM_MATCH })
        fun facet(plan: CoveragePlan, extra: String) =
            CoverageComparison.compare(
                    candidate,
                    plan,
                    api(root.resolve("x").also { it.toFile().mkdirs() }, extra),
                )
                .capabilities
                .single { it.capability.facet != null }
                .annotations
                .map { it.state }
        // A method only the facet uses carries the facet's version: without one it's a finding.
        assertEquals(
            listOf(AnnotationState.MISSING),
            facet(
                expected,
                "/** @aggregation.expression ${'$'}median */\n" +
                    "public static Object modern(Object value) { return value; }",
            ),
        )
        // The base's factory, serving the facet too, keeps the base's 7.0.
        val shared =
            expected.copy(
                bindings =
                    plan().bindings +
                        ApiBinding(
                            "shared",
                            feature,
                            ApiSelector("fixture.Factories", "median"),
                            BindingKind.TYPED,
                        )
            )
        assertEquals(listOf(AnnotationState.LATER_VARIANT), facet(shared, ""))
    }

    @Test
    fun firstObservationAndFcvAreNotReleaseIntroductions(@TempDir root: Path) {
        val candidate =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                versions = emptyList(),
                                constraints =
                                    listOf(
                                        CapabilityConstraint(
                                            ConstraintKind.FCV,
                                            ">=7.0",
                                            listOf("introduced"),
                                        )
                                    ),
                            )
                        }
                )
        val row = CoverageComparison.compare(candidate, plan(), api(root)).capabilities.single()
        assertEquals(IntroductionState.UNKNOWN, row.introduction.state)
        assertEquals(AnnotationState.UNKNOWN_EXPECTATION, row.annotations.single().state)
        assertEquals(1, row.constraints.size)
    }

    @Test
    fun conflictingAndUnparsedVersionEvidenceStaysUncertain(@TempDir root: Path) {
        val changed =
            catalog()
                .copy(
                    evidence = catalog().evidence + evidence("conflict", EvidenceKind.VERSION),
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                versions =
                                    it.versions +
                                        VersionEvent(
                                            LifecycleEvent.INTRODUCED,
                                            null,
                                            "6.0",
                                            VersionRange.parse("6.0"),
                                            EvidenceConfidence.EXPLICIT,
                                            listOf("conflict"),
                                        )
                            )
                        },
                )
        val inventory = api(root)
        val conflict = CoverageComparison.compare(changed, plan(), inventory).capabilities.single()
        assertEquals(IntroductionState.CONFLICTING, conflict.introduction.state)
        assertNull(conflict.introduction.version)
        assertEquals(AnnotationState.UNKNOWN_EXPECTATION, conflict.annotations.single().state)
        val unknown =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                versions =
                                    it.versions.map { event ->
                                        event.copy(rawVersion = "future", version = null)
                                    }
                            )
                        }
                )
        assertEquals(
            IntroductionState.UNKNOWN,
            CoverageComparison.compare(unknown, plan(), inventory)
                .capabilities
                .single()
                .introduction
                .state,
        )
    }

    @Test
    fun rejectsMutablePinsDanglingEvidenceAndDuplicateBindings(@TempDir root: Path) {
        val inventory = api(root)
        val mutable = catalog().copy(sources = listOf(source.copy(revision = "main")))
        assertTrue(
            CoverageComparison.compare(mutable, plan(), inventory).problems.any {
                it.code == "INVALID_SOURCE_REVISION"
            }
        )
        val dangling =
            catalog().copy(evidence = catalog().evidence.filterNot { it.id == "definition" })
        assertTrue(
            CoverageComparison.compare(dangling, plan(), inventory).problems.any {
                it.code == "DANGLING_EVIDENCE"
            }
        )
        assertTrue(
            CoverageComparison.compare(
                    catalog(),
                    plan().copy(bindings = plan().bindings + plan().bindings),
                    inventory,
                )
                .problems
                .any { it.code == "DUPLICATE_ID" }
        )
    }

    @Test
    fun catalogAndPlanOrderingCannotChangeTheReport(@TempDir root: Path) {
        val candidate = catalog()
        val expected = plan()
        val inventory = api(root)
        val first = AuditJson.write(CoverageComparison.compare(candidate, expected, inventory))
        val second =
            AuditJson.write(
                CoverageComparison.compare(
                    candidate.copy(
                        sources = candidate.sources.reversed(),
                        evidence = candidate.evidence.reversed(),
                        capabilities = candidate.capabilities.reversed(),
                    ),
                    expected.copy(
                        contexts = expected.contexts.reversed(),
                        bindings = expected.bindings.reversed(),
                    ),
                    inventory,
                )
            )
        assertEquals(first, second)
    }

    @Test
    fun strictJsonRoundTripsAllArtifactsAndRejectsAmbiguousInputs(@TempDir root: Path) {
        val candidate = catalog()
        val expected = plan()
        val inventory = api(root)
        assertEquals(candidate, AuditJson.catalog(AuditJson.write(candidate)))
        assertEquals(expected, AuditJson.plan(AuditJson.write(expected)))
        assertEquals(inventory, AuditJson.api(inventory.toJson()))
        val report = CoverageComparison.compare(candidate, expected, inventory)
        assertEquals(report, AuditJson.report(AuditJson.write(report)))
        val json = AuditJson.write(candidate)
        assertThrows(Exception::class.java) {
            AuditJson.catalog(
                json.replace(
                    "\"schemaVersion\" : 1",
                    "\"schemaVersion\" : 1, \"schemaVersion\" : 1",
                )
            )
        }
        assertThrows(Exception::class.java) {
            AuditJson.catalog(json.replace("\"schemaVersion\" : 1", "\"schemaVersion\" : 99"))
        }
        assertThrows(Exception::class.java) {
            AuditJson.catalog(json.replaceFirst("{", "{\"unexpected\":true,"))
        }
        assertThrows(Exception::class.java) { AuditJson.catalog(json + " {}") }
        assertThrows(Exception::class.java) {
            AuditJson.catalog(json.replace("\"GROUP_ACCUMULATOR\"", "0"))
        }
        assertThrows(Exception::class.java) {
            AuditJson.catalog(json.replace("\"definition\"", "null"))
        }
    }

    @Test
    fun releaseParsingSupportsRangesAndQualifiedVersionsWithoutGuessing() {
        assertTrue(ReleaseVersion.parse("7.0.0-rc10")!! > ReleaseVersion.parse("7.0.0-rc2")!!)
        assertTrue(ReleaseVersion.parse("7.0")!! > ReleaseVersion.parse("7.0-rc99")!!)
        assertTrue(
            VersionRange.parse(">= 5.0 < 8.0")!!.equivalent(VersionRange.parse("[5.0,8.0)")!!)
        )
        assertEquals(ReleaseVersion.parse("7.0"), VersionRange.parse("7.0.0")!!.exact())
        assertNull(VersionRange.parse("future"))
        assertNull(VersionRange.parse("5.0 and later"))
        assertNull(VersionRange.parse("[8.0,5.0)"))
        assertNull(VersionRange.parse(">7.0 <=7.0"))
        assertNull(ReleaseVersion.parse("9999999999999999.0"))
    }

    @Test
    fun missingCompatibleContextsPreventSpeculativeAutomaticMappings(@TempDir root: Path) {
        val candidate =
            catalog()
                .copy(
                    discoveryProblems =
                        listOf(
                            AuditProblem(
                                "FAILED_WINDOW_INDEX",
                                "Window definitions could not be read",
                                OperatorContext.WINDOW_FUNCTION,
                                null,
                            )
                        )
                )
        val expected = plan().copy(bindings = emptyList())
        val row = CoverageComparison.compare(candidate, expected, api(root)).capabilities.single()
        assertEquals(ApiState.UNKNOWN_MAPPING, row.api)
        assertFalse(row.notes.any { it.contains("inferred") })
    }

    @Test
    fun ambiguousAnnotationsAndLaterAutomaticVariantsNeedReview(@TempDir root: Path) {
        val inventory = api(root, server = "8.3")
        val automatic =
            CoverageComparison.compare(catalog(), plan().copy(bindings = emptyList()), inventory)
                .capabilities
                .single()
        assertEquals(AnnotationState.AMBIGUOUS, automatic.annotations.single().state)
        val explicit =
            CoverageComparison.compare(catalog(), plan(), inventory).capabilities.single()
        assertEquals(AnnotationState.INCORRECT_MINIMUM, explicit.annotations.single().state)
        val malformed =
            inventory.copy(
                declarations =
                    inventory.declarations.map {
                        if (it.name == "median")
                            it.copy(tags = it.tags + ApiTag("mongodb.server.release", "7.0"))
                        else it
                    }
            )
        assertEquals(
            AnnotationState.AMBIGUOUS,
            CoverageComparison.compare(catalog(), plan(), malformed)
                .capabilities
                .single()
                .annotations
                .single()
                .state,
        )
    }

    @Test
    fun strictJsonRejectsMissingFlagsNullElementsAndScalarCoercion(@TempDir root: Path) {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        val missingFlag = mapper.readTree(AuditJson.write(catalog()))
        (missingFlag.at("/capabilities/0/versions/0/version")
                as com.fasterxml.jackson.databind.node.ObjectNode)
            .remove("minimumInclusive")
        assertThrows(Exception::class.java) { AuditJson.catalog(missingFlag.toString()) }
        val nullElement = mapper.readTree(AuditJson.write(catalog()))
        (nullElement.at("/capabilities/0/observations/0/evidence")
                as com.fasterxml.jackson.databind.node.ArrayNode)
            .removeAll()
            .addNull()
        assertThrows(Exception::class.java) { AuditJson.catalog(nullElement.toString()) }
        val scalar =
            mapper.readTree(AuditJson.write(catalog()))
                as com.fasterxml.jackson.databind.node.ObjectNode
        scalar.put("product", 42)
        assertThrows(Exception::class.java) { AuditJson.catalog(scalar.toString()) }
        val absentParser =
            mapper.readTree(api(root).toJson()) as com.fasterxml.jackson.databind.node.ObjectNode
        absentParser.remove("parser")
        assertThrows(Exception::class.java) { AuditJson.api(absentParser.toString()) }
    }

    @Test
    fun preservesAllLifecycleEventsAndRejectsContradictoryNormalization(@TempDir root: Path) {
        val changed =
            VersionEvent(
                LifecycleEvent.CHANGED,
                null,
                "8.3",
                VersionRange.parse("8.3"),
                EvidenceConfidence.EXPLICIT,
                listOf("introduced"),
            )
        val candidate =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(versions = listOf(changed) + it.versions)
                        }
                )
        val inventory = api(root)
        val row = CoverageComparison.compare(candidate, plan(), inventory).capabilities.single()
        assertEquals(
            setOf(LifecycleEvent.INTRODUCED, LifecycleEvent.CHANGED),
            row.lifecycle.map { it.kind }.toSet(),
        )
        assertEquals(7, row.introduction.version!!.major)
        val contradictory =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                versions =
                                    it.versions.map { event ->
                                        event.copy(version = VersionRange.parse("8.0"))
                                    }
                            )
                        }
                )
        assertEquals(
            ReportState.INVALID,
            CoverageComparison.compare(contradictory, plan(), inventory).state,
        )
    }

    @Test
    fun cliEmitsStructuredReportsAndFailsInvalidInputsWithoutChangingFiles(@TempDir root: Path) {
        val candidate = catalog()
        val expected = plan()
        val inventory = api(root)
        val catalogFile =
            root.resolve("catalog.json").also { it.writeText(AuditJson.write(candidate)) }
        val planFile = root.resolve("plan.json").also { it.writeText(AuditJson.write(expected)) }
        val apiFile = root.resolve("api.json").also { it.writeText(inventory.toJson()) }
        val before =
            listOf(catalogFile, planFile, apiFile).associateWith {
                java.nio.file.Files.readAllBytes(it).toList()
            }
        val javaName =
            if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val executable = Path.of(System.getProperty("java.home"), "bin", javaName)
        fun run(): Pair<Int, CoverageReport> {
            val output = root.resolve("report.json")
            val errors = root.resolve("errors.txt")
            val process =
                ProcessBuilder(
                        executable.toString(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        "dev.morphia.audits.coverage.CoverageMainKt",
                        catalogFile.toString(),
                        planFile.toString(),
                        apiFile.toString(),
                    )
                    .redirectOutput(output.toFile())
                    .redirectError(errors.toFile())
                    .start()
            try {
                assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS))
                return process.exitValue() to
                    AuditJson.report(java.nio.file.Files.readString(output))
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        }
        val healthy = run()
        assertEquals(0, healthy.first)
        assertEquals(ReportState.COMPARED, healthy.second.state)
        before.forEach { (path, bytes) ->
            assertEquals(bytes, java.nio.file.Files.readAllBytes(path).toList())
        }
        catalogFile.writeText(
            AuditJson.write(
                candidate.copy(
                    discoveryProblems =
                        listOf(
                            AuditProblem(
                                "FAILED_WINDOW_INDEX",
                                "Window input is missing",
                                OperatorContext.WINDOW_FUNCTION,
                                null,
                            )
                        )
                )
            )
        )
        val partial = run()
        assertEquals(3, partial.first)
        assertEquals(ReportState.PARTIAL, partial.second.state)
        catalogFile.writeText("{\"schemaVersion\":99}")
        val invalid = run()
        assertEquals(2, invalid.first)
        assertEquals(ReportState.INVALID, invalid.second.state)
        assertEquals("INVALID_INPUT", invalid.second.problems.single().code)
        assertTrue(CoverageMarkdown.render(healthy.second).contains("NOT_ASSESSED"))
    }

    @Test
    fun conflictingTypedAndRawBindingsCannotConcealCoverageGaps(@TempDir root: Path) {
        val binding = plan().bindings.single().copy(id = "raw", kind = BindingKind.RAW_BSON)
        val result =
            CoverageComparison.compare(
                catalog(),
                plan().copy(bindings = plan().bindings + binding),
                api(root),
            )
        assertEquals(ReportState.INVALID, result.state)
        assertTrue(result.problems.any { it.code == "DUPLICATE_ID" })
    }

    @Test
    fun historicalDefinitionsCannotMasqueradeAsCurrentObservations(@TempDir root: Path) {
        val old = source.copy(id = "old", role = SourceRole.HISTORICAL)
        val candidate =
            catalog()
                .copy(
                    sources = catalog().sources + old,
                    evidence =
                        catalog().evidence.map {
                            if (it.id == "definition") it.copy(source = "old") else it
                        },
                )
        val result = CoverageComparison.compare(candidate, plan(), api(root))
        assertEquals(ReportState.INVALID, result.state)
        assertTrue(result.problems.any { it.code == "MISSING_DEFINITION_EVIDENCE" })
    }

    @Test
    fun rootedDefinitionsMayRetainImportedHistoricalEvidence(@TempDir root: Path) {
        val old = source.copy(id = "old", role = SourceRole.HISTORICAL)
        val candidate =
            catalog()
                .copy(
                    sources = catalog().sources + old,
                    evidence =
                        catalog().evidence +
                            evidence("imported", EvidenceKind.DEFINITION).copy(source = "old"),
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                observations =
                                    listOf(
                                        CapabilityObservation(
                                            "manual",
                                            listOf("definition", "imported"),
                                        )
                                    )
                            )
                        },
                )
        assertEquals(
            ReportState.COMPARED,
            CoverageComparison.compare(candidate, plan(), api(root)).state,
        )
    }

    @Test
    fun automaticOverloadGroupsMustAgreeOnContext(@TempDir root: Path) {
        val inventory =
            api(
                root,
                """
                /** @aggregation.stage ${'$'}median */
                public static Object median(Object value, Object other) { return value; }
                """
                    .trimIndent(),
            )
        val stageKey = key.copy(context = OperatorContext.PIPELINE_STAGE)
        val candidate =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities +
                            catalog().capabilities.single().copy(key = stageKey)
                )
        val expected =
            plan()
                .copy(
                    bindings = emptyList(),
                    contexts =
                        plan().contexts.map {
                            if (it.context == stageKey.context)
                                it.copy(
                                    minimumCapabilities = 1,
                                    expected = listOf(CapabilityRef(stageKey, null)),
                                    intentionalEmptyEvidence = emptyList(),
                                )
                            else it
                        },
                )
        val result = CoverageComparison.compare(candidate, expected, inventory)
        assertEquals(ReportState.COMPARED, result.state)
        assertTrue(result.capabilities.all { it.api == ApiState.UNKNOWN_MAPPING })
    }

    @Test
    fun brokenBindingsArePlanErrorsRatherThanMissingApis(@TempDir root: Path) {
        val missing = key.copy(token = "${'$'}newOperator")
        val candidate =
            catalog().copy(capabilities = catalog().capabilities.map { it.copy(key = missing) })
        val expected =
            plan()
                .copy(
                    contexts =
                        plan().contexts.map {
                            if (it.context == key.context)
                                it.copy(expected = listOf(CapabilityRef(missing, null)))
                            else it
                        },
                    bindings =
                        listOf(
                            ApiBinding(
                                "typo",
                                CapabilityRef(missing, null),
                                ApiSelector("fixture.Factories", "eqq"),
                                BindingKind.TYPED,
                            )
                        ),
                )
        val result = CoverageComparison.compare(candidate, expected, api(root))
        assertEquals(ReportState.PARTIAL, result.state)
        assertEquals(ApiState.INVALID_BINDING, result.capabilities.single().api)
        assertTrue(result.problems.any { it.code == "UNRESOLVED_API_BINDING" })
        assertFalse(result.capabilities.any { it.api == ApiState.MISSING_TYPED_API })
    }

    @Test
    fun explicitBindingsCannotCombineDifferentOperatorClaims(@TempDir root: Path) {
        val inventory =
            api(
                root,
                """
                /**
                 * @aggregation.expression ${'$'}avg
                 * @mongodb.server.release 2.0
                 */
                public static Object median(Object value, Object other) { return value; }
                """
                    .trimIndent(),
            )
        val result = CoverageComparison.compare(catalog(), plan(), inventory)
        assertEquals(ReportState.PARTIAL, result.state)
        assertEquals(ApiState.INVALID_BINDING, result.capabilities.single().api)
        assertTrue(result.capabilities.single().annotations.isEmpty())
        assertTrue(result.problems.any { it.code == "AMBIGUOUS_API_BINDING" })
    }

    /**
     * [catalog] and [plan] with a second capability, [other] introduced in [version], expected in
     * its context and, when [bound], bound to the same factory as the first.
     */
    private fun withSecond(
        other: CapabilityKey,
        version: String,
        bound: Boolean,
    ): Pair<CapabilityCatalog, CoveragePlan> {
        val base = catalog()
        val second =
            base.capabilities
                .single()
                .copy(
                    key = other,
                    versions =
                        listOf(
                            VersionEvent(
                                LifecycleEvent.INTRODUCED,
                                null,
                                version,
                                VersionRange.parse(version),
                                EvidenceConfidence.EXPLICIT,
                                listOf("introduced"),
                            )
                        ),
                )
        val otherRef = CapabilityRef(other, null)
        val plan = plan()
        return base.copy(capabilities = base.capabilities + second) to
            plan.copy(
                contexts =
                    plan.contexts.map {
                        if (it.context == other.context)
                            it.copy(
                                minimumCapabilities = 1,
                                expected = listOf(otherRef),
                                intentionalEmptyEvidence = emptyList(),
                            )
                        else it
                    },
                bindings =
                    plan.bindings +
                        listOfNotNull(
                            ApiBinding(
                                    "second",
                                    otherRef,
                                    ApiSelector("fixture.Factories", "median"),
                                    BindingKind.TYPED,
                                )
                                .takeIf { bound }
                        ),
            )
    }

    @Test
    fun aReviewedGapIsMissingWithItsReason(@TempDir root: Path) {
        val reason = "Factories has no median option. The server added it in 7.0."
        val gapped = plan().copy(bindings = emptyList(), missing = listOf(ReviewedGap(ref, reason)))
        val row = CoverageComparison.compare(catalog(), gapped, api(root)).capabilities.single()
        assertEquals(ApiState.MISSING_TYPED_API, row.api)
        assertTrue(row.notes.contains(REVIEWED_GAP + reason), row.notes.toString())
        // A reviewed gap can't also be bound.
        val bound = plan().copy(missing = listOf(ReviewedGap(ref, reason)))
        val invalid = CoverageComparison.compare(catalog(), bound, api(root))
        assertEquals(ReportState.INVALID, invalid.state)
        assertTrue(
            invalid.problems.any { it.code == "INVALID_REVIEWED_GAP" },
            invalid.problems.toString(),
        )
    }

    @Test
    fun aSelectorMayAlsoClaimAnOperatorOfAnotherContext(@TempDir root: Path) {
        // As a push that takes a list writes $each, a modifier the manual documents elsewhere.
        val each = CapabilityKey(key.product, OperatorContext.WINDOW_FUNCTION, "${'$'}each")
        val (catalog, plan) = withSecond(each, "7.0", bound = false)
        val inventory =
            api(
                root,
                """
                /**
                 * @aggregation.expression ${'$'}median
                 * @aggregation.expression ${'$'}each
                 * @mongodb.server.release 7.0
                 */
                public static Object median(Object value, Object other) { return value; }
                """
                    .trimIndent(),
            )
        val result = CoverageComparison.compare(catalog, plan, inventory)
        assertEquals(
            ApiState.TYPED_PRESENT,
            result.capabilities.single { it.capability == ref }.api,
            result.problems.toString(),
        )
        assertTrue(result.problems.none { it.code == "AMBIGUOUS_API_BINDING" })
    }

    @Test
    fun aLaterContextOfOneFactoryIsAVariantNotAWrongAnnotation(@TempDir root: Path) {
        // The factory's 7.0 annotation is right for the accumulator; the window function came
        // later.
        val window = CapabilityKey(key.product, OperatorContext.WINDOW_FUNCTION, key.token)
        val (catalog, plan) = withSecond(window, "8.0", bound = true)
        val result = CoverageComparison.compare(catalog, plan, api(root))
        fun state(of: CapabilityKey) =
            result.capabilities.single { it.capability.key == of }.annotations.single().state
        assertEquals(AnnotationState.MINIMUM_MATCH, state(key))
        assertEquals(AnnotationState.LATER_VARIANT, state(window))
        // A minimum after the window function's own introduction is wrong whatever else it serves.
        val late = CoverageComparison.compare(catalog, plan, api(root, server = "9.0"))
        assertEquals(
            AnnotationState.INCORRECT_MINIMUM,
            late.capabilities.single { it.capability.key == window }.annotations.single().state,
        )
        // Alone, the same 7.0 annotation is wrong for an 8.0 capability.
        val alone = plan.copy(bindings = plan.bindings.filter { it.capability.key == window })
        assertEquals(
            AnnotationState.INCORRECT_MINIMUM,
            CoverageComparison.compare(catalog, alone, api(root))
                .capabilities
                .single { it.capability.key == window }
                .annotations
                .single()
                .state,
        )
    }

    @Test
    fun diagnosticsAreDeterministicEvenWhenCodesAndSubjectsCollide(@TempDir root: Path) {
        val diagnostics =
            listOf(
                AuditProblem(
                    "PARSE_FAILURE",
                    "second",
                    OperatorContext.WINDOW_FUNCTION,
                    "window.txt",
                ),
                AuditProblem(
                    "PARSE_FAILURE",
                    "first",
                    OperatorContext.WINDOW_FUNCTION,
                    "window.txt",
                ),
            )
        val inventory = api(root)
        val a =
            CoverageComparison.compare(
                catalog().copy(discoveryProblems = diagnostics),
                plan(),
                inventory,
            )
        val b =
            CoverageComparison.compare(
                catalog().copy(discoveryProblems = diagnostics.reversed()),
                plan(),
                inventory,
            )
        assertEquals(AuditJson.write(a), AuditJson.write(b))
    }

    @Test
    fun evidenceRolesAndUnparsedVersionClaimsStayExplicit(@TempDir root: Path) {
        val inventory = api(root)
        val wrong =
            plan()
                .copy(
                    contexts =
                        plan().contexts.map {
                            if (it.context != key.context)
                                it.copy(intentionalEmptyEvidence = listOf("definition"))
                            else it
                        }
                )
        assertTrue(
            CoverageComparison.compare(catalog(), wrong, inventory).problems.any {
                it.code == "INVALID_EMPTY_CONTEXT_EVIDENCE"
            }
        )
        val unknown =
            catalog()
                .copy(
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                versions =
                                    it.versions.map { version ->
                                        version.copy(
                                            rawVersion = "5.0 (backported to 4.4.x)",
                                            version = VersionRange.parse("5.0"),
                                        )
                                    }
                            )
                        }
                )
        val result = CoverageComparison.compare(unknown, plan(), inventory)
        assertEquals(ReportState.COMPARED, result.state)
        assertEquals(IntroductionState.UNKNOWN, result.capabilities.single().introduction.state)
    }

    @Test
    fun currentFacetsRequireCurrentFeatureEvidence(@TempDir root: Path) {
        val featureRef = CapabilityRef(key, "array-input")
        val candidate =
            catalog()
                .copy(
                    sources =
                        catalog().sources + source.copy(id = "old", role = SourceRole.HISTORICAL),
                    evidence =
                        catalog().evidence +
                            evidence("feature-old", EvidenceKind.FEATURE).copy(source = "old"),
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                facets =
                                    listOf(
                                        CapabilityFacet(
                                            "array-input",
                                            "Array input",
                                            listOf("feature-old", "definition"),
                                        )
                                    )
                            )
                        },
                )
        val expected =
            plan()
                .copy(
                    contexts =
                        plan().contexts.map {
                            if (it.context == key.context)
                                it.copy(expected = it.expected + featureRef)
                            else it
                        }
                )
        val result = CoverageComparison.compare(candidate, expected, api(root))
        assertEquals(ReportState.PARTIAL, result.state)
        assertTrue(
            result.problems.any {
                it.code == "MISSING_EXPECTED_IDENTITY" && it.subject == featureRef.id()
            }
        )
        assertEquals(
            DocumentationState.UNRESOLVED,
            result.capabilities.single { it.capability == featureRef }.documentation,
        )
        assertTrue(
            CoverageMarkdown.render(result)
                .contains("MISSING_EXPECTED_IDENTITY (${key.context.key()} / ${featureRef.id()})")
        )
    }

    @Test
    fun unrelatedContextTagsCannotConcealMissingApis(@TempDir root: Path) {
        val queryKey = key.copy(context = OperatorContext.QUERY_PROJECTION)
        val candidate =
            catalog().copy(capabilities = catalog().capabilities.map { it.copy(key = queryKey) })
        val expected =
            plan()
                .copy(
                    bindings = emptyList(),
                    contexts =
                        plan().contexts.map {
                            when (it.context) {
                                key.context ->
                                    it.copy(
                                        minimumCapabilities = 0,
                                        expected = emptyList(),
                                        intentionalEmptyEvidence = listOf("empty"),
                                    )
                                queryKey.context ->
                                    it.copy(
                                        minimumCapabilities = 1,
                                        expected = listOf(CapabilityRef(queryKey, null)),
                                        intentionalEmptyEvidence = emptyList(),
                                    )
                                else -> it
                            }
                        },
                )
        val result = CoverageComparison.compare(candidate, expected, api(root))
        assertEquals(ReportState.COMPARED, result.state)
        assertEquals(ApiState.MISSING_TYPED_API, result.capabilities.single().api)
    }

    @Test
    fun historicalOnlyBindingTargetsCannotDisappearSilently(@TempDir root: Path) {
        val candidate =
            catalog()
                .copy(
                    sources =
                        catalog().sources + source.copy(id = "old", role = SourceRole.HISTORICAL),
                    evidence =
                        catalog().evidence +
                            evidence("feature-old", EvidenceKind.FEATURE).copy(source = "old"),
                    capabilities =
                        catalog().capabilities.map {
                            it.copy(
                                facets =
                                    listOf(
                                        CapabilityFacet(
                                            "old-input",
                                            "Historical input",
                                            listOf("feature-old"),
                                        )
                                    )
                            )
                        },
                )
        val expected =
            plan()
                .copy(
                    bindings =
                        plan().bindings +
                            ApiBinding(
                                "stale-feature",
                                CapabilityRef(key, "old-input"),
                                ApiSelector("fixture.Factories", "typo"),
                                BindingKind.TYPED,
                            )
                )
        val result = CoverageComparison.compare(candidate, expected, api(root))
        assertEquals(ReportState.INVALID, result.state)
        assertTrue(
            result.problems.any {
                it.code == "UNKNOWN_BINDING_TARGET" && it.subject == "stale-feature"
            }
        )
    }
}
