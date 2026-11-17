package dev.morphia.audits.shell

import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.published.PublishedReadProfile
import dev.morphia.audits.semantics.BsonSnapshot
import dev.morphia.audits.semantics.ResultOrder

/** Formats are declared, never selected by retrying unrelated language parsers. */
enum class ProgramFormat {
    SHELL,
    LEGACY_DOCUMENTS,
    TEXT,
}

enum class ProgramRole {
    SETUP,
    ACTION,
    EXPECTED,
    EXPECTED_ERROR,
}

enum class ProgramBindingKind {
    VALUE,
    DATABASE,
    COLLECTION,
}

enum class ProgramDeclaration {
    VAR,
    LET,
    CONST,
    ASSIGNMENT,
}

enum class ShellOperationKind {
    INSERT,
    INDEX,
    FIND,
    AGGREGATE,
    UPDATE,
}

enum class QueryModifierKind {
    PROJECT,
    SORT,
    LIMIT,
    SKIP,
    HINT,
    COLLATION,
    BATCH_SIZE,
    MAX_TIME_MS,
}

/** Null database denotes the caller's isolated default database, not a literal name. */
data class LogicalNamespace(val database: String?, val collection: String) {
    init {
        require(collection.isNotBlank() && '\u0000' !in collection)
        require(database == null || database.isNotBlank() && '\u0000' !in database)
    }
}

data class ProgramLocation(val block: String, val span: ShellSpan?)

data class CommandSelector(val block: String, val command: Int) {
    init {
        require(command >= 0)
    }
}

data class ProgramExpectedError(val code: Int?, val codeName: String?, val message: String?) {
    init {
        require(code != null || codeName != null || message != null)
        require(code == null || code >= 0)
        require(codeName == null || Regex("[A-Za-z_][A-Za-z0-9_]*").matches(codeName))
        require(message == null || message.isNotEmpty() && message.length <= 2048)
    }
}

data class ProgramBlock(
    val id: String,
    val source: String,
    val role: ProgramRole,
    val format: ProgramFormat = ProgramFormat.SHELL,
    /** Required to seed/aggregate an explicitly declared legacy document fragment. */
    val namespace: LogicalNamespace? = null,
    /** Required when more than one action could own a published result. */
    val expectedFor: CommandSelector? = null,
    val expectedError: ProgramExpectedError? = null,
)

data class ShellProgramRequest(
    val schemaVersion: Int = 1,
    val dialect: ShellDialect,
    val blocks: List<ProgramBlock>,
    val resultOrder: ResultOrder = ResultOrder.ORDERED,
)

data class ProgramSource(
    val id: String,
    val source: String,
    val sha256: String,
    val role: ProgramRole,
    val format: ProgramFormat,
    val namespace: LogicalNamespace?,
    val expectedFor: CommandSelector?,
    val expectedError: ProgramExpectedError?,
)

data class ProgramBinding(
    val id: String,
    val name: String,
    val kind: ProgramBindingKind,
    val declaration: ProgramDeclaration,
    val source: ProgramLocation,
    val dependencies: List<String>,
)

/** Closed in-process hierarchy; no class-name/default-typing transport is accepted. */
sealed interface ShellOperation {
    val kind: ShellOperationKind
}

data class InsertOperation(
    val sourceMethod: String,
    val many: Boolean,
    val documents: List<BsonSnapshot>,
    val options: BsonSnapshot?,
) : ShellOperation {
    override val kind = ShellOperationKind.INSERT
}

data class IndexOperation(val keys: BsonSnapshot, val options: BsonSnapshot?) : ShellOperation {
    override val kind = ShellOperationKind.INDEX
}

data class QueryModifier(val kind: QueryModifierKind, val value: BsonSnapshot?, val number: Long?)

data class FindOperation(
    val filter: BsonSnapshot,
    val projection: BsonSnapshot?,
    val modifiers: List<QueryModifier>,
) : ShellOperation {
    override val kind = ShellOperationKind.FIND
}

data class AggregateOperation(val pipeline: BsonSnapshot, val options: BsonSnapshot?) :
    ShellOperation {
    override val kind = ShellOperationKind.AGGREGATE
}

data class UpdateOperation(
    val sourceMethod: String,
    val filter: BsonSnapshot,
    val update: BsonSnapshot,
    val options: BsonSnapshot?,
) : ShellOperation {
    override val kind = ShellOperationKind.UPDATE
}

data class ShellCommand(
    val id: CommandSelector,
    val role: ProgramRole,
    val namespace: LogicalNamespace,
    val operation: ShellOperation,
    val source: ProgramLocation,
    val dependencies: List<String>,
)

data class ProgramExpectation(
    val command: CommandSelector,
    val documents: List<BsonSnapshot>?,
    val error: ProgramExpectedError?,
    val order: ResultOrder,
    val source: ProgramLocation,
    val dependencies: List<String>,
    val published: List<PublishedDocument>? = null,
) {
    init {
        require(listOf(documents, error, published).count { it != null } == 1)
    }

    val publishedProfile: String? = if (published != null) PublishedReadProfile.ID else null
}

data class ProgramProblem(val code: String, val message: String, val source: ProgramLocation?)

data class ShellProgramResult(
    val schemaVersion: Int = 2,
    val state: ShellLiteralState,
    val parser: String,
    val dialect: ShellDialect,
    val sources: List<ProgramSource>,
    val bindings: List<ProgramBinding>,
    val commands: List<ShellCommand>,
    val expectations: List<ProgramExpectation>,
    /** Excludes source formatting, binding identifiers and other provenance-only metadata. */
    val semanticSha256: String?,
    val problems: List<ProgramProblem>,
) {
    init {
        require(schemaVersion == 2)
        require(
            (state == ShellLiteralState.NORMALIZED) ==
                (problems.isEmpty() && semanticSha256 != null)
        )
        require(
            state == ShellLiteralState.NORMALIZED || commands.isEmpty() && expectations.isEmpty()
        )
    }
}

data class ShellProgramLimits(
    val values: ShellLiteralLimits = ShellLiteralLimits(),
    val maximumBlocks: Int = 64,
    val maximumBindings: Int = 512,
    val maximumCommands: Int = 512,
    val maximumCharacters: Int = 1_048_576,
    val maximumSnapshotBytes: Int = 1_048_576,
    val maximumStoredValueBytes: Int = 8_388_608,
    val maximumArtifactBytes: Int = 67_108_864,
) {
    init {
        require(maximumBlocks in 1..256 && maximumBindings in 1..1024 && maximumCommands in 1..1024)
        require(maximumCharacters in 1..4_194_304 && maximumSnapshotBytes in 32..16_777_216)
        require(maximumStoredValueBytes in maximumSnapshotBytes..67_108_864)
        require(maximumArtifactBytes in 1024..134_217_728)
    }
}
