package dev.morphia.audits.repairs

import dev.morphia.audits.bindings.BindingDescriptor
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale

/** One case of the generated operator-case namespace; core's `GeneratedCases.Case` reads it. */
data class ManifestCase(val name: String, val kind: String, val scenario: String, val test: String)

/**
 * A file the namespace owns: `GENERATED` files are rewritten by generation, `PROTECTED` ones never.
 */
data class OwnedFile(val path: String, val sha256: String, val owner: String)

data class CaseManifest(
    val schemaVersion: Int = 1,
    val cases: List<ManifestCase>,
    val files: List<OwnedFile>,
)

object CaseManifests {
    /** Where core reads the manifest, relative to the repository root. */
    const val PATH = "core/src/test/resources/dev/morphia/test/generated/manifest.json"
    private const val resources = "core/src/test/resources"

    /**
     * The manifest for the cases in [ledger]: each case, its scenario (generated from the override)
     * and, for a migrated case, its test source (a moved handwritten body, so protected). A file in
     * [contents] (by repository path) is hashed from there rather than from disk, so a candidate
     * can describe files it has not written.
     */
    fun capture(
        repository: Path,
        ledger: FixtureMigrationLedger,
        contents: Map<String, ByteArray> = emptyMap(),
    ): CaseManifest {
        val root = repository.toRealPath()
        val cases =
            ledger.migrations.map {
                require(it.scenario.startsWith("$resources/")) {
                    "${it.fixture}: scenario outside core"
                }
                ManifestCase(it.fixture, it.kind, it.scenario.removePrefix(resources), it.test)
            }
        val files =
            ledger.migrations
                .flatMap { migrated ->
                    val source =
                        "core/src/test/java/" +
                            migrated.test.substringBefore('#').replace('.', '/') +
                            ".java"
                    // A converted case's test stays a legacy test, owned by the binding manifest.
                    if (migrated.kind == CONVERTED) listOf(migrated.scenario to "GENERATED")
                    else listOf(migrated.scenario to "GENERATED", source to "PROTECTED")
                }
                .plus(protectedResources(root).map { it to "PROTECTED" })
                .distinct()
                .map { (path, owner) ->
                    OwnedFile(
                        path.removePrefix("core/"),
                        sha256(contents[path] ?: Files.readAllBytes(root.resolve(path))),
                        owner,
                    )
                }
                .sortedBy { it.path }
        return CaseManifest(cases = cases.sortedBy { it.name }, files = files)
    }

    /**
     * Reviewed resources of the namespace that are not cases: runner self-tests and golden vectors.
     */
    private fun protectedResources(root: Path): List<String> =
        listOf("fixtures/runner", "vectors").flatMap { folder ->
            val start = root.resolve("$resources/dev/morphia/test/generated/$folder")
            if (!Files.isDirectory(start)) emptyList()
            else
                Files.walk(start).use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) }
                        .map { root.relativize(it).toString().replace('\\', '/') }
                        .toList()
                }
        }

    /**
     * The registry check: every documented example has at most one owner, a legacy test bound in
     * [descriptor] or a case in [manifest].
     */
    fun owners(descriptor: BindingDescriptor, manifest: CaseManifest) {
        val legacy =
            descriptor.classes.flatMap { type ->
                type.bindings.map {
                    "${type.resources.removePrefix("dev/morphia/test/")}/${it.resource}" to
                        "${type.type}#${it.method}"
                }
            }
        // A converted case is run by its legacy test, so the two name one owner. Fixture
        // directories that differ only by case collide on a case-insensitive file system.
        val problems =
            (legacy + manifest.cases.map { it.name to it.test })
                .distinct()
                .groupingBy { it.first.lowercase(Locale.ROOT) }
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .sorted()
                .map { "Example has more than one owner: $it" }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
