package dev.morphia.audits.naming

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.ContractValidation
import java.text.Normalizer
import java.util.Locale
import javax.lang.model.SourceVersion

/**
 * Pure proposal: no filesystem writes, ordinal matching, nearest-neighbor guesses or publication.
 */
object NameReconciliation {
    private val digest = Regex("[0-9a-f]{64}")
    private val language = Regex("[a-z][a-z0-9-]*")
    private val problemOrder =
        compareBy<NamingProblem>({ it.scenario }, { it.qualifiedName }, { it.code }, { it.message })

    private data class Choice(
        val scenario: NamingScenario,
        val previous: ExampleNameEntry?,
        val match: NameMatch,
        val options: List<String>,
        val reviewed: ReviewedName?,
        var index: Int = 0,
    ) {
        fun stem(): String = options[index]

        fun priority(): Int =
            when {
                previous != null &&
                    previous.stem == stem() &&
                    previous.destination == scenario.destination -> 3
                reviewed != null -> 2
                else -> 1
            }

        fun entry(): ExampleNameEntry =
            ExampleNameEntry(
                scenario.scope,
                scenario.destination,
                stem(),
                (previous?.aliases.orEmpty() +
                        listOfNotNull(
                            previous?.qualifiedName()?.takeIf { previous.stem != stem() }
                        ))
                    .distinct()
                    .filter { it != scenario.scope.capability.id() + "/example/" + stem() }
                    .sorted(),
                (previous?.observations.orEmpty() + scenario.trace).distinct().sortedBy {
                    AuditJson.write(it)
                },
                scenario.trace,
                (previous?.previousBindings.orEmpty() +
                        listOfNotNull(previous?.let { HistoricalBinding(it.destination, it.stem) }))
                    .distinct()
                    .filter { it != HistoricalBinding(scenario.destination, stem()) }
                    .sortedBy { AuditJson.write(it) },
                (previous?.decisions.orEmpty() +
                        listOfNotNull(
                            reviewed?.let {
                                NameDecision(
                                    NameDecisionAction.ASSIGNMENT,
                                    scenario.trace.catalogSha256,
                                    previous?.qualifiedName(),
                                    stem(),
                                    it.reason,
                                    it.evidence,
                                )
                            }
                        ))
                    .distinct()
                    .sortedBy { AuditJson.write(it) },
                true,
            )
    }

