package dev.morphia.audits.repairs

import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType
import dev.morphia.audits.rst.RstDocument
import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import dev.morphia.audits.shell.AggregateOperation
import dev.morphia.audits.shell.FindOperation
import dev.morphia.audits.shell.InsertOperation
import dev.morphia.audits.shell.LegacyDocuments
import dev.morphia.audits.shell.ProgramBlock
import dev.morphia.audits.shell.ProgramRole
import dev.morphia.audits.shell.ShellDialect
import dev.morphia.audits.shell.ShellLiteralState
import dev.morphia.audits.shell.ShellProgramRequest
import dev.morphia.audits.shell.ShellPrograms
import dev.morphia.audits.shell.UpdateOperation
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.bson.BsonArray
import org.bson.BsonValue

/** A page of the documentation repository at an exact commit. */
data class FormSource(val commit: String, val path: String) {
    init {
        require(Regex("[0-9a-f]{40}").matches(commit)) {
            "Source commit must be a full SHA: $commit"
        }
        require(
            (path.startsWith("source/") || path.startsWith("content/manual/manual/source/")) &&
                path.endsWith(".txt") &&
                ".." !in path.split('/')
        ) {
            "Unsupported source path: $path"
        }
    }
}

/**
 * Reviewed claim that a locked fixture is the documented example at [source] under [rules].
 * [origin] is the Morphia commit that created the fixture; [source] is the page as of that commit.
 */
data class FormDeclaration(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    /** Exact text of the lock this declaration retires. */
    val lock: String,
    val rules: List<FormRule>,
    /** Why the commands are equivalent; required exactly when [rules] has ALTERNATIVE_COMMANDS. */
    val justification: String? = null,
    /**
     * Git blob id of the reviewed page, and of each include it splices in. Pages are not committed
     * (they are licensed separately), so these pins are what a networked recapture is checked
     * against.
     */
    val pageBlob: String = "",
    val includes: List<FormInclude> = emptyList(),
)

/** Why a locked fixture cannot (yet) be reproduced from its documented source. */
enum class FormGap {
    /** The legacy extractor cannot read the page, for example because of an include. */
    PAGE_UNSUPPORTED,
    /** No example on the page carries the fixture's recorded name. */
    NO_MATCHING_EXAMPLE,
    /** The documented command or data uses syntax the shell normalizer does not lower. */
    SOURCE_UNSUPPORTED,
    /** The fixture holds files or command options this comparison does not cover. */
    FIXTURE_UNSUPPORTED,
    /** Every declared rule applied, and the fixture still differs from the source. */
    RESIDUAL_DIFFERENCE,
}

data class FormUnresolved(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    val gap: FormGap,
    val detail: String,
)

data class FormReview(
    val schemaVersion: Int,
    val repository: String,
    val declarations: List<FormDeclaration>,
    val unresolved: List<FormUnresolved>,
)

enum class BlockRole {
    ACTION,
    DATA,
    EXPECTED,
    INDEX,
}

/** Source text as extracted from the page, so the claim can be rechecked without the page. */
data class SourceBlock(val role: BlockRole, val text: String, val sha256: String)

data class FormOverride(
    val fixture: String,
    val origin: String,
    val source: FormSource,
    /** Git blob id of the page, checkable against the repository at [FormSource.commit]. */
    val pageBlob: String,
    /** Pages spliced in by `.. include::`, in the order they were read. */
    val includes: List<FormInclude>,
    /** Include targets left in place because they are not tracked files (generated extracts). */
    val unexpandedIncludes: List<String>,
    val example: String,
    val shape: ActionShape,
    val blocks: List<SourceBlock>,
    val rules: List<FormRule>,
    val retiredLock: RepairMarker,
    /** SHA-256 of every remaining fixture file, by file name. */
    val fixtureFiles: Map<String, String>,
    /** The test's extra collections by data file, as the repair inventory records them. */
    val collections: Map<String, String> = emptyMap(),
)

data class FormInclude(val path: String, val blob: String)

data class FormRuleDescription(val rule: FormRule, val description: String)

data class FormMigrationReport(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val sourceRepository: String,
    val rules: List<FormRuleDescription>,
    val overrides: List<FormOverride>,
    val unresolved: List<FormUnresolved>,
    val summary: Map<String, Int>,
)

