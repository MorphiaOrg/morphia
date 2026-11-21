package dev.morphia.audits.repairs

import dev.morphia.audits.bindings.LegacyBindingCatalog
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonNull
import org.bson.BsonString
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/**
 * One legacy case moved onto the operator case runner: its legacy method and fixture files are
 * gone, and its documented source, the accepted [override], is carried here so the provenance that
 * retired its lock survives. [files] holds the SHA-256 of every legacy fixture file by name,
 * including the retired lock.
 */
data class MigratedFixture(
    val fixture: String,
    val legacyType: String,
    val legacyMethod: String,
    val legacyBodySha256: String,
    val files: Map<String, String>,
    val override: FormOverride,
    /** Whether the legacy test compared results in order (`orderMatters`, true unless disabled). */
    val ordered: Boolean,
    val scenario: String,
    val scenarioSha256: String,
    /** The test that now owns the example, as `<type>#<method>`. */
    val test: String,
)

data class FixtureMigrationLedger(
    val schemaVersion: Int = 1,
    val migrations: List<MigratedFixture> = emptyList(),
)

object FixtureMigration {
    private const val resources = "core/src/test/resources/dev/morphia/test/"
    private val settings =
        JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).indent(true).build()

    /** Where the generated scenario of [fixture] lives, relative to the repository root. */
    fun scenarioPath(fixture: String): String {
        val segments = fixture.split('/')
        require(segments.size == 4 && segments.none { it.isEmpty() || it == ".." }) {
            "Not an operator fixture: $fixture"
        }
        return "${resources}generated/fixtures/${segments.drop(1).joinToString("/")}/scenario.json"
    }

    /**
     * The scenario a runner executes for [override]: its documented data, action and results.
     * [ordered] is whether the legacy test compared results in order.
     */
    fun scenario(override: FormOverride, ordered: Boolean): String {
        val documented = FormMigration.documented(override)
        require(documented.shape == ActionShape.PIPELINE) {
            "${override.fixture}: only aggregations are supported"
        }
        val collections = BsonDocument()
        documented.data.toSortedMap().forEach { (name, rows) ->
            collections[name] = BsonArray(rows)
        }
        val scenario =
            BsonDocument()
                .append("schemaVersion", BsonInt32(1))
                .append("name", BsonString(override.fixture))
                .append("context", BsonString(context(override.fixture)))
                .append(
                    "source",
                    BsonDocument()
                        .append("repository", BsonString("mongodb/docs"))
                        .append("commit", BsonString(override.source.commit))
                        .append("path", BsonString(override.source.path))
                        .append("blob", BsonString(override.pageBlob)),
                )
                .append("dataset", BsonNull.VALUE)
                .append("collections", collections)
                .append("indexes", BsonArray())
                .append(
                    "action",
                    BsonDocument()
                        .append("kind", BsonString("aggregate"))
                        .append("collection", BsonString(documented.collection))
                        .append("pipeline", BsonArray(documented.action)),
                )
                .append(
                    "expected",
                    documented.expected?.let { BsonDocument("documents", BsonArray(it)) }
                        ?: BsonDocument("none", BsonString("the page publishes no result")),
                )
                .append(
                    "comparison",
                    BsonDocument()
                        .append("order", BsonString(if (ordered) "ORDERED" else "UNORDERED"))
                        .append("rules", BsonArray())
                        .append("ignore", BsonArray())
                        .append("tolerance", BsonNull.VALUE),
                )
                .append(
                    "requirements",
                    BsonDocument()
                        .append("minimumServer", BsonNull.VALUE)
                        .append("topology", BsonString("STANDALONE")),
                )
        return scenario.toJson(settings) + "\n"
    }

    /**
     * Moves the legacy case of [fixture] onto the runner: writes its scenario from the accepted
     * override, deletes its legacy fixture directory, and returns the ledger entry. The legacy
     * method is identified in [catalog] (the current binding capture, taken before its source is
     * edited); moving its body into [test] is the caller's reviewed edit.
     */
    fun migrate(
        repository: Path,
        report: FormMigrationReport,
        inventory: CuratedRepairReport,
        catalog: LegacyBindingCatalog,
        fixture: String,
        test: String,
    ): MigratedFixture {
        val root = repository.toRealPath()
        val override =
            report.overrides.singleOrNull { it.fixture == fixture }
                ?: throw IllegalArgumentException("$fixture has no accepted override")
        val example =
            inventory.examples.singleOrNull { it.fixture == fixture }
                ?: throw IllegalArgumentException("$fixture is not in the repair inventory")
        val type = requireNotNull(example.type) { "$fixture has no bound legacy test" }
        val name = requireNotNull(example.method) { "$fixture has no bound legacy test" }
        val method =
            catalog.methods.singleOrNull { it.type == type && it.method == name }
                ?: throw IllegalArgumentException("$fixture has no bound legacy method")
        require(method.resource == fixture) { "$type#$name is bound to ${method.resource}" }
        val folder = root.resolve(resources + fixture)
        val present =
            Files.list(folder).use { paths ->
                paths
                    .filter { it.isRegularFile() }
                    .toList()
                    .associate { it.name to sha256(Files.readAllBytes(it)) }
            }
        require(present == override.fixtureFiles) {
            "$fixture: fixture files changed since capture"
        }
        require(override.retiredLock.name !in present) { "$fixture: its lock is not retired" }
        // ActionTestOptions.orderMatters defaults to true.
        val ordered = example.options.none { it.name == "orderMatters" && it.argument == "false" }
        val text = scenario(override, ordered)
        val target = root.resolve(scenarioPath(fixture))
        require(!target.exists()) { "$target already exists" }
        Files.createDirectories(target.parent)
        Files.writeString(target, text)
        present.keys.forEach { Files.delete(folder.resolve(it)) }
        Files.delete(folder)
        // Git keeps no empty directories, so neither does the migration.
        Files.list(folder.parent)
            .use { siblings -> siblings.findAny().isPresent }
            .let { if (!it) Files.delete(folder.parent) }
        return MigratedFixture(
            fixture,
            type,
            name,
            method.bodySha256,
            present + (override.retiredLock.name to override.retiredLock.sha256),
            override,
            ordered,
            scenarioPath(fixture),
            sha256(text.toByteArray()),
            test,
        )
    }

    /** Every legacy fixture file the migration removed, by repository path, with its digest. */
    fun retired(ledger: FixtureMigrationLedger): Map<String, String> =
        ledger.migrations
            .flatMap { migrated ->
                migrated.files.map { (name, digest) ->
                    "$resources${migrated.fixture}/$name" to digest
                }
            }
            .toMap()

    /**
     * [catalog] without the legacy methods the migration moved; each must be exactly as recorded.
     */
    fun remove(
        catalog: LegacyBindingCatalog,
        ledger: FixtureMigrationLedger,
    ): LegacyBindingCatalog {
        val moved = ledger.migrations.associateBy { it.legacyType to it.legacyMethod }
        val problems =
            moved.values.mapNotNull { migrated ->
                val method =
                    catalog.methods.singleOrNull {
                        it.type == migrated.legacyType && it.method == migrated.legacyMethod
                    }
                when {
                    method == null -> "${migrated.legacyType}#${migrated.legacyMethod} is not bound"
                    method.bodySha256 != migrated.legacyBodySha256 ->
                        "${migrated.legacyType}#${migrated.legacyMethod} changed before it moved"
                    else -> null
                }
            }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return catalog.copy(methods = catalog.methods.filter { (it.type to it.method) !in moved })
    }

    /**
     * Checks the tree against [ledger]: each legacy fixture directory is gone, each scenario is
     * exactly what its override produces, and each owning test exists.
     */
    fun verify(
        repository: Path,
        ledger: FixtureMigrationLedger,
        inventory: CuratedRepairReport,
        review: FormReview,
    ) {
        require(ledger.schemaVersion == 1) {
            "Unsupported fixture migration ${ledger.schemaVersion}"
        }
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        ledger.migrations
            .groupingBy { it.fixture }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Migrated more than once: $it" }
        // The reviewed declaration keeps pinning each migrated source; the ledger's copy must
        // agree.
        val declared = review.migrated.associateBy { it.fixture }
        if (declared.keys != ledger.migrations.map { it.fixture }.toSet()) {
            problems += "Migrated fixtures do not match the review's migrated declarations"
        }
        ledger.migrations.forEach { migrated ->
            val declaration = declared[migrated.fixture]
            val override = migrated.override
            if (
                declaration != null &&
                    (declaration.origin != override.origin ||
                        declaration.source != override.source ||
                        declaration.lock != override.retiredLock.text ||
                        declaration.marker != override.retiredLock.name ||
                        declaration.pageBlob != override.pageBlob ||
                        declaration.includes != override.includes ||
                        declaration.rules != override.rules ||
                        declaration.retypes != override.retypes ||
                        declaration.corrections != override.corrections)
            ) {
                problems += "${migrated.fixture}: the ledger's override differs from its review"
            }
            if (migrated.override.fixture != migrated.fixture) {
                problems +=
                    "${migrated.fixture}: the override belongs to ${migrated.override.fixture}"
            }
            if (root.resolve(resources + migrated.fixture).exists()) {
                problems += "${migrated.fixture}: legacy fixture directory is still present"
            }
            if (inventory.examples.any { it.fixture == migrated.fixture }) {
                problems += "${migrated.fixture}: a legacy test still owns it"
            }
            val expectedFiles =
                migrated.override.fixtureFiles +
                    (migrated.override.retiredLock.name to migrated.override.retiredLock.sha256)
            if (migrated.files != expectedFiles) {
                problems += "${migrated.fixture}: removed files differ from the override"
            }
            val scenario = root.resolve(migrated.scenario)
            if (migrated.scenario != scenarioPath(migrated.fixture) || !scenario.isRegularFile()) {
                problems += "${migrated.fixture}: scenario is missing at ${migrated.scenario}"
            } else {
                val text = Files.readString(scenario)
                if (sha256(text.toByteArray()) != migrated.scenarioSha256) {
                    problems += "${migrated.fixture}: scenario changed since the migration"
                }
                if (text != scenario(migrated.override, migrated.ordered)) {
                    problems +=
                        "${migrated.fixture}: scenario is not its override's documented source"
                }
            }
            val (type, method) = migrated.test.split('#').let { it[0] to it.getOrElse(1) { "" } }
            val source = root.resolve("core/src/test/java/${type.replace('.', '/')}.java")
            if (
                !source.isRegularFile() ||
                    !Regex("""\bvoid\s+$method\s*\(""").containsMatchIn(Files.readString(source))
            ) {
                problems += "${migrated.fixture}: owning test ${migrated.test} is missing"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    private fun context(fixture: String): String =
        when (fixture.split('/').take(2).joinToString("/")) {
            "aggregation/expressions" -> "EXPRESSION"
            "aggregation/stages" -> "STAGE"
            "query/filters" -> "FILTER"
            "query/updates" -> "UPDATE"
            else -> throw IllegalArgumentException("Not an operator fixture: $fixture")
        }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
