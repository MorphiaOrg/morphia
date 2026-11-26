package dev.morphia.audits.extraction

import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.shell.AggregateOperation
import dev.morphia.audits.shell.FindOperation
import dev.morphia.audits.shell.IndexOperation
import dev.morphia.audits.shell.InsertOperation
import dev.morphia.audits.shell.LegacyDocuments
import dev.morphia.audits.shell.ProgramBlock
import dev.morphia.audits.shell.ProgramFormat
import dev.morphia.audits.shell.ProgramRole
import dev.morphia.audits.shell.QueryModifierKind
import dev.morphia.audits.shell.ShellDialect
import dev.morphia.audits.shell.ShellLiteralState
import dev.morphia.audits.shell.ShellProgramRequest
import dev.morphia.audits.shell.ShellPrograms
import dev.morphia.audits.shell.UpdateOperation
import java.security.MessageDigest
import java.util.HexFormat
import org.bson.BsonDocument
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

/** How the action's documents are read: the same shapes as core's `FormRules.Shape`. */
enum class ActionShape {
    /** One document per pipeline stage. */
    PIPELINE,
    /** The filter, then an optional projection. */
    FIND,
    /** The filter, then the update. */
    UPDATE,
}

enum class BlockRole {
    ACTION,
    DATA,
    EXPECTED,
    INDEX,
    /** The find a page runs after a write to show its effect; its printed result is [EXPECTED]. */
    READ,
}

/** Source text as extracted from the page, so the claim can be rechecked without the page. */
data class SourceBlock(val role: BlockRole, val text: String, val sha256: String)

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
    /** The find the page runs after a write, whose result is the printed one. */
    val read: DocumentedRead? = null,
)

/** A find a page runs after a write: its collection, filter, projection and sort. */
data class DocumentedRead(
    val collection: String,
    val filter: BsonDocument,
    val projection: BsonDocument?,
    val sort: BsonDocument?,
)

data class FormInclude(val path: String, val blob: String)

/**
 * Lowers an example's recorded source blocks to its documented operation: seed data, indexes, the
 * action and its alternatives, the page's read after a write, and what its printed results prove.
 * It never executes anything.
 */
object Documented {
    private sealed interface Examined {
        data class Gap(val detail: String) : Examined
    }