/**
 * Replaces MORPHIA_FORM locks with declared equivalences. The documented example is extracted from
 * the page as it was when the fixture was created; the declared rules are applied to it and to the
 * curated fixture, and the two must then be BSON-identical in action, data, expected results and
 * index. A lock is retired only when the rules are sufficient and each one is necessary.
 */
object FormMigration {
    private val fixtureFileNames =
        setOf("action.json", "data.json", "expected.json", "index.json", "name", "lock")

    /** A locked fixture and the page it is believed to come from. */
    data class Candidate(val fixture: String, val origin: String, val source: FormSource)

    data class Candidates(val schemaVersion: Int, val candidates: List<Candidate>)

    /**
     * Proposes a review: the minimal rules for each reproducible fixture, the gap for the rest.
     * Each fixture uses the first candidate (in the order given) whose page reproduces it; a
     * fixture nothing reproduces reports the gap of its first candidate. The proposal is input for
     * a reviewer, not an accepted declaration.
     */
    fun draft(
        repository: Path,
        pages: Path,
        sourceRepository: String,
        candidates: List<List<Candidate>>,
        collections: Map<String, Map<String, String>> = emptyMap(),
    ): FormReview {
        val root = repository.toRealPath()
        val declarations = mutableListOf<FormDeclaration>()
        val unresolved = mutableListOf<FormUnresolved>()
        candidates
            .flatten()
            .groupBy { it.fixture }
            .toSortedMap()
            .forEach { (fixture, choices) ->
                val lock = folder(root, fixture).resolve("lock")
                require(lock.isRegularFile()) { "$fixture has no lock" }
                val bound = collections[fixture].orEmpty()
                val outcomes = choices.map { it to examine(root, pages, fixture, it.source, bound) }
                val reproduced = outcomes.firstOrNull { it.second is Examined.Reproduced }
                if (reproduced != null) {
                    val (candidate, outcome) = reproduced
                    outcome as Examined.Reproduced
                    declarations +=
                        FormDeclaration(
                            fixture,
                            candidate.origin,
                            candidate.source,
                            Files.readString(lock),
                            outcome.rules,
                            pageBlob = outcome.pageBlob,
                            includes = outcome.includes,
                        )
                } else {
                    val (candidate, outcome) = outcomes.first()
                    unresolved +=
                        FormUnresolved(
                            fixture,
                            candidate.origin,
                            candidate.source,
                            (outcome as Examined.Gap).gap,
                            outcome.detail,
                        )
                }
            }
        return FormReview(1, sourceRepository, declarations, unresolved)
    }

