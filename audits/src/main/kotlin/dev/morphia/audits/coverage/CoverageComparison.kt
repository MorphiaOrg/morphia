package dev.morphia.audits.coverage

import dev.morphia.audits.api.ApiDeclaration
import dev.morphia.audits.api.ApiInventory
import dev.morphia.audits.api.DeclarationKind

/** Pure comparison. No source writes, test-directory inference, network, or publication. */
object CoverageComparison {
    fun compare(catalog: CapabilityCatalog, plan: CoveragePlan, api: ApiInventory): CoverageReport {
        val invalid = ContractValidation.validate(catalog, plan, api)
        if (invalid.isNotEmpty())
            return CoverageReport(
                state = ReportState.INVALID,
                catalogSha256 = null,
                planSha256 = null,
                apiSha256 = null,
                contexts = emptyList(),
                capabilities = emptyList(),
                problems = invalid,
            )
        val capabilities = catalog.capabilities.associateBy { it.key }
        val current = catalog.currentRefs()
        val active = catalog.capabilities.filter { CapabilityRef(it.key, null) in current }
        fun activeRef(ref: CapabilityRef): Boolean = ref in current
        fun omission(ref: CapabilityRef): ExpectedOmission? =
            plan.omissions.firstOrNull { it.capability == ref }
                ?: plan.omissions.firstOrNull { it.capability == CapabilityRef(ref.key, null) }
        val global = mutableListOf<AuditProblem>()
        if (active.isEmpty())
            global +=
                AuditProblem(
                    "EMPTY_OPERATOR_INVENTORY",
                    "There are no current capability observations",
                    null,
                    null,
                )
        plan.omissions
            .filter { activeRef(it.capability) }
            .forEach {
                global +=
                    AuditProblem(
                        "OMISSION_STILL_OBSERVED",
                        "An observed identity cannot also be omitted",
                        it.capability.key.context,
                        it.capability.id(),
                    )
            }
        val health =
            plan.contexts
                .sortedBy { it.context.name }
                .map { expectation ->
                    val count = active.count { it.key.context == expectation.context }
                    val problems =
                        (catalog.discoveryProblems + global)
                            .filter { it.context == null || it.context == expectation.context }
                            .toMutableList()
                    if (count < expectation.minimumCapabilities)
                        problems +=
                            AuditProblem(
                                "CAPABILITY_COLLAPSE",
                                "Observed $count capabilities; expected at least ${expectation.minimumCapabilities}",
                                expectation.context,
                                null,
                            )
                    if (
                        count == 0 &&
                            expectation.minimumCapabilities == 0 &&
                            expectation.intentionalEmptyEvidence.isEmpty()
                    )
                        problems +=
                            AuditProblem(
                                "UNJUSTIFIED_EMPTY_CONTEXT",
                                "An empty context requires explicit evidence",
                                expectation.context,
                                null,
                            )
                    expectation.expected
                        .filter { !activeRef(it) && omission(it) == null }
                        .forEach {
                            problems +=
                                AuditProblem(
                                    "MISSING_EXPECTED_IDENTITY",
                                    "An accepted identity disappeared without a disposition",
                                    expectation.context,
                                    it.id(),
                                )
                        }
                    ContextHealth(
                        expectation.context,
                        problems.isEmpty(),
                        count,
                        expectation.minimumCapabilities,
                        problems.sortedWith(problemOrder),
                    )
                }
        val byContext = health.associateBy { it.context }
        val types =
            api.declarations.filter { it.kind == DeclarationKind.TYPE }.associateBy { it.id }
        val expected = plan.contexts.flatMap { it.expected }.toSet()
        val refs =
            (active.flatMap { cap ->
                    listOf(CapabilityRef(cap.key, null)) +
                        cap.facets.map { CapabilityRef(cap.key, it.id) }.filter(::activeRef)
                } + expected)
                .distinct()
                .sortedBy { it.id() }
        val bindingProblems = mutableListOf<AuditProblem>()
        val rows =
            refs.map { ref ->
                val cap = capabilities[ref.key]
                val intro = introduction(cap, ref.facet)
                val healthy = byContext.getValue(ref.key.context).healthy
                val observed = activeRef(ref)
                val omitted = omission(ref)
                val exclusion =
                    plan.exclusions.firstOrNull { it.capability == ref }
                        ?: plan.exclusions.firstOrNull {
                            it.capability == CapabilityRef(ref.key, null)
                        }
                fun matches(binding: ApiBinding): List<ApiDeclaration> =
                    api.declarations.filter {
                        it.owner == binding.selector.owner &&
                            it.name == binding.selector.member &&
                            it.kind in
                                setOf(
                                    DeclarationKind.METHOD,
                                    DeclarationKind.CONSTRUCTOR,
                                    DeclarationKind.FIELD,
                                )
                    }
                val configured = plan.bindings.filter { it.capability == ref }
                // A reviewed gap overrides what the tags would infer.
                val gap = plan.missing.firstOrNull { it.capability == ref }
                val inferred =
                    if (configured.isEmpty() && ref.facet == null && gap == null)
                        inferBindings(ref, active, plan, api, byContext)
                    else emptyList()
                val bindings = configured + inferred
                val matched = bindings.associateWith(::matches)
                val typed =
                    matched
                        .filterKeys { it.kind == BindingKind.TYPED }
                        .values
                        .flatten()
                        .distinctBy { it.id }
                        .sortedBy { it.id }
                val raw = matched.filterKeys { it.kind == BindingKind.RAW_BSON }.values.flatten()
                val stale = matched.filterValues { it.isEmpty() }.keys
                val tokenClaim =
                    api.declarations.any { declaration ->
                        declaration.tags.any {
                            ref.key.context in contexts(it.name) &&
                                ContractValidation.operatorToken(it.value) == ref.key.token
                        }
                    }
                val mixed =
                    matched
                        .filterValues { declarations ->
                            val claims =
                                declarations
                                    .flatMap { declaration ->
                                        declaration.tags.mapNotNull {
                                            if (it.name in ApiInventory.operatorTags)
                                                ContractValidation.operatorToken(it.value)
                                            else null
                                        }
                                    }
                                    .toSet()
                            // A selector may claim operators the manual documents only in
                            // other contexts, as a push that takes a list writes $each. Another
                            // operator of this context, one the manual doesn't document, or
                            // claims that name other tokens only, leave its capability
                            // undecided. An untagged member, such as a builder option, claims
                            // nothing.
                            claims.isNotEmpty() && ref.key.token !in claims ||
                                (claims - ref.key.token).any { other ->
                                    val documented = active.filter { it.key.token == other }
                                    documented.isEmpty() ||
                                        documented.any { it.key.context == ref.key.context }
                                }
                        }
                        .keys
                val mappingProblems =
                    if (healthy && observed && exclusion == null)
                        stale.map {
                            AuditProblem(
                                "UNRESOLVED_API_BINDING",
                                "API selector ${it.selector.owner}#${it.selector.member} matches nothing",
                                ref.key.context,
                                it.id,
                            )
                        } +
                            mixed.map {
                                AuditProblem(
                                    "AMBIGUOUS_API_BINDING",
                                    "Selector spans overloads with different operator claims",
                                    ref.key.context,
                                    it.id,
                                )
                            }
                    else emptyList()
                bindingProblems += mappingProblems
                val notes = mutableListOf<String>()
                mixed.forEach { notes += "Ambiguous operator claims in binding: ${it.id}" }
                if (inferred.isNotEmpty())
                    notes += "API mapping inferred from unambiguous method-level operator tags"

                stale.forEach {
                    notes +=
                        "Unresolved API binding: ${it.id} (${it.selector.owner}#${it.selector.member})"
                }
                if (exclusion != null) notes += "Policy exclusion: ${exclusion.reason}"
                if (gap != null) notes += REVIEWED_GAP + gap.reason
                if (!observed && omitted != null) notes += "Reviewed omission: ${omitted.reason}"
                val apiState =
                    when {
                        !healthy || !observed -> ApiState.NOT_ASSESSED
                        exclusion != null -> ApiState.EXCLUDED
                        mixed.isNotEmpty() -> ApiState.INVALID_BINDING
                        typed.isNotEmpty() && stale.isNotEmpty() -> ApiState.PARTIAL
                        stale.isNotEmpty() -> ApiState.INVALID_BINDING
                        typed.isNotEmpty() -> ApiState.TYPED_PRESENT
                        raw.isNotEmpty() -> ApiState.RAW_BSON_ONLY
                        gap != null -> ApiState.MISSING_TYPED_API
                        tokenClaim -> ApiState.UNKNOWN_MAPPING
                        else -> ApiState.MISSING_TYPED_API
                    }
                val annotationChecks =
                    if (apiState in setOf(ApiState.TYPED_PRESENT, ApiState.PARTIAL))
                        typed.map { declaration ->
                            val owner = types.getValue(declaration.owner)
                            fun effective(tag: String): List<DeclaredAnnotation> {
                                val own = declaration.tags.filter { it.name == tag }
                                val selected = if (own.isNotEmpty()) declaration else owner
                                return selected.tags
                                    .filter { it.name == tag }
                                    .map { DeclaredAnnotation(selected.id, it.value) }
                                    .sortedBy { it.declaringId + it.value }
                            }
                            val requirements = effective("mongodb.server.release")
                            val parsed = requirements.map { VersionRange.parse(it.value) }
                            val state =
                                when {
                                    declaration.documentationErrors.isNotEmpty() ||
                                        owner.documentationErrors.isNotEmpty() ->
                                        AnnotationState.AMBIGUOUS
                                    intro.state != IntroductionState.EXPLICIT ->
                                        AnnotationState.UNKNOWN_EXPECTATION
                                    requirements.isEmpty() -> AnnotationState.MISSING
                                    parsed.any { it == null } -> AnnotationState.AMBIGUOUS
                                    !parsed.filterNotNull().all {
                                        it.equivalent(parsed.first()!!)
                                    } -> AnnotationState.AMBIGUOUS
                                    parsed.first()!!.minimum == null -> AnnotationState.AMBIGUOUS
                                    parsed.first()!!.minimum!!.compareTo(intro.version!!) == 0 &&
                                        parsed.first()!!.minimumInclusive ->
                                        AnnotationState.MINIMUM_MATCH
                                    inferred.isNotEmpty() &&
                                        parsed.first()!!.minimum!! > intro.version!! ->
                                        AnnotationState.AMBIGUOUS
                                    else -> AnnotationState.INCORRECT_MINIMUM
                                }
                            OverloadVersionCheck(
                                declaration.id,
                                state,
                                requirements,
                                effective("since"),
                            )
                        }
                    else emptyList()
                CapabilityCoverage(
                    capability = ref,
                    documentation =
                        if (!healthy) DocumentationState.UNRESOLVED
                        else if (observed) DocumentationState.OBSERVED
                        else DocumentationState.REVIEWED_OMISSION,
                    newlyObserved = observed && ref !in expected,
                    api = apiState,
                    declarations = (typed + raw).map { it.id }.distinct().sorted(),
                    introduction = intro,
                    lifecycle =
                        cap?.versions
                            .orEmpty()
                            .filter { it.facet == ref.facet }
                            .map { it.copy(evidence = it.evidence.sorted()) }
                            .sortedBy { AuditJson.write(it) },
                    annotations = annotationChecks,
                    serialization = CheckState.NOT_ASSESSED,
                    behavior = CheckState.NOT_ASSESSED,
                    examples = cap?.examples ?: ExtractionState.UNRESOLVED,
                    exampleProblems = cap?.exampleProblems.orEmpty().sorted(),
                    constraints =
                        cap?.constraints
                            .orEmpty()
                            .map { it.copy(evidence = it.evidence.sorted()) }
                            .sortedBy { AuditJson.write(it) },
                    notes = notes.sorted(),
                )
            }
        val state =
            when {
                health.none { it.healthy } -> ReportState.INVALID
                health.any { !it.healthy } || bindingProblems.isNotEmpty() -> ReportState.PARTIAL
                else -> ReportState.COMPARED
            }
        return CoverageReport(
            state = state,
            catalogSha256 = AuditJson.hash(canonical(catalog)),
            planSha256 = AuditJson.hash(canonical(plan)),
            apiSha256 =
                AuditJson.hash(
                    api.copy(
                        sourceRoots = api.sourceRoots.sorted(),
                        sourceFiles = api.sourceFiles.sortedBy { it.path },
                        sourceUnits = api.sourceUnits.sortedBy { it.path },
                        declarations = api.declarations.sortedBy { it.id },
                    )
                ),
            contexts = health,
            capabilities = laterVariants(rows),
            problems =
                (health.flatMap { it.problems } + bindingProblems)
                    .distinct()
                    .sortedWith(problemOrder),
        )
    }

