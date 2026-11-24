package dev.morphia.audits.repairs

import com.fasterxml.jackson.annotation.JsonInclude
import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.shell.AggregateOperation
import dev.morphia.audits.shell.FindOperation
import dev.morphia.audits.shell.IndexOperation
import dev.morphia.audits.shell.InsertOperation
import dev.morphia.audits.shell.LegacyDocuments
import dev.morphia.audits.shell.ProgramBlock
import dev.morphia.audits.shell.ProgramFormat
import dev.morphia.audits.shell.ProgramRole
import dev.morphia.audits.shell.ShellDialect
import dev.morphia.audits.shell.ShellLiteralState
import dev.morphia.audits.shell.ShellProgramRequest
import dev.morphia.audits.shell.ShellPrograms
import dev.morphia.audits.shell.UpdateOperation
import java.security.MessageDigest
import java.util.HexFormat
import org.bson.BsonDocument
import org.bson.BsonValue

/** How a source page's commit relates to the Morphia commit that wrote the fixture. */
enum class SourceRelation {
    /** The page as of the fixture's origin commit (or just before the manual move removed it). */
    AT_ORIGIN,
    /**
     * The fixture predates every page at that path, so the source is the commit that first added
     * the page. It pins the documented values a declaration changes; it is not where the fixture
     * came from.
     */
    AFTER_ORIGIN,
}

/** A page of the documentation repository at an exact commit. */
data class FormSource(
    val commit: String,
    val path: String,
    val relation: SourceRelation = SourceRelation.AT_ORIGIN,
) {
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
     * (they are licensed separately), so these pins record exactly which page text was reviewed.
     */
    val pageBlob: String = "",
    val includes: List<FormInclude> = emptyList(),
    /** Number type changes; allowed only when the lock is filed as NUMERIC_TYPE. */
    val retypes: List<NumericRetype> = emptyList(),
    /** Value corrections; allowed only when the lock is filed as DOCS_ERROR or FIXTURE_CURATION. */
    val corrections: List<FormCorrection> = emptyList(),
    /** File name of the lock marker: `lock`, or `lockd` for the one fixture that spells it so. */
    val marker: String = "lock",
    /** Result arrays the server builds in no defined order; omitted from the review when empty. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val unorderedArrays: List<UnorderedArray> = emptyList(),
)

/**
 * A result array compared as a multiset: [path] is its RFC 6901 pointer within each result, and
 * [reason] is the reviewed evidence that the server does not order it.
 */
data class UnorderedArray(val path: String, val reason: String)

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
    /**
     * The tool reproduces the fixture, but a reviewer rejected the declaration it drafts: its
     * corrections are not the edits the lock describes. The detail is the reviewer's reason; only a
     * reviewer sets this gap, and capture checks that the fixture still reproduces.
     */
    REJECTED,
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
    /**
     * Declarations whose fixtures moved onto the operator case runner. They keep their reviewed
     * pins; `FixtureMigration.verify` checks the migration ledger against them.
     */
    val migrated: List<FormDeclaration> = emptyList(),
)

enum class BlockRole {
    ACTION,
    DATA,
    EXPECTED,
    INDEX,
}

/** Source text as extracted from the page, so the claim can be rechecked without the page. */
data class SourceBlock(val role: BlockRole, val text: String, val sha256: String)

/** A documented operation in BSON: seed data by collection, the action, and its printed result. */
/** An index the page creates: its collection, key document and `createIndex` options. */
data class DocumentedIndex(
    val collection: String,
    val keys: BsonDocument,
    val options: BsonDocument?,
)

