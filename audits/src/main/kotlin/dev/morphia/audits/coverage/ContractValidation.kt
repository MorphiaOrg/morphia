package dev.morphia.audits.coverage

import dev.morphia.audits.api.ApiInventory
import dev.morphia.audits.api.DeclarationKind
import java.net.URI
import javax.lang.model.SourceVersion

internal object ContractValidation {
    private val sha256 = Regex("[0-9a-f]{64}")
    private val revision = Regex("[0-9a-f]{40}")
    private val product = Regex("[a-z][a-z0-9-]*")
    private val facet = Regex("[a-z][a-z0-9-]*")

    fun relative(path: String, root: Boolean = false): Boolean =
        (root && path == ".") ||
            (path.isNotBlank() &&
                !path.startsWith('/') &&
                '\\' !in path &&
                ':' !in path &&
                path.split('/').none { it in setOf("", ".", "..") })

    fun operatorToken(value: String): String? = OperatorTokens.leading(value)

    fun validate(
        catalog: CapabilityCatalog,
        plan: CoveragePlan,
        api: ApiInventory,
    ): List<AuditProblem> {
        val problems = mutableListOf<AuditProblem>()
        fun check(valid: Boolean, code: String, message: String, subject: String? = null) {
            if (!valid) problems += AuditProblem(code, message, null, subject)
        }
        fun <T> unique(values: List<T>, subject: String) =
            check(
                values.distinct().size == values.size,
                "DUPLICATE_ID",
                "Duplicate identities in $subject",
                subject,
            )
        fun key(key: CapabilityKey) {
            check(
                key.product == plan.product && OperatorTokens.valid(key.token),
                "INVALID_CAPABILITY_KEY",
                "Invalid product/operator key",
                key.id(),
            )
        }
        fun ref(ref: CapabilityRef) {
            key(ref.key)
            check(
                ref.facet == null || facet.matches(ref.facet),
                "INVALID_FACET",
                "Invalid facet identifier",
                ref.id(),
            )
        }
        check(
            catalog.schemaVersion == 1 && plan.schemaVersion == 1 && api.schemaVersion == 1,
            "UNSUPPORTED_SCHEMA",
            "All input schemas must be version 1",
        )
        check(
            product.matches(plan.product) && catalog.product == plan.product,
            "PRODUCT_MISMATCH",
            "Catalog and plan must have the same explicit product",
        )
        check(
            catalog.discoveryProblems.all { it.code.isNotBlank() && it.message.isNotBlank() },
            "INVALID_DISCOVERY_DIAGNOSTIC",
            "Discovery failures must retain a code and explanation",
        )
        unique(catalog.sources.map { it.id }, "sources")
        unique(catalog.evidence.map { it.id }, "evidence")
        unique(catalog.capabilities.map { it.key }, "capabilities")
        val sources = catalog.sources.associateBy { it.id }
        val evidence = catalog.evidence.associateBy { it.id }
        fun evidence(ids: List<String>, subject: String, nonempty: Boolean = true) {
            check(
                !nonempty || ids.isNotEmpty(),
                "MISSING_EVIDENCE",
                "Evidence is required",
                subject,
            )
            unique(ids, "evidence references for $subject")
            check(
                ids.all { it in evidence },
                "DANGLING_EVIDENCE",
                "An evidence reference is missing",
                subject,
            )
        }
        check(
            catalog.sources.any { it.role == SourceRole.CURRENT },
            "MISSING_CURRENT_SOURCE",
            "At least one current documentation source is required",
        )
        catalog.sources.forEach { source ->
            check(
                source.id.isNotBlank() &&
                    runCatching { URI(source.uri).isAbsolute }.getOrDefault(false),
                "INVALID_SOURCE",
                "Source identity and absolute URI are required",
                source.id,
            )
            check(
                sha256.matches(source.contentSha256) && relative(source.root, true),
                "INVALID_SOURCE_PIN",
                "Source content digest/root is invalid",
                source.id,
            )
            check(
                if (source.kind == SourceKind.GIT) source.revision?.matches(revision) == true
                else source.revision == null,
                "INVALID_SOURCE_REVISION",
                "Git sources require a full revision; other sources use content digests",
                source.id,
            )
            check(
                source.parser.name.isNotBlank() &&
                    source.parser.version.isNotBlank() &&
                    sha256.matches(source.parser.sha256) &&
                    source.normalizerVersion.isNotBlank(),
                "MISSING_TOOL_PIN",
                "Parser and normalizer identities must be pinned",
                source.id,
            )
            check(
                source.label.isNotBlank(),
                "MISSING_SOURCE_LABEL",
                "A source label is required",
                source.id,
            )
        }
        catalog.evidence.forEach { item ->
            check(
                item.id.isNotBlank() &&
                    item.source in sources &&
                    relative(item.path) &&
                    item.anchor.isNotBlank(),
                "INVALID_EVIDENCE",
                "Evidence must identify a pinned source, resource and anchor",
                item.id,
            )
            check(
                item.startLine > 0 &&
                    item.endLine >= item.startLine &&
                    sha256.matches(item.textSha256),
                "INVALID_EVIDENCE_SPAN",
                "Evidence span/digest is invalid",
                item.id,
            )
        }
        catalog.capabilities.forEach { cap ->
            key(cap.key)
            check(
                cap.observations.isNotEmpty(),
                "MISSING_OBSERVATION",
                "Capabilities require source observations",
                cap.key.id(),
            )
            unique(cap.observations.map { it.source }, "observations for ${cap.key.id()}")
            cap.observations.forEach { observation ->
                check(
                    observation.source in sources,
                    "DANGLING_SOURCE",
                    "Observation source is missing",
                    cap.key.id(),
                )
                evidence(observation.evidence, cap.key.id())
                check(
                    observation.evidence.any {
                        evidence[it]?.let { item ->
                            item.kind == EvidenceKind.DEFINITION &&
                                item.source == observation.source
                        } == true
                    },
                    "MISSING_DEFINITION_EVIDENCE",
                    "A capability must have definition evidence rooted in its observation source",
                    cap.key.id(),
                )
            }
            unique(cap.facets.map { it.id }, "facets for ${cap.key.id()}")
            cap.facets.forEach { feature ->
                check(
                    facet.matches(feature.id) && feature.title.isNotBlank(),
                    "INVALID_FACET",
                    "Facets require a stable identifier and title",
                    cap.key.id(),
                )
                evidence(feature.evidence, "${cap.key.id()}/${feature.id}")
                check(
                    feature.evidence.any { evidence[it]?.kind == EvidenceKind.FEATURE },
                    "INVALID_FACET_EVIDENCE",
                    "Facets require feature evidence",
                    "${cap.key.id()}/facet/${feature.id}",
                )
            }
            cap.versions.forEach { event ->
                check(
                    event.facet == null || cap.facets.any { it.id == event.facet },
                    "DANGLING_VERSION_SCOPE",
                    "Version event references an unknown facet",
                    cap.key.id(),
                )
                check(
                    event.rawVersion.isNotBlank(),
                    "MISSING_RAW_VERSION",
                    "Retain the unmodified version argument",
                    cap.key.id(),
                )
                evidence(event.evidence, cap.key.id())
                check(
                    event.evidence.any { evidence[it]?.kind == EvidenceKind.VERSION },
                    "MISSING_VERSION_EVIDENCE",
                    "Version events require version evidence",
                    cap.key.id(),
                )
                val parsed = VersionRange.parse(event.rawVersion)
                check(
                    parsed == null || event.version == null || parsed.equivalent(event.version),
                    "VERSION_NORMALIZATION_CONFLICT",
                    "Normalized version disagrees with its raw value",
                    cap.key.id(),
                )
            }
            cap.constraints.forEach { condition ->
                check(
                    condition.expression.isNotBlank(),
                    "EMPTY_CONSTRAINT",
                    "Constraints must retain their source expression",
                    cap.key.id(),
                )
                evidence(condition.evidence, cap.key.id())
                check(
                    condition.evidence.any {
                        evidence[it]?.kind in setOf(EvidenceKind.CONSTRAINT, EvidenceKind.VERSION)
                    },
                    "INVALID_CONSTRAINT_EVIDENCE",
                    "Constraints require constraint/version evidence",
                    cap.key.id(),
                )
            }
            check(
                cap.exampleProblems.none(String::isBlank),
                "EMPTY_EXAMPLE_DIAGNOSTIC",
                "Example diagnostics must have an explanation",
                cap.key.id(),
            )
            check(
                cap.examples != ExtractionState.COMPLETE || cap.exampleProblems.isEmpty(),
                "CONTRADICTORY_EXAMPLE_STATE",
                "Complete extraction cannot have unresolved example problems",
                cap.key.id(),
            )
        }
        unique(plan.contexts.map { it.context }, "context expectations")
        check(
            plan.contexts.map { it.context }.toSet() == OperatorContext.entries.toSet(),
            "INCOMPLETE_CONTEXT_SCOPE",
            "Every context needs an expectation, including explicit empty contexts",
        )
        val expected = plan.contexts.flatMap { it.expected }
        unique(expected, "expected capability/facet identities")
        plan.contexts.forEach { context ->
            check(
                context.minimumCapabilities >= 0 &&
                    context.expected.all { it.key.context == context.context },
                "INVALID_EXPECTATION",
                "Expected keys/minimum must belong to their context",
                context.context.key(),
            )
            context.expected.forEach(::ref)
            evidence(context.intentionalEmptyEvidence, context.context.key(), false)
            check(
                context.intentionalEmptyEvidence.all {
                    evidence[it]?.kind == EvidenceKind.DISPOSITION
                },
                "INVALID_EMPTY_CONTEXT_EVIDENCE",
                "Empty contexts require explicit disposition evidence",
                context.context.key(),
            )
        }
        val allRefs =
            catalog.capabilities
                .flatMap { cap ->
                    listOf(CapabilityRef(cap.key, null)) +
                        cap.facets.map { CapabilityRef(cap.key, it.id) }
                }
                .toSet() + expected
        val bindingRefs = catalog.currentRefs() + expected
        unique(plan.bindings.map { it.id }, "API binding identities")
        unique(plan.bindings.map { it.capability to it.selector }, "API selectors")
        plan.bindings.forEach { binding ->
            ref(binding.capability)
            check(
                binding.id.isNotBlank() && binding.capability in bindingRefs,
                "UNKNOWN_BINDING_TARGET",
                "Binding target must be currently observed or explicitly expected",
                binding.id,
            )
            check(
                SourceVersion.isName(binding.selector.owner) &&
                    (binding.selector.member == "<init>" ||
                        (SourceVersion.isIdentifier(binding.selector.member) &&
                            !SourceVersion.isKeyword(binding.selector.member))),
                "INVALID_API_SELECTOR",
                "A fully qualified owner and member are required",
                binding.id,
            )
        }
        unique(plan.exclusions.map { it.capability }, "policy exclusions")
        plan.exclusions.forEach {
            ref(it.capability)
            check(
                it.capability in allRefs && it.reason.isNotBlank(),
                "INVALID_EXCLUSION",
                "Exclusions need a known target and rationale",
                it.capability.id(),
            )
        }
        unique(plan.omissions.map { it.capability }, "expected omissions")
        plan.omissions.forEach {
            ref(it.capability)
            check(
                it.capability in expected && it.reason.isNotBlank(),
                "INVALID_OMISSION",
                "Omissions must explain an expected identity",
                it.capability.id(),
            )
            evidence(it.evidence, it.capability.id())
            check(
                it.evidence.all { evidence[it]?.kind == EvidenceKind.DISPOSITION },
                "INVALID_OMISSION_EVIDENCE",
                "Omissions require disposition evidence",
                it.capability.id(),
            )
        }
        val expectedApi = plan.api
        check(
            expectedApi.sourceRoots.isNotEmpty() &&
                expectedApi.sourceRoots.all { relative(it, true) } &&
                expectedApi.minimumDeclarations > 0 &&
                expectedApi.minimumOperatorTags > 0,
            "INVALID_API_EXPECTATION",
            "API roots and nonzero lower bounds are required",
        )
        unique(expectedApi.sourceRoots, "expected API roots")
        unique(api.sourceRoots, "API roots")
        check(
            api.sourceRoots.toSet() == expectedApi.sourceRoots.toSet(),
            "API_ROOT_MISMATCH",
            "The API inventory does not cover the expected roots",
        )
        check(
            api.declarations.size >= expectedApi.minimumDeclarations &&
                api.sourceFiles.isNotEmpty(),
            "INCOMPLETE_API_INVENTORY",
            "The API inventory is empty or below the expected lower bound",
        )
        check(
            api.declarations.sumOf {
                it.tags.count { tag ->
                    tag.name in ApiInventory.operatorTags && operatorToken(tag.value) != null
                }
            } >= expectedApi.minimumOperatorTags,
            "INCOMPLETE_OPERATOR_API_TAGS",
            "Too few public operator-tagged declarations",
        )
        unique(api.sourceFiles.map { it.path }, "API source paths")
        unique(api.sourceUnits.map { it.path }, "API compilation units")
        unique(api.declarations.map { it.id }, "API declarations")
        val files = api.sourceFiles.associateBy { it.path }
        check(
            api.sourceUnits.map { it.path }.toSet() == files.keys,
            "INCOMPLETE_API_UNITS",
            "Compilation units must account for every source file",
        )
        api.sourceFiles.forEach { file ->
            check(
                relative(file.path) && sha256.matches(file.sha256) && file.size >= 0,
                "INVALID_API_SOURCE",
                "API source path/digest/size is invalid",
                file.path,
            )
        }
        check(
            api.sourceRoots.all { root ->
                files.keys.any { root == "." || it.startsWith("$root/") }
            },
            "EMPTY_API_ROOT",
            "Each expected source root must contain source files",
        )
        check(
            files.keys.all { path -> api.sourceRoots.any { it == "." || path.startsWith("$it/") } },
            "API_SOURCE_OUTSIDE_ROOT",
            "API sources must belong to the declared roots",
        )
        val types =
            api.declarations.filter { it.kind == DeclarationKind.TYPE }.map { it.id }.toSet()
        api.declarations.forEach { declaration ->
            check(
                declaration.id.isNotBlank() &&
                    declaration.owner in types &&
                    declaration.visibility == "public" &&
                    "public" in declaration.modifiers,
                "INVALID_API_DECLARATION",
                "API declarations must have a public owner and identity",
                declaration.id,
            )
            check(
                declaration.source.path in files && declaration.source.line > 0,
                "INVALID_API_LOCATION",
                "API declaration is missing its source",
                declaration.id,
            )
        }
        check(
            api.repositoryRevision.isNotBlank() &&
                api.parser.engine == "jdk-javac" &&
                api.parser.languageLevel == 17 &&
                api.parser.runtimeVersion.isNotBlank() &&
                api.parser.runtimeVendor.isNotBlank(),
            "INVALID_API_PROVENANCE",
            "API revision and compiler provenance must be explicit",
        )
        check(
            api.classpath.map { it.index } == api.classpath.indices.toList() &&
                api.classpath.all { it.name.isNotBlank() && sha256.matches(it.sha256) },
            "INVALID_CLASSPATH_PROVENANCE",
            "Classpath inputs must have ordered identities and digests",
        )
        return problems.sortedWith(problemOrder)
    }
}