    private fun contexts(tag: String): Set<OperatorContext> =
        when (tag) {
            "aggregation.stage" -> setOf(OperatorContext.PIPELINE_STAGE)
            "aggregation.expression" ->
                setOf(
                    OperatorContext.AGGREGATION_EXPRESSION,
                    OperatorContext.GROUP_ACCUMULATOR,
                    OperatorContext.WINDOW_FUNCTION,
                )
            "query.filter" ->
                setOf(OperatorContext.QUERY_PREDICATE, OperatorContext.QUERY_PROJECTION)
            "update.operator" ->
                setOf(OperatorContext.UPDATE_OPERATOR, OperatorContext.UPDATE_MODIFIER)
            else -> emptySet()
        }

    /**
     * A declaration serving several capabilities carries the earliest one's annotation. A missing
     * or earlier annotation for a later one, when another capability the declaration serves is
     * earlier or of unknown introduction, reports [AnnotationState.LATER_VARIANT]; an annotation
     * after the capability's own introduction stays a finding. A facet's base counts only when the
     * declaration serves it too: a method only the facet uses carries the facet's version.
     */
    private fun laterVariants(rows: List<CapabilityCoverage>): List<CapabilityCoverage> {
        val served =
            rows
                .flatMap { row -> row.annotations.map { it.declaration to row } }
                .groupBy({ it.first }) { it.second }
        fun earlier(other: CapabilityCoverage, row: CapabilityCoverage): Boolean {
            val version = other.introduction.version
            return other.introduction.state != IntroductionState.EXPLICIT ||
                version == null ||
                version < row.introduction.version!!
        }
        return rows.map { row ->
            if (row.introduction.state != IntroductionState.EXPLICIT) return@map row
            row.copy(
                annotations =
                    row.annotations.map { check ->
                        val others = served[check.declaration].orEmpty().filter { it !== row }
                        // Only a missing or earlier annotation fits an earlier capability; a
                        // minimum after this one's introduction is wrong whatever else it serves.
                        val minimum =
                            check.serverRequirements.firstOrNull()?.let {
                                VersionRange.parse(it.value)?.minimum
                            }
                        val earlierAnnotation =
                            check.state == AnnotationState.MISSING ||
                                check.state == AnnotationState.INCORRECT_MINIMUM &&
                                    minimum != null &&
                                    minimum < row.introduction.version!!
                        if (earlierAnnotation && others.any { earlier(it, row) })
                            check.copy(state = AnnotationState.LATER_VARIANT)
                        else check
                    }
            )
        }
    }