data class DocumentedScenario(
    val collection: String,
    val data: Map<String, List<BsonDocument>>,
    val shape: ActionShape,
    val action: List<BsonValue>,
    /**
     * What the documented results prove, or null when the page shows none. A page's results are
     * what mongosh printed, even when they read as JSON: a printed `10` is an Int32 or the same
     * Double, so they are constraints, never guessed BSON values.
     */
    val printed: List<PublishedDocument>?,
    /** For an update, whether the page uses `updateMany` rather than `updateOne`. */
    val many: Boolean = false,
    /**
     * The results read as documents, when every block reads that way: what a correction's pointer
     * addresses. The scenario never carries them; see [printed].
     */
    val documents: List<BsonValue>? = null,
    /** The indexes the page creates before the action, in order. */
    val indexes: List<DocumentedIndex> = emptyList(),
    /** The page's other spellings of the action, when it declares alternative commands. */
    val alternatives: List<List<BsonValue>> = emptyList(),
)

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
    /** The lock this override retired; null for a never-locked fixture's pinned reference. */
    val retiredLock: RepairMarker?,
    /** SHA-256 of every remaining fixture file, by file name. */
    val fixtureFiles: Map<String, String>,
    /** The test's extra collections by data file, as the repair inventory records them. */
    val collections: Map<String, String> = emptyMap(),
    val retypes: List<NumericRetype> = emptyList(),
    val corrections: List<FormCorrection> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val unorderedArrays: List<UnorderedArray> = emptyList(),
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
 * The documented reference of each never-locked fixture: the page as of the commit that created the
 * fixture, which reproduces the fixture exactly, or, where the page prints its results as mongosh
 * prints them, satisfies what those printed results prove (counted as `references.printed`).
 * [references] are [FormOverride]s without a retired lock, rules, retypes or corrections;
 * [unresolved] holds the fixtures no candidate reproduces, with the gap of their first candidate.
 * This file is the reviewed artifact and is frozen: pages are not committed, so the checks read its
 * recorded source text.
 */
data class LegacyReferenceReport(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val sourceRepository: String,
    val references: List<FormOverride>,
    val unresolved: List<FormUnresolved>,
    val summary: Map<String, Int>,
)

/**
 * The frozen record of the MORPHIA_FORM migration, which replaced locks with declared equivalences
 * and pinned never-locked fixtures to the pages that reproduce them. The commands that read those
 * historical pages with the retired RST extractor are gone; what remains reads the documented
 * operations back from the recorded source text ([documented]) and rechecks the committed records.
 */
