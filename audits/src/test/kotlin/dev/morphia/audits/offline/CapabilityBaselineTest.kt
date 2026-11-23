package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.discovery.CatalogDiscovery
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The accepted capability baseline: the discovery plan, the catalog it produced at the baseline's
 * documentation commit, and the coverage plan the weekly job compares each new catalog with.
 */
class CapabilityBaselineTest {
    private val root =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("audits/catalog")) }

    private fun read(path: String) = Files.readString(root.resolve(path))

    @Test
    fun theBaselineIsConsistent() {
        val plan = AuditJson.discoveryPlan(read("audits/catalog/discovery-plan.json"))
        // The plan validates: reviews carry reasons, name each definition once, and fit a catalog.
        CatalogDiscovery.canonical(plan)
        val catalog = AuditJson.catalog(read("audits/catalog/capability-catalog.json"))
        assertEquals(emptyList<Any>(), catalog.discoveryProblems)
        val coverage = AuditJson.plan(read("audits/catalog/coverage-plan.json"))
        val keys = catalog.capabilities.map { it.key }.toSet()
        coverage.contexts.forEach { context ->
            val observed = keys.count { it.context == context.context }
            assertEquals(observed, context.minimumCapabilities, "${context.context}")
            assertEquals(
                keys.filter { it.context == context.context }.toSet(),
                context.expected.map { it.key }.toSet(),
                "${context.context}",
            )
        }
        // Every reviewed binding and exclusion names a capability the baseline holds.
        (coverage.bindings.map { it.capability } + coverage.exclusions.map { it.capability })
            .forEach { assertTrue(it.key in keys, it.id()) }
        assertTrue(coverage.exclusions.all { it.reason.isNotBlank() })
    }
}