    private fun inferBindings(
        ref: CapabilityRef,
        active: List<Capability>,
        plan: CoveragePlan,
        api: ApiInventory,
        health: Map<OperatorContext, ContextHealth>,
    ): List<ApiBinding> {
        val groups =
            api.declarations
                .filter { it.kind in setOf(DeclarationKind.METHOD, DeclarationKind.CONSTRUCTOR) }
                .groupBy { ApiSelector(it.owner, it.name) }
        return groups
            .mapNotNull { (selector, declarations) ->
                if (
                    plan.bindings.any {
                        it.selector.owner == selector.owner && it.selector.member == selector.member
                    }
                )
                    return@mapNotNull null
                val claims =
                    declarations
                        .flatMap { declaration ->
                            declaration.tags.mapNotNull { tag ->
                                ContractValidation.operatorToken(tag.value)
                                    ?.takeIf { contexts(tag.name).isNotEmpty() }
                                    ?.let { tag.name to it }
                            }
                        }
                        .distinct()
                if (claims.map { it.second }.distinct().size != 1) return@mapNotNull null
                val unambiguous =
                    claims.isNotEmpty() &&
                        claims.all { (tag, token) ->
                            token == ref.key.token &&
                                contexts(tag).all { health.getValue(it).healthy } &&
                                active
                                    .filter {
                                        it.key.token == token && it.key.context in contexts(tag)
                                    }
                                    .map { it.key.context }
                                    .toSet() == setOf(ref.key.context)
                        }
                if (unambiguous)
                    ApiBinding(
                        "inferred:${selector.owner}#${selector.member}",
                        ref,
                        selector,
                        BindingKind.TYPED,
                    )
                else null
            }
            .sortedBy { it.id }
    }