    fun propose(accepted: ExampleNameRegistry, request: NamingRequest): NameProposal {
        val problems = validate(accepted, request).toMutableList()
        fun result(
            state: NameProposalState,
            registry: ExampleNameRegistry,
            resolutions: List<NameResolution>,
        ) =
            NameProposal(
                state = state,
                acceptedSha256 = AuditJson.hash(canonical(accepted)),
                requestSha256 = AuditJson.hash(canonical(request)),
                registry = canonical(registry),
                resolutions = resolutions.sortedBy { it.scenario },
                problems = problems.distinct().sortedWith(problemOrder),
            )
        if (problems.isNotEmpty()) return result(NameProposalState.INVALID, accepted, emptyList())
        val names =
            accepted.entries
                .flatMap { entry -> (entry.aliases + entry.qualifiedName()).map { it to entry } }
                .toMap()
        val assignments = request.reviewedNames.associateBy { it.scenario }
        val retired = request.retirements.map { it.qualifiedName }.toSet()
        val choices = mutableListOf<Choice>()
        request.scenarios
            .sortedBy { it.id }
            .forEach { candidate ->
                val reviewed = assignments[candidate.id]
                var previous: ExampleNameEntry? = null
                var basis = if (reviewed == null) NameMatch.NEW else NameMatch.REVIEWED
                if (reviewed?.existingName != null) previous = names.getValue(reviewed.existingName)
                else if (reviewed == null) {
                    val eligible =
                        accepted.entries.filter { it.active && it.scope == candidate.scope }
                    val loc = candidate.trace.location
                    val tiers =
                        listOf(
                            NameMatch.STABLE_ID to
                                { old: ScenarioLocation ->
                                    (old.document == loc.document &&
                                        old.explicitLabels.any { it in loc.explicitLabels }) ||
                                        old.snippetIds.any { it in loc.snippetIds }
                                },
                            NameMatch.ANCHOR_AND_CONTEXT to
                                { old: ScenarioLocation ->
                                    loc.autoAnchor != null &&
                                        loc.enclosingContext != null &&
                                        old.document == loc.document &&
                                        old.autoAnchor == loc.autoAnchor &&
                                        old.enclosingContext == loc.enclosingContext
                                },
                            NameMatch.FINGERPRINT to
                                { old: ScenarioLocation ->
                                    loc.fingerprint != null && old.fingerprint == loc.fingerprint
                                },
                        )
                    val matchesByTier =
                        tiers.map { (kind, predicate) ->
                            kind to eligible.filter { predicate(it.current.location) }
                        }
                    val first = matchesByTier.firstOrNull { it.second.isNotEmpty() }
                    if (first != null) {
                        if (first.second.size != 1) {
                            problems +=
                                NamingProblem(
                                    "AMBIGUOUS_MATCH",
                                    candidate.id,
                                    null,
                                    "${first.first} matches ${first.second.map { it.qualifiedName() }.sorted()}; a reviewed mapping is required",
                                )
                            return@forEach
                        }
                        val selected = first.second.single()
                        val contradicting =
                            matchesByTier.filter {
                                it.second.isNotEmpty() && selected !in it.second
                            }
                        if (contradicting.isNotEmpty()) {
                            problems +=
                                NamingProblem(
                                    "CONFLICTING_IDENTITY_EVIDENCE",
                                    candidate.id,
                                    selected.qualifiedName(),
                                    "Source identity, anchor/context and fingerprint point to different accepted scenarios; review the mapping",
                                )
                            return@forEach
                        }
                        val fingerprintMatches =
                            matchesByTier.single { it.first == NameMatch.FINGERPRINT }.second
                        if (
                            first.first == NameMatch.ANCHOR_AND_CONTEXT &&
                                Regex(".*[0-9]+$").matches(loc.autoAnchor.orEmpty()) &&
                                fingerprintMatches != listOf(selected)
                        ) {
                            problems +=
                                NamingProblem(
                                    "UNSTABLE_AUTO_ANCHOR",
                                    candidate.id,
                                    selected.qualifiedName(),
                                    "A numeric auto-anchor suffix requires corroborating fingerprint evidence or a reviewed mapping",
                                )
                            return@forEach
                        }
                        previous = selected
                        basis = first.first
                    }
                }
                if (previous != null && previous.qualifiedName() in retired) {
                    problems +=
                        NamingProblem(
                            "MATCHED_RETIREMENT",
                            candidate.id,
                            previous.qualifiedName(),
                            "Retired identities cannot simultaneously be consumed; review a split/merge explicitly",
                        )
                    return@forEach
                }
                if (
                    previous != null &&
                        previous.destination != candidate.destination &&
                        reviewed == null
                ) {
                    problems +=
                        NamingProblem(
                            "DESTINATION_CHANGED",
                            candidate.id,
                            previous.qualifiedName(),
                            "Moving a test/resource binding requires a reviewed mapping",
                        )
                    return@forEach
                }
                val fixed = reviewed?.newStem ?: previous?.stem
                val options =
                    if (fixed != null) listOf(fixed)
                    else
                        runCatching { options(candidate) }
                            .getOrElse {
                                problems +=
                                    NamingProblem(
                                        "DESCRIPTIVE_NAME_REQUIRED",
                                        candidate.id,
                                        null,
                                        it.message ?: "A reviewed name is required",
                                    )
                                return@forEach
                            }
                val bounded =
                    options.filter {
                        runCatching {
                                DescriptiveNames.resourcePath(
                                    candidate.destination.resourceDirectory,
                                    it,
                                )
                            }
                            .isSuccess
                    }
                if (bounded.isEmpty()) {
                    problems +=
                        NamingProblem(
                            "PATH_BUDGET",
                            candidate.id,
                            previous?.qualifiedName(),
                            "No proposed name fits the resource path and fixture-leaf budget",
                        )
                    return@forEach
                }
                choices += Choice(candidate, previous, basis, bounded, reviewed)
            }
        val duplicateClaims =
            choices
                .filter { it.previous != null }
                .groupBy { it.previous!!.qualifiedName() }
                .filterValues { it.size > 1 }
        duplicateClaims.forEach { (name, competing) ->
            competing.forEach {
                problems +=
                    NamingProblem(
                        "COMPETING_MATCH",
                        it.scenario.id,
                        name,
                        "More than one incoming scenario claims the accepted identity; group equivalent tabs or review a split",
                    )
            }
        }
        choices.removeAll { it.previous?.qualifiedName() in duplicateClaims }

        fun conflicts(choice: Choice): Boolean {
            val proposed = choice.entry()
            return accepted.entries.any { old ->
                old.qualifiedName() != choice.previous?.qualifiedName() &&
                    (proposed.qualifiedName() in (old.aliases + old.qualifiedName()) ||
                        reservations(old).any { collides(it, proposed) })
            } ||
                request.occupied.any { occupied ->
                    val owned =
                        choice.previous?.let { old ->
                            names[occupied.owner]?.qualifiedName() == old.qualifiedName() &&
                                when (occupied.kind) {
                                    OccupiedKind.FOLDER ->
                                        fold(occupied.namespace) ==
                                            fold(old.destination.resourceDirectory) &&
                                            occupied.name == old.stem
                                    OccupiedKind.METHOD ->
                                        fold(occupied.namespace) ==
                                            fold(old.destination.testClass) &&
                                            occupied.name == DescriptiveNames.method(old.stem)
                                }
                        } == true
                    !owned &&
                        when (occupied.kind) {
                            OccupiedKind.FOLDER ->
                                fold(occupied.namespace) ==
                                    fold(proposed.destination.resourceDirectory) &&
                                    fold(occupied.name) == fold(proposed.stem)
                            OccupiedKind.METHOD ->
                                fold(occupied.namespace) == fold(proposed.destination.testClass) &&
                                    occupied.name == DescriptiveNames.method(proposed.stem)
                        }
                } ||
                choices.any { other ->
                    other !== choice &&
                        other.priority() >= choice.priority() &&
                        collides(other.entry(), proposed)
                }
        }
        val exhausted = mutableSetOf<String>()
        while (true) {
            val collision = choices.filter(::conflicts)
            collision
                .filter { it.priority() == 1 && it.index + 1 == it.options.size }
                .forEach { exhausted += it.scenario.id }
            val advancing = collision.filter { it.index + 1 < it.options.size }
            if (advancing.isEmpty()) break
            advancing.forEach { it.index++ }
        }
        val collision = choices.filter { it.scenario.id in exhausted || conflicts(it) }.toSet()
        collision.forEach {
            problems +=
                NamingProblem(
                    "NAME_COLLISION",
                    it.scenario.id,
                    it.entry().qualifiedName(),
                    "No unique meaningful name is available in the accepted registry or physical namespace; provide a reviewed canonical name",
                )
        }
        val valid = choices.filter { it !in collision }
        val consumed = valid.mapNotNull { it.previous?.qualifiedName() }.toSet()
        accepted.entries
            .filter {
                it.active &&
                    it.scope in request.completeScopes &&
                    it.qualifiedName() !in consumed &&
                    it.qualifiedName() !in retired
            }
            .forEach {
                problems +=
                    NamingProblem(
                        "UNACCOUNTED_EXISTING",
                        null,
                        it.qualifiedName(),
                        "An accepted scenario disappeared from a complete scope without a reviewed disposition",
                    )
            }
        val entries =
            accepted.entries
                .filter { it.qualifiedName() !in consumed }
                .map { entry ->
                    val retirement =
                        request.retirements.firstOrNull {
                            it.qualifiedName == entry.qualifiedName()
                        }
                    if (retirement == null) entry
                    else
                        entry.copy(
                            active = false,
                            decisions =
                                entry.decisions +
                                    NameDecision(
                                        NameDecisionAction.RETIREMENT,
                                        request.catalogSha256,
                                        entry.qualifiedName(),
                                        null,
                                        retirement.reason,
                                        retirement.evidence,
                                    ),
                        )
                } + valid.map { it.entry() }
        return result(
            if (problems.isEmpty()) NameProposalState.READY else NameProposalState.UNRESOLVED,
            ExampleNameRegistry(entries = entries),
            valid.map { choice ->
                val entry = choice.entry()
                NameResolution(
                    choice.scenario.id,
                    entry.qualifiedName(),
                    entry.stem,
                    DescriptiveNames.method(entry.stem),
                    DescriptiveNames.resourcePath(entry.destination.resourceDirectory, entry.stem),
                    choice.match,
                    choice.previous?.qualifiedName(),
                )
            },
        )
    }