object FormMigration {
    /**
     * Rechecks the frozen references: each fixture is referenced once, exactly (no retired lock,
     * rules, retypes or corrections), with intact source digests, and has moved onto the runner
     * ([moved]). The migration ledger checks each moved case against its reference.
     */
    fun verifyReferences(report: LegacyReferenceReport, moved: Set<String>) {
        val problems = mutableListOf<String>()
        (report.references.map { it.fixture } + report.unresolved.map { it.fixture })
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "Fixture referenced more than once: $it" }
        report.references.forEach { reference ->
            if (
                reference.retiredLock != null ||
                    reference.rules.isNotEmpty() ||
                    reference.retypes.isNotEmpty() ||
                    reference.corrections.isNotEmpty()
            ) {
                problems += "${reference.fixture}: a reference reproduces its fixture exactly"
            }
            problems += digests(reference)
            if (reference.fixture !in moved) {
                problems += "${reference.fixture}: a referenced fixture has not moved"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /**
     * Rechecks the frozen migration report against its review: each override matches its
     * declaration and keeps intact source digests, and the unresolved fixtures are the review's.
     */
    fun verify(review: FormReview, report: FormMigrationReport) {
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
                    declaration.lock != override.retiredLock?.text ||
                    declaration.pageBlob != override.pageBlob ||
                    declaration.includes != override.includes ||
                    declaration.retypes != override.retypes ||
                    declaration.corrections != override.corrections
            ) {
                problems += "${override.fixture}: override differs from its declaration"
            }
            problems += digests(override)
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    private fun digests(override: FormOverride): List<String> =
        override.blocks
            .filter { sha256(it.text.toByteArray()) != it.sha256 }
            .map { "${override.fixture}: ${it.role} source text digest mismatch" }

    private sealed interface Examined {
        data class Gap(val gap: FormGap, val detail: String) : Examined
    }

    /**
     * The documented operation in [blocks], or a gap naming why it cannot be lowered: the same
     * reading as [documented] of an override, without one.
     */
    fun documented(
        label: String,
        shape: ActionShape?,
        blocks: List<SourceBlock>,
        alternatives: Boolean = false,
    ): DocumentedScenario {
        val documented =
            when (val parsed = documented(blocks)) {
                is Examined.Gap -> throw IllegalArgumentException("$label: ${parsed.detail}")
                else -> parsed as Documented
            }
        // Alternative commands, when declared, are the page's equivalent spellings of one action.
        val action =
            if (alternatives) documented.alternatives.first() else documented.alternatives.single()
        require(shape == null || action.shape == shape) { "$label: action is ${action.shape}" }
        val expected = blocks.filter { it.role == BlockRole.EXPECTED }
        val printed =
            if (expected.isEmpty()) null
            else
                when (val read = published(documented.collection, expected)) {
                    is Examined.Gap -> throw IllegalArgumentException("$label: ${read.detail}")
                    else -> @Suppress("UNCHECKED_CAST") (read as List<PublishedDocument>)
                }
        val listed = expected.map { documents(it.text) }
        return DocumentedScenario(
            documented.collection,
            documented.data.mapValues { (_, values) -> values.map { it.asDocument() } },
            action.shape,
            action.values,
            printed,
            action.many,
            listed.takeIf { it.isNotEmpty() && it.all { rows -> rows != null } }?.flatMap { it!! },
            indexes = documented.indexes,
            alternatives =
                if (alternatives) documented.alternatives.drop(1).map { it.values } else emptyList(),
        )
    }

    /**
     * The documented operation an override was proven against, read from its recorded source text.
     * Its declared rules travel with it to the runner, and its retypes and corrections are applied
     * by `FixtureMigration`; declared alternative commands travel as the action's alternatives.
     */
    fun documented(override: FormOverride): DocumentedScenario {
        // The documented data names every collection, so a test's extra collections need nothing
        // more; their legacy data files were loaded under those same names.
        override.blocks.forEach {
            require(sha256(it.text.toByteArray()) == it.sha256) {
                "${override.fixture}: ${it.role} source text digest mismatch"
            }
        }
        return documented(
            override.fixture,
            override.shape,
            override.blocks,
            FormRule.ALTERNATIVE_COMMANDS in override.rules,
        )
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
        val indexes: List<DocumentedIndex> = emptyList(),
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
                        } +
                            blocks
                                .filter { it.role == BlockRole.INDEX }
                                .mapIndexed { index, block ->
                                    ProgramBlock("index$index", block.text, ProgramRole.SETUP)
                                } +
                            ProgramBlock("action", action.text, ProgramRole.ACTION),
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return gap(FormGap.SOURCE_UNSUPPORTED, program.problems.joinToString { it.code })
        }
        val setup = program.commands.filter { it.role == ProgramRole.SETUP }
        setup
            .firstOrNull { it.operation !is InsertOperation && it.operation !is IndexOperation }
            ?.let {
                return gap(FormGap.SOURCE_UNSUPPORTED, "setup ${it.operation.kind}")
            }
        val inserts = setup.filter { it.operation is InsertOperation }
        val indexes =
            setup
                .filter { it.operation is IndexOperation }
                .map {
                    val index = it.operation as IndexOperation
                    DocumentedIndex(
                        it.namespace?.collection
                            ?: return gap(
                                FormGap.SOURCE_UNSUPPORTED,
                                "an index without a collection",
                            ),
                        index.keys.value().asDocument(),
                        index.options?.value()?.asDocument(),
                    )
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
            inserts
                .groupBy({ it.namespace!!.collection }) { command ->
                    (command.operation as InsertOperation).documents.map { it.value() }
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
        return Documented(alternatives, collection, data, indexes)
    }

    /**
     * The constraints that results printed as mongosh prints them prove, or a gap. Printed text
     * proves less than BSON, so this is a [PublishedDocument] per result, never a guessed value.
     */
    private fun published(collection: String, expected: List<SourceBlock>): Any {
        // The results are read back as the default mongosh profile reads them; a find on the
        // action's collection certifies that profile for every action kind.
        if (collection.any { it == '"' || it == '\\' || it.isISOControl() }) {
            return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "collection name $collection")
        }
        val owner = "db.getCollection(\"$collection\").find({})"
        val program =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        listOf(ProgramBlock("owner", owner, ProgramRole.ACTION)) +
                            expected.mapIndexed { index, block ->
                                ProgramBlock(
                                    "expected$index",
                                    block.text,
                                    ProgramRole.EXPECTED,
                                    ProgramFormat.SHELL_RESULTS,
                                )
                            },
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return Examined.Gap(FormGap.SOURCE_UNSUPPORTED, "expected syntax")
        }
        return program.expectations.flatMap { it.published.orEmpty() }
    }

    private fun documents(text: String): List<BsonValue>? =
        LegacyDocuments.parse(text).takeIf { it.state == ShellLiteralState.NORMALIZED }?.documents

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
