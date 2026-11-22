package dev.morphia.audits.repairs

import dev.morphia.audits.bindings.LegacyBindingCatalog
import dev.morphia.audits.bindings.LegacyBindings
import dev.morphia.audits.published.PublishedEncoding
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
 * One legacy case moved onto the operator case runner, and the accepted [override] that retired its
 * lock, carried so that provenance survives. [files] holds the SHA-256 of every legacy fixture file
 * the move removed, by name, including the retired lock.
 * - `MIGRATED`: the legacy method and fixture directory are gone; the body moved into [test], and
 *   the scenario lives in the generated namespace.
 * - `CONVERTED`: the legacy method stays and runs through the runner's compatibility adapter; its
 *   fixture directory holds only the scenario.
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
    val kind: String = MIGRATED,
    /** The legacy method's body text, whose SHA-256 is [legacyBodySha256]. */
    val legacyBody: String = "",
    /** The legacy test's options other than `orderMatters`, which the scenario maps. */
    val options: List<OptionCall> = emptyList(),
)

data class FixtureMigrationLedger(
    val schemaVersion: Int = 1,
    val migrations: List<MigratedFixture> = emptyList(),
)

/** The check a legacy case failed when the cutover tried to move it onto the runner. */
enum class CutoverCheck {
    /** The documented source could not become a scenario. */
    SOURCE,
    /** Morphia emits a different command than the page documents. */
    SERIALIZATION,
    /** Morphia's operation and the documented one leave different results or collections. */
    BEHAVIOR,
    /** The results differ from what the page prints. */
    PUBLISHED,
}

/**
 * A legacy case the cutover tried and left on the legacy path (`LEGACY_ONLY`): its strict checks
 * fail, so it is not counted as supported until it converts. [detail] is the first difference.
 */
data class LegacyOnly(val fixture: String, val check: CutoverCheck, val detail: String)

/**
 * The cutover's outcome: the [contexts] (fixture path prefixes) it covers and its legacy-only
 * cases.
 */
data class CutoverReport(
    val schemaVersion: Int = 1,
    val contexts: List<String> = emptyList(),
    val legacyOnly: List<LegacyOnly> = emptyList(),
)

const val MIGRATED = "MIGRATED"
const val CONVERTED = "CONVERTED"