    private fun options(candidate: NamingScenario): List<String> {
        val title =
            (listOf(candidate.trace.displayTitle) + candidate.contextTitles).firstOrNull {
                !DescriptiveNames.isGeneric(it)
            } ?: error("No descriptive source title or enclosing context")
        val primary = DescriptiveNames.stem(title)
        val qualifiers =
            candidate.contextTitles.filter {
                !DescriptiveNames.isGeneric(it) &&
                    runCatching { DescriptiveNames.stem(it) }.getOrNull() != primary
            }
        return (listOf(primary) +
                qualifiers.mapNotNull {
                    runCatching { DescriptiveNames.stem("$it $title") }.getOrNull()
                } +
                listOfNotNull(
                    if (qualifiers.size > 1)
                        runCatching {
                                DescriptiveNames.stem(qualifiers.joinToString(" ") + " " + title)
                            }
                            .getOrNull()
                    else null
                ))
            .distinct()
    }

    private fun fold(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .uppercase(Locale.ROOT)
            .lowercase(Locale.ROOT)

    private fun reservations(entry: ExampleNameEntry): List<ExampleNameEntry> =
        listOf(entry) +
            entry.previousBindings.map {
                entry.copy(
                    destination = it.destination,
                    stem = it.stem,
                    aliases = emptyList(),
                    previousBindings = emptyList(),
                )
            }

    private fun collides(left: ExampleNameEntry, right: ExampleNameEntry): Boolean =
        left.qualifiedName() == right.qualifiedName() ||
            (fold(left.destination.resourceDirectory) ==
                fold(right.destination.resourceDirectory) && fold(left.stem) == fold(right.stem)) ||
            (fold(left.destination.testClass) == fold(right.destination.testClass) &&
                DescriptiveNames.method(left.stem) == DescriptiveNames.method(right.stem))

    private fun validate(
        accepted: ExampleNameRegistry,
        request: NamingRequest,
    ): List<NamingProblem> {
        val problems = mutableListOf<NamingProblem>()
        fun check(ok: Boolean, message: String) {
            if (!ok) problems += NamingProblem("INVALID_NAMING_INPUT", null, null, message)
        }
        fun <T> unique(values: List<T>, label: String) =
            check(values.distinct().size == values.size, "Duplicate $label")
        fun scope(scope: ScenarioScope) {
            check(
                scope.capability.key.product.matches(Regex("[a-z][a-z0-9-]*")) &&
                    dev.morphia.audits.coverage.OperatorTokens.valid(scope.capability.key.token) &&
                    (scope.capability.facet == null ||
                        scope.capability.facet.matches(Regex("[a-z][a-z0-9-]*"))) &&
                    language.matches(scope.language),
                "Invalid capability/language scope",
            )
        }
        fun destination(dest: ExampleDestination) {
            check(
                SourceVersion.isName(dest.testClass) &&
                    ContractValidation.relative(dest.resourceDirectory),
                "Invalid destination",
            )
        }
        fun trace(trace: ScenarioTrace) {
            val loc = trace.location
            check(
                digest.matches(trace.catalogSha256) &&
                    trace.evidence.isNotEmpty() &&
                    trace.evidence.none(String::isBlank),
                "Trace requires a catalog digest and evidence",
            )
            check(
                ContractValidation.relative(loc.document) &&
                    loc.explicitLabels.none(String::isBlank) &&
                    loc.snippetIds.none(String::isBlank) &&
                    (loc.autoAnchor == null || loc.autoAnchor.isNotBlank()) &&
                    (loc.enclosingContext == null || loc.enclosingContext.isNotBlank()) &&
                    (loc.fingerprint == null || digest.matches(loc.fingerprint)),
                "Invalid source location/fingerprint",
            )
            check(
                loc.explicitLabels.isNotEmpty() ||
                    loc.snippetIds.isNotEmpty() ||
                    (loc.autoAnchor != null && loc.enclosingContext != null) ||
                    loc.fingerprint != null,
                "A scenario requires non-positional identity evidence",
            )
            unique(loc.explicitLabels, "source labels")
            unique(loc.snippetIds, "snippet identities")
            unique(trace.evidence, "trace evidence")
        }
        check(
            accepted.schemaVersion == 1 && request.schemaVersion == 1,
            "Unsupported naming schema",
        )
        unique(accepted.entries.map { it.qualifiedName() }, "canonical names")
        unique(
            accepted.entries.flatMap { it.aliases + it.qualifiedName() },
            "canonical names/aliases",
        )
        accepted.entries.forEach { entry ->
            scope(entry.scope)
            destination(entry.destination)
            check(
                runCatching {
                        DescriptiveNames.validate(entry.stem)
                        DescriptiveNames.resourcePath(
                            entry.destination.resourceDirectory,
                            entry.stem,
                        )
                    }
                    .isSuccess,
                "Invalid accepted stem/path",
            )
            check(entry.observations.isNotEmpty(), "Accepted names require source observations")
            entry.observations.forEach(::trace)
            trace(entry.current)
            check(
                entry.observations.map(::canonical).contains(canonical(entry.current)),
                "Current trace must be retained in observation history",
            )
            entry.previousBindings.forEach { binding ->
                destination(binding.destination)
                check(
                    runCatching {
                            DescriptiveNames.resourcePath(
                                binding.destination.resourceDirectory,
                                binding.stem,
                            )
                        }
                        .isSuccess,
                    "Invalid historical binding",
                )
            }
            entry.decisions.forEach { decision ->
                check(
                    digest.matches(decision.catalogSha256) &&
                        decision.reason.isNotBlank() &&
                        decision.evidence.isNotEmpty() &&
                        decision.evidence.none(String::isBlank),
                    "Naming decisions require a catalog digest and rationale/evidence",
                )
                check(
                    if (decision.action == NameDecisionAction.ASSIGNMENT)
                        decision.newStem != null &&
                            runCatching { DescriptiveNames.validate(decision.newStem) }.isSuccess
                    else decision.previousName != null && decision.newStem == null,
                    "Invalid naming decision action/target",
                )
                if (decision.previousName != null)
                    check(
                        decision.previousName ==
                            entry.scope.capability.id() +
                                "/example/" +
                                decision.previousName.substringAfterLast('/') &&
                            runCatching {
                                    DescriptiveNames.validate(
                                        decision.previousName.substringAfterLast('/')
                                    )
                                }
                                .isSuccess,
                        "Invalid previous decision identity",
                    )
            }
            entry.aliases.forEach { alias ->
                check(
                    alias ==
                        entry.scope.capability.id() + "/example/" + alias.substringAfterLast('/') &&
                        runCatching { DescriptiveNames.validate(alias.substringAfterLast('/')) }
                            .isSuccess,
                    "Alias must retain the capability and a descriptive name",
                )
            }
        }
        // Only call naming helpers after validating their arguments.
        if (problems.isNotEmpty()) return problems
        val reservations =
            accepted.entries.flatMap { entry ->
                reservations(entry).map { entry.qualifiedName() to it }
            }
        check(
            reservations.indices.none { i ->
                (i + 1 until reservations.size).any { j ->
                    reservations[i].first != reservations[j].first &&
                        collides(reservations[i].second, reservations[j].second)
                }
            },
            "Accepted names/aliases collide in a physical namespace",
        )
        check(
            digest.matches(request.catalogSha256),
            "Request requires the pinned current catalog digest",
        )
        check(request.completeScopes.isNotEmpty(), "Complete scopes must be explicit")
        unique(request.completeScopes, "complete scopes")
        request.completeScopes.forEach(::scope)
        unique(request.scenarios.map { it.id }, "scenario identifiers")
        request.scenarios.forEach { scenario ->
            check(
                scenario.id.isNotBlank() && scenario.scope in request.completeScopes,
                "Scenario must belong to a declared complete scope",
            )
            check(
                scenario.trace.catalogSha256 == request.catalogSha256,
                "Incoming scenarios must refer to the request's current catalog",
            )
            scope(scenario.scope)
            destination(scenario.destination)
            trace(scenario.trace)
            check(scenario.contextTitles.none(String::isBlank), "Context titles cannot be blank")
        }
        val names =
            accepted.entries
                .flatMap { entry -> (entry.aliases + entry.qualifiedName()).map { it to entry } }
                .toMap()
        val scenarios = request.scenarios.associateBy { it.id }
        unique(request.reviewedNames.map { it.scenario }, "reviewed assignments")
        request.reviewedNames.forEach { assignment ->
            val scenario = scenarios[assignment.scenario]
            val entry = names[assignment.existingName]
            check(
                scenario != null &&
                    (assignment.existingName != null || assignment.newStem != null) &&
                    assignment.reason.isNotBlank() &&
                    assignment.evidence.isNotEmpty() &&
                    assignment.evidence.none(String::isBlank),
                "Reviewed assignment needs a known scenario, target/name and rationale/evidence",
            )
            check(
                assignment.existingName == null ||
                    (entry?.active == true && entry.scope == scenario?.scope),
                "Reviewed target must be active and in the same capability/language scope",
            )
            if (assignment.newStem != null)
                check(
                    runCatching { DescriptiveNames.validate(assignment.newStem) }.isSuccess,
                    "Invalid reviewed stem",
                )
        }
        unique(request.retirements.map { it.qualifiedName }, "retirements")
        request.retirements.forEach {
            val entry =
                accepted.entries.firstOrNull { entry -> entry.qualifiedName() == it.qualifiedName }
            check(
                entry?.active == true &&
                    entry.scope in request.completeScopes &&
                    it.reason.isNotBlank() &&
                    it.evidence.isNotEmpty() &&
                    it.evidence.none(String::isBlank),
                "Retirement requires an active canonical name in scope and rationale/evidence",
            )
        }
        request.occupied.forEach {
            check(
                it.name.isNotBlank() &&
                    '/' !in it.name &&
                    '\\' !in it.name &&
                    (it.owner == null || it.owner in names),
                "Invalid occupied name/owner",
            )
            check(
                if (it.kind == OccupiedKind.FOLDER) ContractValidation.relative(it.namespace)
                else SourceVersion.isName(it.namespace),
                "Invalid occupied namespace",
            )
        }
        return problems
    }

    private fun canonical(trace: ScenarioTrace): ScenarioTrace =
        trace.copy(
            location =
                trace.location.copy(
                    explicitLabels = trace.location.explicitLabels.sorted(),
                    snippetIds = trace.location.snippetIds.sorted(),
                ),
            evidence = trace.evidence.sorted(),
        )

    private fun canonical(registry: ExampleNameRegistry): ExampleNameRegistry =
        registry.copy(
            entries =
                registry.entries
                    .map {
                        it.copy(
                            aliases = it.aliases.sorted(),
                            current = canonical(it.current),
                            previousBindings =
                                it.previousBindings.distinct().sortedBy { binding ->
                                    AuditJson.write(binding)
                                },
                            decisions =
                                it.decisions
                                    .map { decision ->
                                        decision.copy(evidence = decision.evidence.sorted())
                                    }
                                    .distinct()
                                    .sortedBy { decision -> AuditJson.write(decision) },
                            observations =
                                it.observations.map(::canonical).distinct().sortedBy { trace ->
                                    AuditJson.write(trace)
                                },
                        )
                    }
                    .sortedBy { it.qualifiedName() }
        )

    private fun canonical(request: NamingRequest): NamingRequest =
        request.copy(
            completeScopes = request.completeScopes.sortedBy { AuditJson.write(it) },
            scenarios =
                request.scenarios.map { it.copy(trace = canonical(it.trace)) }.sortedBy { it.id },
            occupied = request.occupied.sortedBy { AuditJson.write(it) },
            reviewedNames =
                request.reviewedNames
                    .map { it.copy(evidence = it.evidence.sorted()) }
                    .sortedBy { it.scenario },
            retirements =
                request.retirements
                    .map { it.copy(evidence = it.evidence.sorted()) }
                    .sortedBy { it.qualifiedName },
        )
}
