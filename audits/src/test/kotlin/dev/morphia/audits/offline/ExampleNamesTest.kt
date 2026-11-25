package dev.morphia.audits.offline

import dev.morphia.audits.coverage.*
import dev.morphia.audits.naming.*
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ExampleNamesTest {
    private val hash = "a".repeat(64)
    private val scope =
        ScenarioScope(
            CapabilityRef(
                CapabilityKey("mongodb-server", OperatorContext.GROUP_ACCUMULATOR, "${'$'}median"),
                null,
            ),
            "mongosh",
        )
    private val destination =
        ExampleDestination("fixture.TestMedian", "core/src/test/resources/median")
    private val empty = ExampleNameRegistry(entries = emptyList())

    private fun scenario(
        id: String = "median-by-year",
        title: String = "Calculate Median Ratings by Year",
        label: String = id,
    ): NamingScenario =
        NamingScenario(
            id,
            scope,
            destination,
            ScenarioTrace(
                hash,
                ScenarioLocation(
                    "reference/median.txt",
                    listOf(label),
                    emptyList(),
                    "median-by-year",
                    "Examples",
                    hash,
                ),
                title,
                listOf("example-evidence"),
            ),
            emptyList(),
        )

    private fun request(vararg scenarios: NamingScenario) =
        NamingRequest(
            catalogSha256 = hash,
            completeScopes = listOf(scope),
            scenarios = scenarios.toList(),
            occupied = emptyList(),
            reviewedNames = emptyList(),
            retirements = emptyList(),
        )

    private fun accept(scenario: NamingScenario = scenario()): ExampleNameRegistry {
        val result = NameReconciliation.propose(empty, request(scenario))
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        return result.registry
    }

    private fun assertPending(result: NameProposal, code: String) {
        assertEquals(NameProposalState.UNRESOLVED, result.state, result.problems.toString())
        assertTrue(result.problems.any { it.code == code }, result.problems.toString())
    }

    @Test
    fun synthesizesLegalDescriptiveNamesAndExactMethods() {
        val cases =
            mapOf(
                "Calculate Median Ratings by Year" to "calculateMedianRatingsByYear",
                "ISO Week" to "isoWeek",
                "ISOWeek" to "isoWeek",
                "2dSphere" to "case2dSphere",
                "switch" to "caseSwitch",
                "CON" to "caseCon",
                "record" to "caseRecord",
                "Café café" to "cafeCafe",
                "Use ${'$'}median with Arrays" to "useMedianWithArrays",
                "Find 中 Values" to "findUnicode4e2dValues",
                "Check 😀" to "checkUnicode1f600",
            )
        cases.forEach { (title, stem) ->
            assertEquals(stem, DescriptiveNames.stem(title))
            val method = DescriptiveNames.method(stem)
            // Contract for the future generator and migration adapter.
            assertEquals(stem, method.substring(4, 5).lowercase(Locale.ROOT) + method.substring(5))
        }
        listOf("main", "Examples", "Example 3", "example12", "", "123").forEach {
            assertThrows(IllegalArgumentException::class.java) { DescriptiveNames.stem(it) }
        }
        assertThrows(IllegalArgumentException::class.java) { DescriptiveNames.stem("A".repeat(81)) }
        assertThrows(IllegalArgumentException::class.java) {
            DescriptiveNames.resourcePath("a".repeat(240), "median")
        }
        assertThrows(IllegalArgumentException::class.java) { DescriptiveNames.validate("example3") }
    }

    @Test
    fun localeCannotAffectNames() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("isoWeek", DescriptiveNames.stem("ISO Week"))
            assertEquals("testIsoWeek", DescriptiveNames.method("isoWeek"))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun titleEditsAndInsertedSiblingsKeepEstablishedNames() {
        val accepted = accept()
        val edited = scenario(title = "Compute the Yearly Median")
        val sibling =
            scenario("by-month", "Calculate Median Ratings by Month").let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    autoAnchor = "by-month",
                                    fingerprint = "b".repeat(64),
                                )
                        )
                )
            }
        val result = NameReconciliation.propose(accepted, request(sibling, edited))
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        val existing = result.resolutions.single { it.scenario == edited.id }
        assertEquals("calculateMedianRatingsByYear", existing.stem)
        assertEquals(NameMatch.STABLE_ID, existing.match)
        assertEquals(
            2,
            result.registry.entries.single { it.stem == existing.stem }.observations.size,
        )
        assertEquals(
            AuditJson.write(result),
            AuditJson.write(NameReconciliation.propose(accepted, request(edited, sibling))),
        )
    }

    @Test
    fun sourceIdentityPrecedesFingerprintAndRetainsContentEdits() {
        val accepted = accept()
        val changed =
            scenario(title = "A Different Calculation").let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location = it.trace.location.copy(fingerprint = "c".repeat(64))
                        )
                )
            }
        val result = NameReconciliation.propose(accepted, request(changed))
        assertEquals(NameProposalState.READY, result.state)
        assertEquals(NameMatch.STABLE_ID, result.resolutions.single().match)
        assertEquals(accepted.entries.single().stem, result.resolutions.single().stem)
    }

    @Test
    fun movesCanMatchByFingerprintButDestinationMovesNeedReview() {
        val accepted = accept()
        val moved =
            scenario().let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    document = "tutorial/new.txt",
                                    explicitLabels = emptyList(),
                                    autoAnchor = "different",
                                    enclosingContext = "Moved",
                                )
                        )
                )
            }
        val byFingerprint = NameReconciliation.propose(accepted, request(moved))
        assertEquals(NameProposalState.READY, byFingerprint.state)
        assertEquals(NameMatch.FINGERPRINT, byFingerprint.resolutions.single().match)
        val changedTarget =
            moved.copy(destination = destination.copy(testClass = "fixture.TestMoved"))
        assertPending(
            NameReconciliation.propose(accepted, request(changedTarget)),
            "DESTINATION_CHANGED",
        )
        val reviewed =
            request(changedTarget)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                moved.id,
                                accepted.entries.single().qualifiedName(),
                                null,
                                "Move alongside the docs",
                                listOf("reviewed-move"),
                            )
                        )
                )
        assertEquals(NameProposalState.READY, NameReconciliation.propose(accepted, reviewed).state)
    }

    @Test
    fun anchorsNeedTheirEnclosingContextAndSnippetsSurviveDocumentMoves() {
        val initial =
            scenario().let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    explicitLabels = emptyList(),
                                    fingerprint = null,
                                )
                        )
                )
            }
        val accepted = accept(initial)
        val changed =
            initial.copy(trace = initial.trace.copy(displayTitle = "Renamed Cosmetic Heading"))
        assertEquals(
            NameMatch.ANCHOR_AND_CONTEXT,
            NameReconciliation.propose(accepted, request(changed)).resolutions.single().match,
        )
        val wrongContext =
            changed.copy(
                trace =
                    changed.trace.copy(
                        location =
                            changed.trace.location.copy(enclosingContext = "Different operation")
                    )
            )
        assertPending(
            NameReconciliation.propose(accepted, request(wrongContext)),
            "UNACCOUNTED_EXISTING",
        )
        val snippet =
            initial.copy(
                trace =
                    initial.trace.copy(
                        location =
                            initial.trace.location.copy(
                                snippetIds = listOf("examples/median.js:year")
                            )
                    )
            )
        val snippetAccepted = accept(snippet)
        val moved =
            snippet.copy(
                trace =
                    snippet.trace.copy(
                        location =
                            snippet.trace.location.copy(
                                document = "new/median.txt",
                                autoAnchor = null,
                                enclosingContext = null,
                            )
                    )
            )
        assertEquals(
            NameMatch.STABLE_ID,
            NameReconciliation.propose(snippetAccepted, request(moved)).resolutions.single().match,
        )
    }

    @Test
    fun genericOrMissingHeadingsRecoverDescriptiveContext() {
        listOf("main", "Example 1", "").forEach { title ->
            val candidate =
                scenario(title = title)
                    .copy(contextTitles = listOf("Examples", "Calculate Median Ratings by Year"))
            assertEquals(
                "calculateMedianRatingsByYear",
                NameReconciliation.propose(empty, request(candidate)).resolutions.single().stem,
            )
        }
        assertPending(
            NameReconciliation.propose(empty, request(scenario(title = "Example 1"))),
            "DESCRIPTIVE_NAME_REQUIRED",
        )
    }

    @Test
    fun duplicateTitlesUseMeaningfulQualifiersIndependentOfInputOrder() {
        val yearly =
            scenario("year", "Calculate Median").copy(contextTitles = listOf("Yearly Ratings"))
        val monthly =
            scenario("month", "Calculate Median").copy(contextTitles = listOf("Monthly Ratings"))
        val result = NameReconciliation.propose(empty, request(yearly, monthly))
        assertEquals(NameProposalState.READY, result.state)
        assertEquals(
            setOf("yearlyRatingsCalculateMedian", "monthlyRatingsCalculateMedian"),
            result.resolutions.map { it.stem }.toSet(),
        )
        assertEquals(
            AuditJson.write(result),
            AuditJson.write(NameReconciliation.propose(empty, request(monthly, yearly))),
        )
        val unresolved =
            NameReconciliation.propose(
                empty,
                request(
                    yearly.copy(contextTitles = emptyList()),
                    monthly.copy(contextTitles = emptyList()),
                ),
            )
        assertPending(unresolved, "NAME_COLLISION")
        assertTrue(unresolved.resolutions.isEmpty())
    }

    @Test
    fun physicalFoldersAndInheritedMethodsAreReserved() {
        val candidate = scenario()
        listOf(
                OccupiedName(
                    OccupiedKind.METHOD,
                    destination.testClass,
                    "testCalculateMedianRatingsByYear",
                    null,
                ),
                OccupiedName(
                    OccupiedKind.FOLDER,
                    destination.resourceDirectory,
                    "CALCULATEMEDIANRATINGSBYYEAR",
                    null,
                ),
                OccupiedName(
                    OccupiedKind.FOLDER,
                    destination.resourceDirectory,
                    "ｃａｌｃｕｌａｔｅＭｅｄｉａｎＲａｔｉｎｇｓＢｙＹｅａｒ",
                    null,
                ),
            )
            .forEach { occupied ->
                assertPending(
                    NameReconciliation.propose(
                        empty,
                        request(candidate).copy(occupied = listOf(occupied)),
                    ),
                    "NAME_COLLISION",
                )
            }
        val accepted = accept()
        val owned =
            OccupiedName(
                OccupiedKind.FOLDER,
                destination.resourceDirectory,
                accepted.entries.single().stem,
                accepted.entries.single().qualifiedName(),
            )
        assertEquals(
            NameProposalState.READY,
            NameReconciliation.propose(accepted, request(candidate).copy(occupied = listOf(owned)))
                .state,
        )
        assertPending(
            NameReconciliation.propose(
                accepted,
                request(candidate).copy(occupied = listOf(owned.copy(owner = null))),
            ),
            "NAME_COLLISION",
        )
        assertPending(
            NameReconciliation.propose(
                empty,
                request(scenario(title = "Query"))
                    .copy(
                        occupied =
                            listOf(
                                OccupiedName(
                                    OccupiedKind.METHOD,
                                    destination.testClass,
                                    "testQuery",
                                    null,
                                )
                            )
                    ),
            ),
            "NAME_COLLISION",
        )
    }

    @Test
    fun differentContextsSharingDestinationsStillCollide() {
        val alternate =
            scope.copy(
                capability =
                    CapabilityRef(
                        scope.capability.key.copy(context = OperatorContext.WINDOW_FUNCTION),
                        null,
                    )
            )
        val first = scenario()
        val second = scenario("window").copy(scope = alternate)
        val input = request(first, second).copy(completeScopes = listOf(scope, alternate))
        assertPending(NameReconciliation.propose(empty, input), "NAME_COLLISION")
    }

    @Test
    fun ambiguousIdentityAndCompetingObservationsRequireReview() {
        val first = scenario("first", "Calculate Median")
        val second =
            scenario("second", "Compute Median").let {
                it.copy(
                    trace = it.trace.copy(location = it.trace.location.copy(autoAnchor = "second"))
                )
            }
        val accepted = NameReconciliation.propose(empty, request(first, second)).registry
        val ambiguous =
            scenario("incoming", "Median Changed").let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    explicitLabels = emptyList(),
                                    autoAnchor = null,
                                    enclosingContext = null,
                                )
                        )
                )
            }
        assertPending(NameReconciliation.propose(accepted, request(ambiguous)), "AMBIGUOUS_MATCH")
        val one = accept()
        assertPending(
            NameReconciliation.propose(
                one,
                request(scenario(), scenario("different-id").copy(trace = scenario().trace)),
            ),
            "COMPETING_MATCH",
        )
    }

    @Test
    fun missingCasesRequireDispositionAndRetiredNamesStayReserved() {
        val accepted = accept()
        assertPending(NameReconciliation.propose(accepted, request()), "UNACCOUNTED_EXISTING")
        val retirement =
            ReviewedRetirement(
                accepted.entries.single().qualifiedName(),
                "Removed upstream",
                listOf("removal-evidence"),
            )
        val result =
            NameReconciliation.propose(accepted, request().copy(retirements = listOf(retirement)))
        assertEquals(NameProposalState.READY, result.state)
        assertFalse(result.registry.entries.single().active)
        assertEquals("Removed upstream", result.registry.entries.single().decisions.single().reason)
        assertEquals(hash, result.registry.entries.single().decisions.single().catalogSha256)
        val newcomer =
            scenario("new", scenario().trace.displayTitle).let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    autoAnchor = "new",
                                    fingerprint = "d".repeat(64),
                                )
                        )
                )
            }
        assertPending(
            NameReconciliation.propose(result.registry, request(newcomer)),
            "NAME_COLLISION",
        )
    }

    @Test
    fun reviewedRenamesKeepAliasesAndCanBeReconciledAgain() {
        val accepted = accept()
        val oldName = accepted.entries.single().qualifiedName()
        val incoming = scenario(title = "Calculate Median Scores")
        val reviewed =
            request(incoming)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                incoming.id,
                                oldName,
                                "calculateMedianScores",
                                "Clarify the scenario",
                                listOf("reviewed-name"),
                            )
                        )
                )
        val result = NameReconciliation.propose(accepted, reviewed)
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        assertEquals(listOf(oldName), result.registry.entries.single().aliases)
        assertEquals(
            "Clarify the scenario",
            result.registry.entries.single().decisions.single().reason,
        )
        assertEquals(
            listOf("reviewed-name"),
            result.registry.entries.single().decisions.single().evidence,
        )
        val again = NameReconciliation.propose(result.registry, request(incoming))
        assertEquals(NameProposalState.READY, again.state)
        assertEquals("calculateMedianScores", again.resolutions.single().stem)
        val back =
            request(incoming)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                incoming.id,
                                oldName,
                                accepted.entries.single().stem,
                                "Restore earlier name",
                                listOf("reviewed-restore"),
                            )
                        )
                )
        val restored = NameReconciliation.propose(result.registry, back)
        assertEquals(NameProposalState.READY, restored.state)
        assertEquals(
            NameProposalState.READY,
            NameReconciliation.propose(restored.registry, request(incoming)).state,
        )
    }

    @Test
    fun explicitAbbreviationsAndSplitDispositionsAreReviewable() {
        val long = scenario(title = "Very " + "Long ".repeat(30) + "Calculation")
        assertPending(NameReconciliation.propose(empty, request(long)), "DESCRIPTIVE_NAME_REQUIRED")
        val reviewed =
            request(long)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                long.id,
                                null,
                                "calculateYearlyMedian",
                                "Bound the descriptive name",
                                listOf("reviewed-abbreviation"),
                            )
                        )
                )
        assertEquals(NameProposalState.READY, NameReconciliation.propose(empty, reviewed).state)
        val accepted = accept()
        val new = scenario("new", "Compute Median for Movies")
        val split =
            request(new)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                new.id,
                                null,
                                "computeMovieMedian",
                                "Split old scenario",
                                listOf("split-evidence"),
                            )
                        ),
                    retirements =
                        listOf(
                            ReviewedRetirement(
                                accepted.entries.single().qualifiedName(),
                                "Superseded by split",
                                listOf("split-evidence"),
                            )
                        ),
                )
        assertEquals(NameProposalState.READY, NameReconciliation.propose(accepted, split).state)
    }

    @Test
    fun strictTransportAndInputValidationRejectUnreviewableClaims() {
        val input = request(scenario())
        val result = NameReconciliation.propose(empty, input)
        assertEquals(input, AuditJson.namingRequest(AuditJson.write(input)))
        assertEquals(empty, AuditJson.names(AuditJson.write(empty)))
        assertEquals(result, AuditJson.nameProposal(AuditJson.write(result)))
        assertThrows(Exception::class.java) {
            AuditJson.namingRequest(
                AuditJson.write(input).replace("\"occupied\" : [ ]", "\"occupied\" : null")
            )
        }
        assertThrows(Exception::class.java) { AuditJson.names("{\"entries\":[]}") }
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(empty, input.copy(completeScopes = emptyList())).state,
        )
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    empty,
                    request(
                        scenario()
                            .copy(destination = destination.copy(resourceDirectory = "../escape"))
                    ),
                )
                .state,
        )
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    empty,
                    input.copy(
                        reviewedNames =
                            listOf(
                                ReviewedName(
                                    "unknown",
                                    null,
                                    "median",
                                    "reason",
                                    listOf("evidence"),
                                )
                            )
                    ),
                )
                .state,
        )
        val accepted = accept()
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    accepted.copy(entries = accepted.entries + accepted.entries),
                    input,
                )
                .state,
        )
    }

    @Test
    fun cliEmitsTypedUtf8ReportsAndNeverWritesInputs(@TempDir root: Path) {
        val registry = root.resolve("registry.json")
        val incoming = root.resolve("request.json")
        val baseline = AuditJson.write(empty)
        registry.writeText(baseline)
        val input = AuditJson.write(request(scenario(title = "Café Median")))
        incoming.writeText(input)
        fun run(): Pair<Int, NameProposal> {
            val process =
                ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-Dfile.encoding=US-ASCII",
                        "-cp",
                        System.getProperty("java.class.path"),
                        "dev.morphia.audits.naming.NamesMainKt",
                        registry.toString(),
                        incoming.toString(),
                    )
                    .start()
            val bytes = process.inputStream.readAllBytes()
            val errors = process.errorStream.readAllBytes().decodeToString()
            assertEquals("", errors)
            return process.waitFor() to
                AuditJson.nameProposal(bytes.decodeToString(throwOnInvalidSequence = true))
        }
        val healthy = run()
        assertEquals(0, healthy.first)
        assertEquals("cafeMedian", healthy.second.resolutions.single().stem)
        assertTrue(AuditJson.write(healthy.second).contains("Café"))
        assertEquals(baseline, registry.readText())
        assertEquals(input, incoming.readText())
        incoming.writeText(AuditJson.write(request(scenario(title = "Example 1"))))
        assertEquals(3, run().first)
        incoming.writeText("{broken")
        val invalid = run()
        assertEquals(2, invalid.first)
        assertEquals(NameProposalState.INVALID, invalid.second.state)
        assertEquals(baseline, registry.readText())
    }

    @Test
    fun incompleteScopesNeverRetireUnrelatedAcceptedCases() {
        val accepted = accept()
        val otherScope =
            scope.copy(
                capability =
                    CapabilityRef(
                        scope.capability.key.copy(context = OperatorContext.WINDOW_FUNCTION),
                        null,
                    )
            )
        val incoming = scenario("window", "Calculate Rolling Median").copy(scope = otherScope)
        val result =
            NameReconciliation.propose(
                accepted,
                request(incoming).copy(completeScopes = listOf(otherScope)),
            )
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        assertEquals(2, result.registry.entries.size)
        assertTrue(result.registry.entries.all { it.active })
        assertTrue(result.registry.entries.contains(accepted.entries.single()))
    }

    @Test
    fun retirementPinsTheCurrentCatalogAndKeepsReviewEvidence() {
        val accepted = accept()
        val current = "b".repeat(64)
        val retirement =
            ReviewedRetirement(
                accepted.entries.single().qualifiedName(),
                "Removed in revised docs",
                listOf("current-removal-span"),
            )
        val result =
            NameReconciliation.propose(
                accepted,
                request().copy(catalogSha256 = current, retirements = listOf(retirement)),
            )
        assertEquals(NameProposalState.READY, result.state)
        val decision = result.registry.entries.single().decisions.single()
        assertEquals(NameDecisionAction.RETIREMENT, decision.action)
        assertEquals(current, decision.catalogSha256)
        assertEquals(listOf("current-removal-span"), decision.evidence)
        assertEquals(
            accepted.entries.single().observations,
            result.registry.entries.single().observations,
        )
    }

    @Test
    fun mismatchedCatalogsForgedAliasesAndUnreviewedOverridesFail() {
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    empty,
                    request(scenario()).copy(catalogSha256 = "b".repeat(64)),
                )
                .state,
        )
        val accepted = accept()
        val badAlias =
            accepted.entries
                .single()
                .copy(aliases = listOf(scope.capability.id() + "/example/nested/median"))
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    accepted.copy(entries = listOf(badAlias)),
                    request(scenario()),
                )
                .state,
        )
        val unreviewed = ReviewedName(scenario().id, null, "differentMedian", "", listOf("proof"))
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    empty,
                    request(scenario()).copy(reviewedNames = listOf(unreviewed)),
                )
                .state,
        )
        assertEquals(
            NameProposalState.INVALID,
            NameReconciliation.propose(
                    empty,
                    request(scenario())
                        .copy(
                            reviewedNames =
                                listOf(unreviewed.copy(reason = "Reviewed", newStem = "switch"))
                        ),
                )
                .state,
        )
    }

    @Test
    fun insertedDuplicateNeverDislodgesAnEstablishedName() {
        val accepted = accept()
        val newcomer =
            scenario("new").let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    autoAnchor = "new",
                                    fingerprint = "b".repeat(64),
                                )
                        )
                )
            }
        val result = NameReconciliation.propose(accepted, request(scenario(), newcomer))
        assertPending(result, "NAME_COLLISION")
        assertEquals(listOf(scenario().id), result.resolutions.map { it.scenario })
        assertEquals(
            accepted.entries.single().qualifiedName(),
            result.resolutions.single().qualifiedName,
        )
        assertFalse(result.problems.any { it.code == "UNACCOUNTED_EXISTING" })
        assertTrue(result.problems.all { it.scenario == newcomer.id })
    }

    @Test
    fun everyNewDuplicateNeedsItsOwnMeaningfulDisambiguation() {
        val yearly =
            scenario("year", "Calculate Median").copy(contextTitles = listOf("Yearly Ratings"))
        val unqualified = scenario("unknown", "Calculate Median")
        val result = NameReconciliation.propose(empty, request(yearly, unqualified))
        assertPending(result, "NAME_COLLISION")
        assertEquals(listOf("yearlyRatingsCalculateMedian"), result.resolutions.map { it.stem })
        assertTrue(result.problems.any { it.scenario == unqualified.id })
        assertEquals(
            AuditJson.write(result),
            AuditJson.write(NameReconciliation.propose(empty, request(unqualified, yearly))),
        )
    }

    @Test
    fun contradictoryAnchorsAndFingerprintsRequireReviewedIdentity() {
        fun unlabeled(id: String, title: String, fingerprint: String) =
            scenario(id, title).let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    explicitLabels = emptyList(),
                                    autoAnchor = id,
                                    fingerprint = fingerprint,
                                )
                        )
                )
            }
        val alpha = unlabeled("alpha", "Calculate Alpha Median", hash)
        val beta = unlabeled("beta", "Calculate Beta Median", "b".repeat(64))
        val accepted = NameReconciliation.propose(empty, request(alpha, beta)).registry
        val incoming =
            beta.copy(
                id = "incoming",
                trace =
                    beta.trace.copy(
                        displayTitle = alpha.trace.displayTitle,
                        location = beta.trace.location.copy(autoAnchor = "alpha"),
                    ),
            )
        val alphaName =
            accepted.entries.single { it.stem == "calculateAlphaMedian" }.qualifiedName()
        val betaName = accepted.entries.single { it.stem == "calculateBetaMedian" }.qualifiedName()
        val input =
            request(incoming)
                .copy(
                    retirements =
                        listOf(
                            ReviewedRetirement(
                                alphaName,
                                "Alpha removed",
                                listOf("removed-evidence"),
                            )
                        )
                )
        assertPending(NameReconciliation.propose(accepted, input), "CONFLICTING_IDENTITY_EVIDENCE")
        val reviewed =
            input.copy(
                reviewedNames =
                    listOf(
                        ReviewedName(
                            incoming.id,
                            betaName,
                            null,
                            "Beta retitled after Alpha removal",
                            listOf("mapping-evidence"),
                        )
                    )
            )
        val result = NameReconciliation.propose(accepted, reviewed)
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        assertEquals(betaName, result.resolutions.single().qualifiedName)
    }

    @Test
    fun historicalWeakAnchorsAndFingerprintsCannotReclaimAnIdentity() {
        val original = scenario()
        val accepted = accept(original)
        val updated =
            original.copy(
                trace =
                    original.trace.copy(
                        location =
                            original.trace.location.copy(
                                autoAnchor = "changed",
                                fingerprint = "b".repeat(64),
                            )
                    )
            )
        val revised = NameReconciliation.propose(accepted, request(updated)).registry
        val newcomer =
            scenario("new", "Calculate New Movie Median").let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location = original.trace.location.copy(explicitLabels = emptyList())
                        )
                )
            }
        val result = NameReconciliation.propose(revised, request(updated, newcomer))
        assertEquals(NameProposalState.READY, result.state, result.problems.toString())
        assertEquals(NameMatch.NEW, result.resolutions.single { it.scenario == newcomer.id }.match)
        assertEquals(
            updated.trace,
            result.registry.entries
                .single { it.stem == original.let { DescriptiveNames.stem(it.trace.displayTitle) } }
                .current,
        )
    }

    @Test
    fun numericAutoAnchorSuffixesNeedFingerprintCorroboration() {
        val candidate =
            scenario().let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(
                                    explicitLabels = emptyList(),
                                    autoAnchor = "example-1",
                                    fingerprint = null,
                                )
                        )
                )
            }
        val accepted = accept(candidate)
        assertPending(
            NameReconciliation.propose(accepted, request(candidate)),
            "UNSTABLE_AUTO_ANCHOR",
        )
        val corroborated =
            candidate.copy(
                trace =
                    candidate.trace.copy(
                        location = candidate.trace.location.copy(fingerprint = hash)
                    )
            )
        val known = accept(corroborated)
        assertEquals(
            NameProposalState.READY,
            NameReconciliation.propose(known, request(corroborated)).state,
        )
    }

    @Test
    fun reviewedMovesKeepOldPhysicalBindingsReserved() {
        val accepted = accept()
        val moved =
            scenario()
                .copy(
                    destination =
                        ExampleDestination("fixture.TestMoved", "core/src/test/resources/moved")
                )
        val reviewed =
            request(moved)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                moved.id,
                                accepted.entries.single().qualifiedName(),
                                null,
                                "Move scenario",
                                listOf("reviewed-move"),
                            )
                        )
                )
        val revised = NameReconciliation.propose(accepted, reviewed).registry
        assertEquals(
            listOf(HistoricalBinding(destination, accepted.entries.single().stem)),
            revised.entries.single().previousBindings,
        )
        val alternate =
            scope.copy(
                capability =
                    CapabilityRef(
                        scope.capability.key.copy(context = OperatorContext.WINDOW_FUNCTION),
                        null,
                    )
            )
        val incoming = scenario("window").copy(scope = alternate)
        val input = request(incoming).copy(completeScopes = listOf(alternate))
        assertPending(NameReconciliation.propose(revised, input), "NAME_COLLISION")
    }

    @Test
    fun namespaceComparisonsHandleCaseAndProvenAliasOwners() {
        val accepted = accept()
        val alternate =
            scope.copy(
                capability =
                    CapabilityRef(
                        scope.capability.key.copy(context = OperatorContext.WINDOW_FUNCTION),
                        null,
                    )
            )
        val incoming = scenario("window").copy(scope = alternate)
        listOf(
                destination.copy(
                    resourceDirectory = destination.resourceDirectory.uppercase(Locale.ROOT),
                    testClass = "other.TestWindow",
                ),
                destination.copy(
                    resourceDirectory = "other/window",
                    testClass = destination.testClass.uppercase(Locale.ROOT),
                ),
            )
            .forEach { dest ->
                assertPending(
                    NameReconciliation.propose(
                        accepted,
                        request(incoming.copy(destination = dest))
                            .copy(completeScopes = listOf(alternate)),
                    ),
                    "NAME_COLLISION",
                )
            }
        val originalName = accepted.entries.single().qualifiedName()
        val reviewed =
            request(scenario())
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                scenario().id,
                                originalName,
                                "computeYearlyMedian",
                                "Clarify name",
                                listOf("rename-proof"),
                            )
                        )
                )
        val revised = NameReconciliation.propose(accepted, reviewed).registry
        val owned =
            OccupiedName(
                OccupiedKind.FOLDER,
                destination.resourceDirectory,
                revised.entries.single().stem,
                originalName,
            )
        assertEquals(
            NameProposalState.READY,
            NameReconciliation.propose(revised, request(scenario()).copy(occupied = listOf(owned)))
                .state,
        )
    }

    @Test
    fun pathBudgetIncludesFixtureLeavesBeforeChoosingNames() {
        val candidate =
            scenario().copy(destination = destination.copy(resourceDirectory = "a".repeat(210)))
        val result = NameReconciliation.propose(empty, request(candidate))
        assertPending(result, "PATH_BUDGET")
        assertEquals(1, result.problems.size)
        assertEquals(
            destination.resourceDirectory + "/median/expected.json",
            DescriptiveNames.fixturePath(destination.resourceDirectory, "median", "expected.json"),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DescriptiveNames.fixturePath("a".repeat(210), "median", "expected.json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DescriptiveNames.fixturePath("fixtures", "median", "x".repeat(33))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DescriptiveNames.fixturePath("fixtures", "median", "../escape")
        }
    }

    @Test
    fun aliasesReserveIdentitiesWithoutInventingPhysicalBindingsAfterMoves() {
        val accepted = accept()
        val oldName = accepted.entries.single().qualifiedName()
        val rename =
            request(scenario())
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                scenario().id,
                                oldName,
                                "computeYearlyMedian",
                                "Clarify name",
                                listOf("rename-proof"),
                            )
                        )
                )
        val renamed = NameReconciliation.propose(accepted, rename).registry
        val alternate =
            scope.copy(
                capability =
                    CapabilityRef(
                        scope.capability.key.copy(context = OperatorContext.WINDOW_FUNCTION),
                        null,
                    )
            )
        val newDestination =
            ExampleDestination("fixture.TestMoved", "core/src/test/resources/moved")
        val neighbor = scenario("window").copy(scope = alternate, destination = newDestination)
        val withNeighbor =
            NameReconciliation.propose(
                renamed,
                request(neighbor).copy(completeScopes = listOf(alternate)),
            )
        assertEquals(NameProposalState.READY, withNeighbor.state, withNeighbor.problems.toString())
        val moved = scenario().copy(destination = newDestination)
        val move =
            request(moved)
                .copy(
                    reviewedNames =
                        listOf(
                            ReviewedName(
                                moved.id,
                                renamed.entries.single().qualifiedName(),
                                null,
                                "Move alongside neighbor",
                                listOf("move-proof"),
                            )
                        )
                )
        val revised = NameReconciliation.propose(withNeighbor.registry, move)
        assertEquals(NameProposalState.READY, revised.state, revised.problems.toString())
        val again = NameReconciliation.propose(revised.registry, request(moved))
        assertEquals(NameProposalState.READY, again.state, again.problems.toString())
        assertEquals(2, again.registry.entries.size)
    }

    @Test
    fun ambiguousContraryEvidenceCannotBecomeWeakerThanUniqueContraryEvidence() {
        fun identified(id: String, title: String, fingerprint: String) =
            scenario(id, title).let {
                it.copy(
                    trace =
                        it.trace.copy(
                            location =
                                it.trace.location.copy(autoAnchor = id, fingerprint = fingerprint)
                        )
                )
            }
        val alpha = identified("alpha", "Calculate Alpha", hash)
        val beta = identified("beta", "Calculate Beta", "b".repeat(64))
        val gamma = identified("gamma", "Calculate Gamma", "b".repeat(64))
        val accepted = NameReconciliation.propose(empty, request(alpha, beta, gamma)).registry
        val conflicting =
            alpha.copy(
                trace =
                    alpha.trace.copy(
                        location = alpha.trace.location.copy(fingerprint = "b".repeat(64))
                    )
            )
        val result = NameReconciliation.propose(accepted, request(conflicting, beta, gamma))
        assertPending(result, "CONFLICTING_IDENTITY_EVIDENCE")
        assertFalse(result.resolutions.any { it.scenario == alpha.id })
        assertEquals(setOf(beta.id, gamma.id), result.resolutions.map { it.scenario }.toSet())
    }

    @Test
    fun separatorlessGeneratedAnchorNumbersNeedCorroboration() {
        listOf("id3", "example2", "version2024").forEach { anchor ->
            val candidate =
                scenario().let {
                    it.copy(
                        trace =
                            it.trace.copy(
                                location =
                                    it.trace.location.copy(
                                        explicitLabels = emptyList(),
                                        autoAnchor = anchor,
                                        fingerprint = null,
                                    )
                            )
                    )
                }
            assertPending(
                NameReconciliation.propose(accept(candidate), request(candidate)),
                "UNSTABLE_AUTO_ANCHOR",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DescriptiveNames.fixturePath("fixtures", "median", "expected.")
        }
    }
}