object FixtureMigration {
    private const val resources = "core/src/test/resources/dev/morphia/test/"
    private val settings =
        JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).indent(true).build()

    /** Where the scenario of [fixture] lives for [kind], relative to the repository root. */
    fun scenarioPath(fixture: String, kind: String = MIGRATED): String {
        val segments = fixture.split('/')
        require(segments.size == 4 && segments.none { it.isEmpty() || it == ".." }) {
            "Not an operator fixture: $fixture"
        }
        return when (kind) {
            MIGRATED ->
                "${resources}generated/fixtures/${segments.drop(1).joinToString("/")}/scenario.json"
            CONVERTED -> "$resources$fixture/scenario.json"
            else -> throw IllegalArgumentException("Unknown migration kind $kind")
        }
    }

    /**
     * Legacy options a converted case's scenario maps: `removeIds` ignores `_id` in every
     * comparison, `serverVersion` becomes the minimum server, and `skipActionCheck` and
     * `skipDataCheck` become skipped checks that name their reason.
     */
    val mappedOptions = setOf("removeIds", "serverVersion", "skipActionCheck", "skipDataCheck")

    /** Whether a legacy body compares results in order: `orderMatters` defaults to true. */
    fun ordered(body: String): Boolean = !unordered.containsMatchIn(body)

    private val unordered = Regex("""\.orderMatters\s*\(\s*false\s*\)""")

    /**
     * The scenario a runner executes for [override]: its documented data, action and results.
     * [ordered] is whether the legacy test compared results in order, and [options] are its other
     * options, each one of [mappedOptions].
     */
    fun scenario(
        override: FormOverride,
        ordered: Boolean,
        options: List<OptionCall> = emptyList(),
    ): String {
        require(options.all { it.name in mappedOptions && it.name != "orderMatters" }) {
            "${override.fixture}: options ${options.map { it.name }} are not mapped"
        }
        fun option(name: String) = options.singleOrNull { it.name == name }?.argument
        val skipped = BsonDocument()
        if (option("skipActionCheck") == "true") {
            skipped["serialization"] =
                BsonString("the legacy test skipped its action check (skipActionCheck)")
        }
        if (option("skipDataCheck") == "true") {
            skipped["results"] =
                BsonString("the legacy test skipped its data check (skipDataCheck)")
        }
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
                // The documented example's title, which the owning test's @DisplayName must equal.
                .append("title", BsonString(override.example))
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
                    documented.printed?.let { printed ->
                        BsonDocument(
                            "printed",
                            BsonArray(printed.map { PublishedEncoding.encode(it) }),
                        )
                    } ?: BsonDocument("none", BsonString("the page publishes no result")),
                )
                .append(
                    "comparison",
                    BsonDocument()
                        .append("order", BsonString(if (ordered) "ORDERED" else "UNORDERED"))
                        .append("rules", BsonArray(override.rules.map { BsonString(it.name) }))
                        .append(
                            "ignore",
                            BsonArray(
                                listOfNotNull(
                                    BsonString("_id").takeIf { option("removeIds") == "true" }
                                )
                            ),
                        )
                        .append("tolerance", BsonNull.VALUE),
                )
                .append(
                    "requirements",
                    BsonDocument()
                        .append(
                            "minimumServer",
                            option("serverVersion")?.let { BsonString(it) } ?: BsonNull.VALUE,
                        )
                        .append("topology", BsonString("STANDALONE")),
                )
        // Only a case with a skipped check carries the key, so the reason is never empty.
        if (!skipped.isEmpty()) scenario["skipped"] = skipped
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
        references: LegacyReferenceReport? = null,
    ): MigratedFixture =
        move(repository, report, references, inventory, catalog, fixture, test, MIGRATED)

    /**
     * Converts the legacy case of [fixture] in place: its fixture directory's files are replaced by
     * the scenario written from its accepted override, or from its pinned reference in [references]
     * when it never had a lock, and its unchanged method runs through the runner's compatibility
     * adapter. Only options the adapter maps are allowed.
     */
    fun convert(
        repository: Path,
        report: FormMigrationReport,
        inventory: CuratedRepairReport,
        catalog: LegacyBindingCatalog,
        fixture: String,
        references: LegacyReferenceReport? = null,
    ): MigratedFixture {
        val example =
            inventory.examples.singleOrNull { it.fixture == fixture }
                ?: throw IllegalArgumentException("$fixture is not in the repair inventory")
        val override =
            (report.overrides + references?.references.orEmpty()).singleOrNull {
                it.fixture == fixture
            } ?: throw IllegalArgumentException("$fixture has no accepted override or reference")
        convertible(override, example)?.let { throw IllegalArgumentException("$fixture: $it") }
        return move(
            repository,
            report,
            references,
            inventory,
            catalog,
            fixture,
            "${example.type}#${example.method}",
            CONVERTED,
        )
    }

    private fun move(
        repository: Path,
        report: FormMigrationReport,
        references: LegacyReferenceReport?,
        inventory: CuratedRepairReport,
        catalog: LegacyBindingCatalog,
        fixture: String,
        test: String,
        kind: String,
    ): MigratedFixture {
        val root = repository.toRealPath()
        val override =
            (report.overrides + references?.references.orEmpty()).singleOrNull {
                it.fixture == fixture
            } ?: throw IllegalArgumentException("$fixture has no accepted override or reference")
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
        require(override.retiredLock == null || override.retiredLock.name !in present) {
            "$fixture: its lock is not retired"
        }
        val body =
            requireNotNull(LegacyBindings.bodies(root, method.source)[name]) {
                "$type#$name has no body in ${method.source}"
            }
        require(sha256(body.toByteArray()) == method.bodySha256) {
            "$type#$name changed since its binding was captured"
        }
        val ordered = ordered(body)
        require(
            ordered == example.options.none { it.name == "orderMatters" && it.argument == "false" }
        ) {
            "$type#$name: its body and the inventory disagree on result order"
        }
        val options = example.options.filter { it.name != "orderMatters" }
        options.forEach {
            require(called(body, it)) { "$type#$name: its body does not call ${it.name}" }
        }
        val text = scenario(override, ordered, options)
        val target = root.resolve(scenarioPath(fixture, kind))
        require(!target.exists()) { "$target already exists" }
        present.keys.forEach { Files.delete(folder.resolve(it)) }
        if (kind == MIGRATED) {
            Files.delete(folder)
            // Git keeps no empty directories, so neither does the migration.
            Files.list(folder.parent)
                .use { siblings -> siblings.findAny().isPresent }
                .let { if (!it) Files.delete(folder.parent) }
        }
        Files.createDirectories(target.parent)
        Files.writeString(target, text)
        return MigratedFixture(
            fixture,
            type,
            name,
            method.bodySha256,
            present + listOfNotNull(override.retiredLock?.let { it.name to it.sha256 }),
            override,
            ordered,
            scenarioPath(fixture, kind),
            sha256(text.toByteArray()),
            test,
            kind,
            body,
            options,
        )
    }

    /**
     * Why the case of [override] cannot be converted yet, or null when the adapter supports it: a
     * `testPipeline` case with mapped options and a documented source without retypes, corrections
     * or alternative commands.
     */
    fun convertible(override: FormOverride, example: CuratedExample): String? =
        when {
            example.helpers != listOf("testPipeline") -> "only testPipeline cases can be converted"
            !example.options.all { it.name == "orderMatters" || it.name in mappedOptions } ->
                "options ${example.options.map { it.name }} are not mapped yet"
            override.retypes.isNotEmpty() || override.corrections.isNotEmpty() ->
                "retypes and corrections are not supported yet"
            FormRule.ALTERNATIVE_COMMANDS in override.rules -> "alternative commands"
            else -> null
        }

    /**
     * Checks the cutover's legacy-only cases: each was eligible (it has an override in [report] or
     * a reference in [references]), is listed once, did not move, and keeps its legacy fixture. In
     * each of the cutover's contexts, every [convertible] case of [inventory] moved or is listed,
     * so a case the batch skipped cannot go unnoticed.
     */
    fun verifyCutover(
        repository: Path,
        cutover: CutoverReport,
        ledger: FixtureMigrationLedger,
        report: FormMigrationReport,
        references: LegacyReferenceReport,
        inventory: CuratedRepairReport,
    ) {
        require(cutover.schemaVersion == 1) { "Unsupported cutover ${cutover.schemaVersion}" }
        val root = repository.toRealPath()
        val moved = ledger.migrations.map { it.fixture }.toSet()
        val eligible =
            (report.overrides.map { it.fixture } + references.references.map { it.fixture }).toSet()
        val problems = mutableListOf<String>()
        cutover.legacyOnly
            .groupingBy { it.fixture }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Listed more than once: $it" }
        cutover.legacyOnly.forEach {
            val folder = root.resolve(resources + it.fixture)
            when {
                it.fixture in moved -> problems += "${it.fixture}: legacy-only, but it moved"
                it.fixture !in eligible -> problems += "${it.fixture}: has no override or reference"
                it.detail.isBlank() -> problems += "${it.fixture}: no failing check recorded"
                !folder.resolve("action.json").isRegularFile() ||
                    folder.resolve("scenario.json").exists() ->
                    problems += "${it.fixture}: its legacy fixture is not intact"
            }
        }
        val listed = cutover.legacyOnly.map { it.fixture }.toSet()
        val examples = inventory.examples.associateBy { it.fixture }
        (report.overrides + references.references)
            .filter { override -> cutover.contexts.any { override.fixture.startsWith("$it/") } }
            .filter { override ->
                examples[override.fixture]?.let { convertible(override, it) == null } ?: false
            }
            .map { it.fixture }
            .filter { it !in moved && it !in listed }
            .sorted()
            .forEach { problems += "$it: convertible, but neither moved nor listed as legacy-only" }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /**
     * [ledger] with every scenario rewritten from its override, after a change to the scenario
     * format: each file is regenerated and its digest refreshed. Nothing else in an entry changes.
     */
    fun rewrite(repository: Path, ledger: FixtureMigrationLedger): FixtureMigrationLedger {
        val root = repository.toRealPath()
        return ledger.copy(
            migrations =
                ledger.migrations.map { migrated ->
                    val text = scenario(migrated.override, migrated.ordered, migrated.options)
                    Files.writeString(root.resolve(migrated.scenario), text)
                    migrated.copy(scenarioSha256 = sha256(text.toByteArray()))
                }
        )
    }

    /**
     * Whether [body] calls [option] with its recorded argument; the inventory records a string
     * literal's value, so the argument may be quoted.
     */
    private fun called(body: String, option: OptionCall): Boolean {
        val argument = Regex.escape(option.argument)
        return Regex("\\.${option.name}\\(\\s*(?:$argument|\"$argument\")\\s*\\)")
            .containsMatchIn(body)
    }

    /**
     * The retired lock text of every example the repair inventory still holds, by fixture: each
     * declaration, and each converted case (it keeps its legacy test); a migrated case left the
     * inventory with its test.
     */
    fun retiredLocks(review: FormReview, ledger: FixtureMigrationLedger): Map<String, String> {
        val converted = ledger.migrations.filter { it.kind == CONVERTED }.map { it.fixture }.toSet()
        return (review.declarations + review.migrated.filter { it.fixture in converted })
            .associate { it.fixture to it.lock }
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
        val moved =
            ledger.migrations
                .filter { it.kind == MIGRATED }
                .associateBy { it.legacyType to it.legacyMethod }
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
        references: LegacyReferenceReport? = null,
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
        // A never-locked fixture's pinned reference stays in the references catalog instead.
        val declared = review.migrated.associateBy { it.fixture }
        val (locked, referenced) = ledger.migrations.partition { it.override.retiredLock != null }
        if (declared.keys != locked.map { it.fixture }.toSet()) {
            problems += "Migrated fixtures do not match the review's migrated declarations"
        }
        val pinned = references?.references.orEmpty().associateBy { it.fixture }
        val reviewed =
            (review.declarations + review.migrated).map { it.fixture } +
                review.unresolved.map { it.fixture }
        (pinned.keys intersect reviewed.toSet()).sorted().forEach {
            problems += "$it: both a locked review entry and a never-locked reference"
        }
        referenced.forEach {
            if (pinned[it.fixture] != it.override) {
                problems +=
                    "${it.fixture}: the ledger's reference differs from the references catalog"
            }
        }
        ledger.migrations.forEach { migrated ->
            val declaration = declared[migrated.fixture]
            val override = migrated.override
            if (
                declaration != null &&
                    (declaration.origin != override.origin ||
                        declaration.source != override.source ||
                        declaration.lock != override.retiredLock?.text ||
                        declaration.marker != override.retiredLock?.name ||
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
            if (sha256(migrated.legacyBody.toByteArray()) != migrated.legacyBodySha256) {
                problems += "${migrated.fixture}: legacy body text does not match its digest"
            } else if (ordered(migrated.legacyBody) != migrated.ordered) {
                problems += "${migrated.fixture}: result order differs from the legacy body"
            } else if (migrated.options.any { !called(migrated.legacyBody, it) }) {
                problems += "${migrated.fixture}: options differ from the legacy body"
            }
            val folder = root.resolve(resources + migrated.fixture)
            val example = inventory.examples.singleOrNull { it.fixture == migrated.fixture }
            when (migrated.kind) {
                MIGRATED -> {
                    if (folder.exists()) {
                        problems += "${migrated.fixture}: legacy fixture directory is still present"
                    }
                    if (example != null) {
                        problems += "${migrated.fixture}: a legacy test still owns it"
                    }
                }
                CONVERTED -> {
                    val left =
                        if (!folder.exists()) emptySet()
                        else
                            Files.list(folder).use { paths ->
                                paths.map { it.name }.toList().toSet()
                            }
                    if (left != setOf("scenario.json")) {
                        problems += "${migrated.fixture}: fixture directory holds $left"
                    }
                    if (
                        example != null &&
                            example.options.filter { it.name != "orderMatters" } != migrated.options
                    ) {
                        problems += "${migrated.fixture}: options differ from the inventory"
                    }
                    val owner = example?.let { "${it.type}#${it.method}" }
                    if (
                        owner != migrated.test ||
                            migrated.test != "${migrated.legacyType}#${migrated.legacyMethod}"
                    ) {
                        problems += "${migrated.fixture}: converted, but owned by $owner"
                    }
                }
                else -> problems += "${migrated.fixture}: unknown kind ${migrated.kind}"
            }
            val expectedFiles =
                migrated.override.fixtureFiles +
                    listOfNotNull(migrated.override.retiredLock?.let { it.name to it.sha256 })
            if (migrated.files != expectedFiles) {
                problems += "${migrated.fixture}: removed files differ from the override"
            }
            val scenario = root.resolve(migrated.scenario)
            if (
                migrated.scenario != scenarioPath(migrated.fixture, migrated.kind) ||
                    !scenario.isRegularFile()
            ) {
                problems += "${migrated.fixture}: scenario is missing at ${migrated.scenario}"
            } else {
                val text = Files.readString(scenario)
                if (sha256(text.toByteArray()) != migrated.scenarioSha256) {
                    problems += "${migrated.fixture}: scenario changed since the migration"
                }
                if (text != scenario(migrated.override, migrated.ordered, migrated.options)) {
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