    /**
     * The documented operation in [blocks]. [alternatives] is whether the page's action block holds
     * equivalent spellings of one command, which a reviewed override declares.
     *
     * @throws IllegalArgumentException naming why the example can't be lowered
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
        val read =
            when (val parsed = read(blocks)) {
                is Examined.Gap -> throw IllegalArgumentException("$label: ${parsed.detail}")
                else -> parsed as DocumentedRead?
            }
        require(read == null || action.shape != ActionShape.FIND) { "$label: a read after a find" }
        val expected = blocks.filter { it.role == BlockRole.EXPECTED }
        val printed =
            if (expected.isEmpty()) null
            else
                when (val read = published(read?.collection ?: documented.collection, expected)) {
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
                if (alternatives) documented.alternatives.drop(1).map { it.values }
                else emptyList(),
            read = read,
        )
    }

    /**
     * The find a page runs after its write, when it shows one, or a gap. Only a filter, a
     * projection and a sort are read; any other modifier would change what the page printed.
     */
    private fun read(blocks: List<SourceBlock>): Any? {
        val reads = blocks.filter { it.role == BlockRole.READ }
        if (reads.isEmpty()) return null
        val block = reads.singleOrNull() ?: return Examined.Gap("${reads.size} reads")
        // The read runs after the example's other code, so it sees the bindings declared there.
        val before = blocks.takeWhile { it !== block }
        val program =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        before
                            .filter {
                                it.role == BlockRole.INDEX ||
                                    it.role == BlockRole.ACTION ||
                                    it.role == BlockRole.DATA &&
                                        (".insertOne(" in it.text || ".insertMany(" in it.text)
                            }
                            .mapIndexed { index, it ->
                                ProgramBlock(
                                    "before$index",
                                    it.text,
                                    if (it.role == BlockRole.ACTION) ProgramRole.ACTION
                                    else ProgramRole.SETUP,
                                )
                            } + ProgramBlock("read", block.text, ProgramRole.ACTION),
                )
            )
        if (program.state != ShellLiteralState.NORMALIZED) {
            return Examined.Gap("read: " + program.problems.joinToString { it.code })
        }
        val command =
            program.commands.filter { it.id.block == "read" }.singleOrNull()
                ?: return Examined.Gap("a read of several commands")
        val find =
            command.operation as? FindOperation
                ?: return Examined.Gap("a ${command.operation.kind} read")
        if (command.namespace.database != null) {
            return Examined.Gap("a read outside the current database")
        }
        find.modifiers
            .firstOrNull {
                it.kind != QueryModifierKind.SORT && it.kind != QueryModifierKind.PROJECT
            }
            ?.let {
                return Examined.Gap("a read with ${it.kind}")
            }
        val projections = find.modifiers.filter { it.kind == QueryModifierKind.PROJECT }
        val sorts = find.modifiers.filter { it.kind == QueryModifierKind.SORT }
        if (projections.size + listOfNotNull(find.projection).size > 1 || sorts.size > 1) {
            return Examined.Gap("a read with repeated modifiers")
        }
        return DocumentedRead(
            command.namespace.collection,
            find.filter.value().asDocument(),
            (find.projection ?: projections.singleOrNull()?.value)?.value()?.asDocument(),
            sorts.singleOrNull()?.value?.value()?.asDocument(),
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
        fun gap(detail: String) = Examined.Gap(detail)
        val action =
            blocks.singleOrNull { it.role == BlockRole.ACTION }
                ?: return gap("No single action block")
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
            return gap(program.problems.joinToString { it.code })
        }
        val setup = program.commands.filter { it.role == ProgramRole.SETUP }
        setup
            .firstOrNull { it.operation !is InsertOperation && it.operation !is IndexOperation }
            ?.let {
                return gap("setup ${it.operation.kind}")
            }
        val inserts = setup.filter { it.operation is InsertOperation }
        val indexes =
            setup
                .filter { it.operation is IndexOperation }
                .map {
                    val index = it.operation as IndexOperation
                    DocumentedIndex(
                        it.namespace?.collection ?: return gap("an index without a collection"),
                        index.keys.value().asDocument(),
                        index.options?.value()?.asDocument(),
                    )
                }
        val commands = program.commands.filter { it.role == ProgramRole.ACTION }
        if (commands.isEmpty() || commands.map { it.namespace }.toSet().size != 1) {
            return gap("${commands.size} action commands")
        }
        val namespaces =
            (commands + program.commands.filter { it.role == ProgramRole.SETUP }).map {
                it.namespace
            }
        if (namespaces.any { it == null || it.database != null }) {
            return gap("command outside the current database")
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
            return gap("document listing beside several collections")
        }
        val listed = listings.flatMap { documents(it.text) ?: return gap("data syntax") }
        val data =
            if (listings.isEmpty()) inserted
            else inserted + (collection to inserted[collection].orEmpty() + listed)
        val alternatives =
            commands.map { command ->
                when (val operation = command.operation) {
                    is AggregateOperation ->
                        if (operation.options != null) return gap("aggregate options")
                        else
                            Action(
                                ActionShape.PIPELINE,
                                operation.pipeline.value().asArray().values,
                                false,
                            )
                    is FindOperation ->
                        if (operation.modifiers.isNotEmpty()) return gap("find modifiers")
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
                        if (operation.options != null) return gap("update options")
                        else
                            Action(
                                ActionShape.UPDATE,
                                listOf(operation.filter.value(), operation.update.value()),
                                operation.sourceMethod == "updateMany",
                            )
                    else -> return gap("${operation.kind} action")
                }
            }
        if (alternatives.map { it.shape to it.many }.toSet().size != 1) {
            return gap("action commands of different kinds")
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
            return Examined.Gap("collection name $collection")
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
            return Examined.Gap("expected syntax")
        }
        return program.expectations.flatMap { it.published.orEmpty() }
    }

    private fun documents(text: String): List<BsonValue>? =
        LegacyDocuments.parse(text).takeIf { it.state == ShellLiteralState.NORMALIZED }?.documents

    fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