    fun capture(
        repository: Path,
        revision: String,
        pages: Path,
        review: FormReview,
        collections: Map<String, Map<String, String>> = emptyMap(),
    ): FormMigrationReport {
        require(review.schemaVersion == 1) { "Unsupported form review ${review.schemaVersion}" }
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        val fixtures = review.declarations.map { it.fixture } + review.unresolved.map { it.fixture }
        fixtures
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Fixture reviewed more than once: $it" }
        val overrides =
            review.declarations.mapNotNull { declaration ->
                val folder = folder(root, declaration.fixture)
                val lock = folder.resolve("lock")
                if (lock.isRegularFile() && Files.readString(lock) != declaration.lock) {
                    problems += "${declaration.fixture}: lock text differs from the declaration"
                }
                val bound = collections[declaration.fixture].orEmpty()
                when (
                    val outcome =
                        examine(root, pages, declaration.fixture, declaration.source, bound)
                ) {
                    is Examined.Gap -> {
                        problems +=
                            "${declaration.fixture}: declared rules do not reproduce the fixture: " +
                                "${outcome.gap} ${outcome.detail}"
                        null
                    }
                    is Examined.Reproduced -> {
                        val alternatives = FormRule.ALTERNATIVE_COMMANDS in declaration.rules
                        if (alternatives == declaration.justification.isNullOrBlank()) {
                            problems +=
                                "${declaration.fixture}: a justification is required exactly " +
                                    "when ALTERNATIVE_COMMANDS is declared"
                        }
                        if (outcome.rules != declaration.rules) {
                            problems +=
                                "${declaration.fixture}: needs rules ${outcome.rules}, " +
                                    "declared ${declaration.rules}"
                        }
                        if (
                            outcome.pageBlob != declaration.pageBlob ||
                                outcome.includes != declaration.includes
                        ) {
                            problems +=
                                "${declaration.fixture}: page or include blobs differ from the review"
                        }
                        FormOverride(
                            fixture = declaration.fixture,
                            origin = declaration.origin,
                            source = declaration.source,
                            pageBlob = outcome.pageBlob,
                            includes = outcome.includes,
                            unexpandedIncludes = outcome.unexpandedIncludes,
                            example = outcome.example,
                            shape = outcome.shape,
                            blocks = outcome.blocks,
                            rules = declaration.rules,
                            retiredLock =
                                RepairMarker(
                                    "lock",
                                    declaration.lock,
                                    sha256(declaration.lock.toByteArray()),
                                ),
                            fixtureFiles = fixtureFiles(folder),
                            collections = bound,
                        )
                    }
                }
            }
        review.unresolved.forEach { unresolved ->
            if (!folder(root, unresolved.fixture).resolve("lock").isRegularFile()) {
                problems += "${unresolved.fixture}: an unresolved fixture must keep its lock"
            }
            val bound = collections[unresolved.fixture].orEmpty()
            when (
                val outcome = examine(root, pages, unresolved.fixture, unresolved.source, bound)
            ) {
                is Examined.Reproduced ->
                    problems +=
                        "${unresolved.fixture}: reproducible with ${outcome.rules}; declare it"
                is Examined.Gap ->
                    if (outcome.gap != unresolved.gap || outcome.detail != unresolved.detail) {
                        problems +=
                            "${unresolved.fixture}: gap is ${outcome.gap} ${outcome.detail}, " +
                                "reviewed ${unresolved.gap} ${unresolved.detail}"
                    }
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return report(revision, review, overrides)
    }

    /**
     * Rechecks a committed report against the tree without the documentation pages: the recorded
     * source text must still reproduce each fixture under exactly its declared rules, and each
     * retired lock must be gone while each unresolved fixture keeps its lock. [collections] are the
     * tests' extra collections from the repair inventory; each override must record the same.
     */
    fun verify(
        repository: Path,
        review: FormReview,
        report: FormMigrationReport,
        collections: Map<String, Map<String, String>> = emptyMap(),
    ) {
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        val declared = review.declarations.associateBy { it.fixture }
        if (declared.keys != report.overrides.map { it.fixture }.toSet()) {
            problems += "Overrides do not match the reviewed declarations"
        }
        if (review.unresolved != report.unresolved) {
            problems += "Unresolved fixtures do not match the review"
        }
        report.overrides.forEach { override ->
            val declaration = declared[override.fixture] ?: return@forEach
            if (
                declaration.rules != override.rules ||
                    declaration.source != override.source ||
                    declaration.lock != override.retiredLock.text ||
                    declaration.pageBlob != override.pageBlob ||
                    declaration.includes != override.includes
            ) {
                problems += "${override.fixture}: override differs from its declaration"
            }
            override.blocks.forEach {
                if (sha256(it.text.toByteArray()) != it.sha256) {
                    problems += "${override.fixture}: ${it.role} source text digest mismatch"
                }
            }
            val folder = folder(root, override.fixture)
            if (folder.resolve("lock").isRegularFile()) {
                problems += "${override.fixture}: retired lock is still present"
            }
            if (fixtureFiles(folder) != override.fixtureFiles) {
                problems += "${override.fixture}: fixture files changed since the override"
            }
            if (collections[override.fixture].orEmpty() != override.collections) {
                problems += "${override.fixture}: test collections changed since the override"
            }
            when (
                val outcome =
                    reproduce(folder, override.shape, override.blocks, override.collections)
            ) {
                is Examined.Gap ->
                    problems += "${override.fixture}: ${outcome.gap} ${outcome.detail}"
                is Examined.Reproduced ->
                    if (outcome.rules != override.rules) {
                        problems +=
                            "${override.fixture}: needs rules ${outcome.rules}, " +
                                "declared ${override.rules}"
                    }
            }
        }
        report.unresolved.forEach {
            if (!folder(root, it.fixture).resolve("lock").isRegularFile()) {
                problems += "${it.fixture}: unresolved fixture lost its lock"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /** The extra collections each bound test loads, by fixture, from the repair inventory. */
    fun collections(inventory: CuratedRepairReport): Map<String, Map<String, String>> =
        inventory.examples
            .filter { it.collections.isNotEmpty() }
            .associate { it.fixture to it.collections }

    /** Deletes each retired lock after checking that every one still holds the recorded text. */
    fun apply(repository: Path, report: FormMigrationReport) {
        val root = repository.toRealPath()
        val locks = report.overrides.map { folder(root, it.fixture).resolve("lock") to it }
        val problems =
            locks.mapNotNull { (lock, override) ->
                when {
                    !lock.isRegularFile() -> null
                    sha256(Files.readAllBytes(lock)) != override.retiredLock.sha256 ->
                        "${override.fixture}: lock changed since capture"
                    else -> null
                }
            }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        locks.forEach { (lock, _) -> Files.deleteIfExists(lock) }
    }

    private fun report(
        revision: String,
        review: FormReview,
        overrides: List<FormOverride>,
    ): FormMigrationReport {
        val sorted = overrides.sortedBy { it.fixture }
        val summary = sortedMapOf<String, Int>()
        summary["overrides"] = sorted.size
        summary["overrides.exact"] = sorted.count { it.rules.isEmpty() }
        summary["unresolved"] = review.unresolved.size
        FormRule.values().forEach { rule ->
            summary["rule.$rule"] = sorted.count { rule in it.rules }
        }
        FormGap.values().forEach { gap ->
            summary["gap.$gap"] = review.unresolved.count { it.gap == gap }
        }
        return FormMigrationReport(
            repositoryRevision = revision,
            sourceRepository = review.repository,
            rules = FormRule.values().map { FormRuleDescription(it, it.description) },
            overrides = sorted,
            unresolved = review.unresolved.sortedBy { it.fixture },
            summary = summary,
        )
    }

    private sealed interface Examined {
        data class Reproduced(
            val rules: List<FormRule>,
            val shape: ActionShape,
            val blocks: List<SourceBlock>,
            val pageBlob: String = "",
            val includes: List<FormInclude> = emptyList(),
            val unexpandedIncludes: List<String> = emptyList(),
            val example: String = "",
        ) : Examined

        data class Gap(val gap: FormGap, val detail: String) : Examined
    }

    private fun examine(
        root: Path,
        pages: Path,
        fixture: String,
        source: FormSource,
        collections: Map<String, String>,
    ): Examined {
        val folder = folder(root, fixture)
        val page = pages.resolve(source.commit).resolve(source.path)
        require(page.isRegularFile()) { "Missing source page $page" }
        val bytes = Files.readAllBytes(page)
        val blob = gitBlob(bytes)
        val type =
            when (fixture.substringBeforeLast('/').substringBeforeLast('/')) {
                "aggregation/expressions" -> OperatorType.EXPRESSION
                "aggregation/stages" -> OperatorType.STAGE
                "query/filters" -> OperatorType.FILTER
                "query/updates" -> OperatorType.UPDATE
                else -> throw IllegalArgumentException("Not an operator fixture: $fixture")
            }
        val includes = mutableListOf<FormInclude>()
        val unexpanded = mutableListOf<String>()
        val examples =
            try {
                val lines =
                    expand(
                        pages.resolve(source.commit),
                        source.path.substring(0, source.path.indexOf("source/") + "source".length),
                        source.path,
                        includes,
                        unexpanded,
                        0,
                    )
                RstDocument(
                        Operator(type, page.toFile()),
                        lines
                            .dropWhile { line -> line.trim() !in listOf("Example", "Examples") }
                            .toMutableList(),
                    )
                    .examples
            } catch (failure: RuntimeException) {
                return Examined.Gap(FormGap.PAGE_UNSUPPORTED, failure.message ?: "$failure")
            }
        val name = Files.readAllLines(folder.resolve("name")).firstOrNull()?.trim() ?: ""
        val example =
            examples.singleOrNull { it.name == name }
                ?: return Examined.Gap(FormGap.NO_MATCHING_EXAMPLE, name)
        fun block(role: BlockRole, lines: List<String>): SourceBlock {
            val text = lines.joinToString("\n")
            return SourceBlock(role, text, sha256(text.toByteArray()))
        }
        val blocks =
            listOfNotNull(example.actionBlock?.let { block(BlockRole.ACTION, it.code()) }) +
                example.dataBlock.map { block(BlockRole.DATA, it.code()) } +
                listOfNotNull(
                    example.expectedBlock?.let { block(BlockRole.EXPECTED, it.code()) },
                    example.indexBlock?.let { block(BlockRole.INDEX, it.code()) },
                )
        val shape =
            when (val documented = documented(blocks)) {
                is Examined.Gap -> return documented
                else -> (documented as Documented).alternatives.first().shape
            }
        return when (val outcome = reproduce(folder, shape, blocks, collections)) {
            is Examined.Gap -> outcome
            is Examined.Reproduced ->
                outcome.copy(
                    pageBlob = blob,
                    example = example.name,
                    includes = includes.distinct(),
                )
        }
    }

    private data class Action(
        val shape: ActionShape,
        val values: List<BsonValue>,
        val many: Boolean,
    )

    /**
     * The documented example lowered to commands; it never executes anything. [data] holds the
     * documented documents by collection; [collection] is the one the action runs against.
     */
    private class Documented(
        val alternatives: List<Action>,
        val collection: String,
        val data: Map<String, List<BsonValue>>,
    )

    private fun documented(blocks: List<SourceBlock>): Any {
        fun gap(gap: FormGap, detail: String) = Examined.Gap(gap, detail)
        val action =
            blocks.singleOrNull { it.role == BlockRole.ACTION }
                ?: return gap(FormGap.SOURCE_UNSUPPORTED, "No single action block")
        // A data block is either a shell insert program or a plain listing of documents.
        val (programs, listings) =
            blocks
                .filter { it.role == BlockRole.DATA }
                .partition { ".insertOne(" in it.text || ".insertMany(" in it.text }
        val program =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        programs.mapIndexed { index, block ->
                            ProgramBlock("data$index", block.text, ProgramRole.SETUP)
                        } + ProgramBlock("action", action.text, ProgramRole.ACTION),
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return gap(FormGap.SOURCE_UNSUPPORTED, program.problems.joinToString { it.code })
        }
        val inserts =
            program.commands
                .filter { it.role == ProgramRole.SETUP }
                .map {
                    it.operation as? InsertOperation
                        ?: return gap(FormGap.SOURCE_UNSUPPORTED, "setup ${it.operation.kind}")
                }
        val commands = program.commands.filter { it.role == ProgramRole.ACTION }
        if (commands.isEmpty() || commands.map { it.namespace }.toSet().size != 1) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "${commands.size} action commands")
        }
        val namespaces =
            (commands + program.commands.filter { it.role == ProgramRole.SETUP }).map {
                it.namespace
            }
        if (namespaces.any { it == null || it.database != null }) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "command outside the current database")
        }
        val collection = commands.first().namespace!!.collection
        val inserted =
            program.commands
                .filter { it.role == ProgramRole.SETUP }
                .zip(inserts)
                .groupBy({ it.first.namespace!!.collection }) { (_, insert) ->
                    insert.documents.map { it.value() }
                }
                .mapValues { (_, documents) -> documents.flatten() }
        // A listing names no collection; it is the action's only when nothing else is documented.
        if (listings.isNotEmpty() && (inserted.keys - collection).isNotEmpty()) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "document listing beside several collections")
        }
        val listed =
            listings.flatMap {
                documents(it.text) ?: return gap(FormGap.SOURCE_UNSUPPORTED, "data syntax")
            }
        val data =
            if (listings.isEmpty()) inserted
            else inserted + (collection to inserted[collection].orEmpty() + listed)
        val alternatives =
            commands.map { command ->
                when (val operation = command.operation) {
                    is AggregateOperation ->
                        if (operation.options != null)
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "aggregate options")
                        else
                            Action(
                                ActionShape.PIPELINE,
                                operation.pipeline.value().asArray().values,
                                false,
                            )
                    is FindOperation ->
                        if (operation.modifiers.isNotEmpty())
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "find modifiers")
                        else
                            Action(
                                ActionShape.FIND,
                                listOfNotNull(
                                    operation.filter.value(),
                                    operation.projection?.value(),
                                ),
                                false,
                            )
                    is UpdateOperation ->
                        if (operation.options != null)
                            return gap(FormGap.FIXTURE_UNSUPPORTED, "update options")
                        else
                            Action(
                                ActionShape.UPDATE,
                                listOf(operation.filter.value(), operation.update.value()),
                                operation.sourceMethod == "updateMany",
                            )
                    else -> return gap(FormGap.SOURCE_UNSUPPORTED, "${operation.kind} action")
                }
            }
        if (alternatives.map { it.shape to it.many }.toSet().size != 1) {
            return gap(FormGap.SOURCE_UNSUPPORTED, "action commands of different kinds")
        }
        return Documented(alternatives, collection, data)
    }

    private fun reproduce(
        folder: Path,
        shape: ActionShape,
        blocks: List<SourceBlock>,
        collections: Map<String, String>,
    ): Examined {
        val files = Files.list(folder).use { stream -> stream.map { it.name }.sorted().toList() }
        (files - fixtureFileNames - collections.keys)
            .takeIf { it.isNotEmpty() }
            ?.let {
                return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "files $it")
            }
        val documented =
            when (val parsed = documented(blocks)) {
                is Examined.Gap -> return parsed
                else -> parsed as Documented
            }
        val first = documented.alternatives.first()
        if (first.shape != shape) {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "action is ${first.shape}")
        }
        val curatedText = Files.readString(folder.resolve("action.json"))
        if (shape == ActionShape.UPDATE && ("updateMany" in curatedText) != first.many) {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "updateOne/updateMany differs")
        }
        val curated =
            documents(unwrap(curatedText.trim()))
                ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "action.json syntax")

        // data.json is loaded into the action's collection; every other documented collection
        // must be a data file the test loads into a collection of that name.
        if (documented.collection in collections.values) {
            return Examined.Gap(
                FormGap.FIXTURE_UNSUPPORTED,
                "a data file targets the action collection",
            )
        }
        (documented.data.keys - documented.collection - collections.values.toSet())
            .sorted()
            .firstOrNull()
            ?.let {
                return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "no data file for collection $it")
            }
        (mapOf("data.json" to documented.collection) + collections).toSortedMap().forEach {
            (file, collection) ->
            exact(folder, file, documented.data[collection])?.let {
                return it
            }
        }
        val expected = blocks.filter { it.role == BlockRole.EXPECTED }
        exact(
                folder,
                "expected.json",
                expected
                    .takeIf { it.isNotEmpty() }
                    ?.flatMap {
                        documents(it.text)
                            ?: return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "expected syntax")
                    },
            )
            ?.let {
                return it
            }
        val index = blocks.filter { it.role == BlockRole.INDEX }
        exact(
                folder,
                "index.json",
                index
                    .takeIf { it.isNotEmpty() }
                    ?.flatMap {
                        documents(unwrap(it.text.trim()))
                            ?: return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "index syntax")
                    },
            )
            ?.let {
                return it
            }

        fun difference(rules: Set<FormRule>): String? {
            val alternatives =
                when {
                    documented.alternatives.size == 1 -> documented.alternatives
                    FormRule.ALTERNATIVE_COMMANDS in rules -> documented.alternatives
                    else -> return "action has ${documented.alternatives.size} commands"
                }
            val target = BsonArray(FormEquivalence.canonical(curated, shape, rules))
            val results =
                alternatives.map {
                    BsonComparison.compare(
                        BsonArray(FormEquivalence.canonical(it.values, shape, rules)),
                        target,
                    )
                }
            if (results.any { it.state == BsonComparisonState.EQUAL }) return null
            val result = results.first()
            return when (result.state) {
                BsonComparisonState.DIFFERENT ->
                    "action ${result.difference!!.kind} at ${result.difference.path.pointer}"
                else -> "action ${result.problem}"
            }
        }
        val all = FormRule.values().toSet()
        difference(all)?.let {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, it)
        }
        // Drop every rule the comparison does not need, so each declared rule is necessary.
        val needed = all.toMutableSet()
        FormRule.values().forEach { rule -> if (difference(needed - rule) == null) needed -= rule }
        return Examined.Reproduced(FormRule.values().filter { it in needed }, shape, blocks)
    }

    /** Data, expected results and indexes carry no form rules: they must match exactly. */
    private fun exact(folder: Path, file: String, documented: List<BsonValue>?): Examined.Gap? {
        val path = folder.resolve(file)
        if (!path.isRegularFile()) {
            return if (documented == null) null
            else Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "$file is missing")
        }
        if (documented == null) {
            return Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "$file has no documented source")
        }
        val curated =
            documents(Files.readString(path))
                ?: return Examined.Gap(FormGap.FIXTURE_UNSUPPORTED, "$file syntax")
        val result = BsonComparison.compare(BsonArray(documented), BsonArray(curated))
        return when (result.state) {
            BsonComparisonState.EQUAL -> null
            BsonComparisonState.DIFFERENT ->
                Examined.Gap(
                    FormGap.RESIDUAL_DIFFERENCE,
                    "$file ${result.difference!!.kind} at ${result.difference.path.pointer}",
                )
            BsonComparisonState.INVALID ->
                Examined.Gap(FormGap.RESIDUAL_DIFFERENCE, "$file ${result.problem}")
        }
    }

    private val include = Regex("""^(\s*)\.\. include:: (/\S+)\s*$""")

    /**
     * Splices `.. include::` targets into the page, as Sphinx does, re-indented to the directive.
     * Includes with options, relative or untracked (generated) targets and other directives are
     * left in place for the extractor, which rejects them only inside an example.
     */
    private fun expand(
        tree: Path,
        sourceRoot: String,
        path: String,
        includes: MutableList<FormInclude>,
        unexpanded: MutableList<String>,
        depth: Int,
    ): List<String> {
        require(depth <= 8) { "Includes nest too deeply at $path" }
        val bytes = Files.readAllBytes(tree.resolve(path))
        if (depth > 0) includes += FormInclude(path, gitBlob(bytes))
        val lines = bytes.decodeToString(throwOnInvalidSequence = true).lines()
        return lines.flatMapIndexed { index, line ->
            val match = include.matchEntire(line)
            val next = lines.getOrNull(index + 1)
            val options =
                next != null &&
                    next.isNotBlank() &&
                    next.indexOfFirst { !it.isWhitespace() } > line.indexOf('.') &&
                    next.trim().startsWith(":")
            val target = match?.let { sourceRoot + it.groupValues[2] }
            if (target != null && !options && !tree.resolve(target).isRegularFile()) {
                unexpanded += target
            }
            if (target == null || options || !tree.resolve(target).isRegularFile()) listOf(line)
            else {
                val indent = match.groupValues[1]
                require(".." !in target.split('/')) { "Invalid include $target" }
                expand(tree, sourceRoot, target, includes, unexpanded, depth + 1).map {
                    if (it.isBlank()) it else indent + it
                }
            }
        }
    }

    /** The template helpers drop a leading `db.<collection>.<method>(` and its closing paren. */
    private fun unwrap(text: String): String =
        if (text.startsWith("db.")) text.substringAfter("(").substringBeforeLast(")") else text

    private fun documents(text: String): List<BsonValue>? =
        LegacyDocuments.parse(text).takeIf { it.state == ShellLiteralState.NORMALIZED }?.documents

    private fun folder(root: Path, fixture: String): Path {
        require(".." !in fixture.split('/')) { "Invalid fixture $fixture" }
        return root.resolve("core/src/test/resources/dev/morphia/test/$fixture")
    }

    private fun fixtureFiles(folder: Path): Map<String, String> =
        Files.list(folder).use { stream ->
            stream
                .filter { it.isRegularFile() && it.name != "lock" }
                .toList()
                .sortedBy { it.name }
                .associate { it.name to sha256(Files.readAllBytes(it)) }
        }

    private fun gitBlob(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1")
            .apply {
                update("blob ${bytes.size}\u0000".toByteArray())
                update(bytes)
            }
            .digest()
            .let { HexFormat.of().formatHex(it) }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