    private fun introduction(capability: Capability?, facet: String?): Introduction {
        val claims =
            capability?.versions.orEmpty().filter {
                it.kind == LifecycleEvent.INTRODUCED && it.facet == facet
            }
        val versions = claims.mapNotNull { it.version?.exact()?.canonical() }.toSortedSet()
        val state =
            when {
                versions.size > 1 -> IntroductionState.CONFLICTING
                versions.size == 1 &&
                    claims.all {
                        it.version?.exact() != null && VersionRange.parse(it.rawVersion) != null
                    } &&
                    claims.any { it.confidence == EvidenceConfidence.EXPLICIT } ->
                    IntroductionState.EXPLICIT
                else -> IntroductionState.UNKNOWN
            }
        return Introduction(
            state,
            versions.singleOrNull().takeIf { state == IntroductionState.EXPLICIT },
            claims.flatMap { it.evidence }.distinct().sorted(),
        )
    }

    private fun canonical(catalog: CapabilityCatalog): CapabilityCatalog =
        catalog.copy(
            sources = catalog.sources.sortedBy { it.id },
            evidence = catalog.evidence.sortedBy { it.id },
            capabilities =
                catalog.capabilities
                    .sortedBy { it.key.id() }
                    .map { cap ->
                        cap.copy(
                            observations =
                                cap.observations
                                    .sortedBy { it.source }
                                    .map { it.copy(evidence = it.evidence.sorted()) },
                            facets =
                                cap.facets
                                    .sortedBy { it.id }
                                    .map { it.copy(evidence = it.evidence.sorted()) },
                            versions =
                                cap.versions
                                    .map { it.copy(evidence = it.evidence.sorted()) }
                                    .sortedBy { AuditJson.write(it) },
                            constraints =
                                cap.constraints
                                    .map { it.copy(evidence = it.evidence.sorted()) }
                                    .sortedBy { AuditJson.write(it) },
                            exampleProblems = cap.exampleProblems.sorted(),
                        )
                    },
            discoveryProblems = catalog.discoveryProblems.sortedWith(problemOrder),
        )

    private fun canonical(plan: CoveragePlan): CoveragePlan =
        plan.copy(
            contexts =
                plan.contexts
                    .sortedBy { it.context.name }
                    .map {
                        it.copy(
                            expected = it.expected.sortedBy { ref -> ref.id() },
                            intentionalEmptyEvidence = it.intentionalEmptyEvidence.sorted(),
                        )
                    },
            api = plan.api.copy(sourceRoots = plan.api.sourceRoots.sorted()),
            bindings = plan.bindings.sortedBy { it.id },
            exclusions = plan.exclusions.sortedBy { it.capability.id() },
            missing = plan.missing.sortedBy { it.capability.id() },
            omissions =
                plan.omissions
                    .sortedBy { it.capability.id() }
                    .map { it.copy(evidence = it.evidence.sorted()) },
        )
}
