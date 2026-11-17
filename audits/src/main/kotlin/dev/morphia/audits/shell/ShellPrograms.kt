package dev.morphia.audits.shell

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.published.*
import dev.morphia.audits.semantics.BsonSnapshot
import dev.morphia.audits.semantics.BsonSnapshotLimit
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.HexFormat
import org.bson.*
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Parser
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.*

/**
 * Builds a canonical MQL command plan; it never executes a documentation program or database call.
 */
object ShellPrograms {
    const val PARSER = "rhino:1.9.1/ordered-program:3"

    fun normalize(
        request: ShellProgramRequest,
        limits: ShellProgramLimits = ShellProgramLimits(),
    ): ShellProgramResult {
        val result = Processor(request.copy(blocks = request.blocks.toList()), limits).run()
        AuditJson.requireMaximumSize(result, limits.maximumArtifactBytes)
        return result
    }

    private class Stop(
        val code: String,
        message: String,
        val location: ProgramLocation?,
        val invalid: Boolean = false,
    ) : RuntimeException(message)

    private data class Bound(
        val record: ProgramBinding,
        val mutable: Boolean,
        val originalDeclaration: ProgramDeclaration,
        val block: ProgramBlock,
        val expression: AstNode?,
        val captured: Map<String, Bound>,
        val database: Database?,
        val namespace: LogicalNamespace?,
    )

    private data class Database(val name: String?)

    private data class PendingExpectation(
        val block: ProgramBlock,
        val values: List<BsonSnapshot>?,
        val dependencies: List<String>,
        val published: List<PublishedDocument>? = null,
    )

    private data class CommandMeaning(
        val role: ProgramRole,
        val namespace: LogicalNamespace,
        val operation: ShellOperation,
    )

    private data class ExpectationMeaning(
        val command: Int,
        val documents: List<BsonSnapshot>?,
        val published: List<PublishedDocument>?,
        val publishedProfile: String?,
        val error: ProgramExpectedError?,
        val order: dev.morphia.audits.semantics.ResultOrder,
    )

    private data class Meaning(
        val dialect: ShellDialect,
        val commands: List<CommandMeaning>,
        val expectations: List<ExpectationMeaning>,
        val modelVersion: Int = 2,
    )

    private class Processor(val request: ShellProgramRequest, val limits: ShellProgramLimits) {
        val sources = mutableListOf<ProgramSource>()
        val records = mutableListOf<ProgramBinding>()
        val commands = mutableListOf<ShellCommand>()
        val pending = mutableListOf<PendingExpectation>()
        val environment = linkedMapOf<String, Bound>()
        val budget = ShellValueBudget(limits.values.maximumNodes)
        var storedBytes = 0L
        var block: ProgramBlock? = null
        var prefix = 0
        var commandOrdinal = 0

        fun run(): ShellProgramResult =
            try {
                validate()
                sources +=
                    request.blocks.map { unit ->
                        ProgramSource(
                            id = unit.id,
                            source = unit.source,
                            sha256 = sha(unit.source),
                            role = unit.role,
                            format = unit.format,
                            namespace = unit.namespace,
                            expectedFor = unit.expectedFor,
                            expectedError = unit.expectedError,
                        )
                    }
                for (unit in request.blocks) {
                    block = unit
                    prefix = 0
                    commandOrdinal = 0
                    when {
                        unit.role in setOf(ProgramRole.EXPECTED, ProgramRole.EXPECTED_ERROR) ->
                            expected(unit)
                        unit.format == ProgramFormat.LEGACY_DOCUMENTS -> legacy(unit)
                        else -> {
                            val root = parse(unit.source, false)
                            for (statement in root.statements) statement(statement)
                        }
                    }
                }
                if (commands.none { it.role == ProgramRole.ACTION })
                    stop("NO_ACTION", "No supported action was provided")
                val expectations = resolveExpectations()
                val meaning =
                    Meaning(
                        request.dialect,
                        commands.map { CommandMeaning(it.role, it.namespace, it.operation) },
                        expectations.map {
                            ExpectationMeaning(
                                commands.indexOfFirst { c -> c.id == it.command },
                                it.documents,
                                it.published,
                                it.publishedProfile,
                                it.error,
                                it.order,
                            )
                        },
                    )
                AuditJson.requireMaximumSize(meaning, limits.maximumArtifactBytes)
                ShellProgramResult(
                        state = ShellLiteralState.NORMALIZED,
                        parser = PARSER,
                        dialect = request.dialect,
                        sources = sources.toList(),
                        bindings = records.toList(),
                        commands = commands.toList(),
                        expectations = expectations,
                        semanticSha256 = AuditJson.hash(meaning),
                        problems = emptyList(),
                    )
                    .also { AuditJson.requireMaximumSize(it, limits.maximumArtifactBytes) }
            } catch (failure: AuditJson.ArtifactSizeLimit) {
                failed(
                    "ARTIFACT_LIMIT",
                    failure.message ?: "Program exceeds artifact size limit",
                    block?.let { ProgramLocation(it.id, null) },
                    false,
                )
            } catch (failure: Stop) {
                failed(
                    failure.code,
                    failure.message ?: failure.code,
                    failure.location,
                    failure.invalid,
                )
            } catch (failure: ShellLiterals.UnsupportedLiteral) {
                failed(
                    failure.code,
                    failure.message ?: failure.code,
                    ProgramLocation(current().id, failure.span),
                    false,
                )
            } catch (failure: EvaluatorException) {
                failed(
                    "SYNTAX",
                    failure.message ?: "Invalid program syntax",
                    block?.let { ProgramLocation(it.id, null) },
                    false,
                )
            } catch (failure: BsonSnapshotLimit) {
                failed(
                    "SNAPSHOT_LIMIT",
                    failure.message ?: "BSON encoding limit",
                    block?.let { ProgramLocation(it.id, null) },
                    false,
                )
            }

        fun failed(code: String, message: String, location: ProgramLocation?, invalid: Boolean) =
            ShellProgramResult(
                state = if (invalid) ShellLiteralState.INVALID else ShellLiteralState.UNSUPPORTED,
                parser = PARSER,
                dialect = request.dialect,
                sources = sources.toList(),
                bindings = records.toList(),
                commands = emptyList(),
                expectations = emptyList(),
                semanticSha256 = null,
                problems = listOf(ProgramProblem(code, message, location)),
            )

        fun current() = requireNotNull(block)

        fun location(node: AstNode? = null) =
            block?.let {
                ProgramLocation(
                    it.id,
                    node?.let { n ->
                        ShellValueLowering(
                                it.source,
                                request.dialect,
                                limits.values,
                                prefix = prefix,
                            )
                            .span(n)
                    },
                )
            }

        fun stop(
            code: String,
            message: String,
            node: AstNode? = null,
            invalid: Boolean = false,
        ): Nothing = throw Stop(code, message, location(node), invalid)

        fun take(node: AstNode, depth: Int = 0) {
            if (!budget.take() || depth > limits.values.maximumDepth)
                stop("VALUE_LIMIT", "Program exceeds AST work/depth limit", node)
        }

        fun sha(source: String): String {
            val bytes =
                Charsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(source))
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            return HexFormat.of().formatHex(digest.digest())
        }

        fun validate() {
            if (
                request.schemaVersion != 1 ||
                    request.blocks.isEmpty() ||
                    request.blocks.size > limits.maximumBlocks
            )
                stop("INVALID_REQUEST", "Invalid program schema or block count", invalid = true)
            if (request.blocks.map { it.id }.distinct().size != request.blocks.size)
                stop("DUPLICATE_BLOCK", "Program block identities must be unique", invalid = true)
            if (request.blocks.sumOf { it.source.length.toLong() } > limits.maximumCharacters)
                stop("SOURCE_LIMIT", "Program source exceeds character limit", invalid = true)
            for (unit in request.blocks) {
                block = unit
                if (
                    unit.id.isBlank() ||
                        unit.id.length > 512 ||
                        '\u0000' in unit.id ||
                        unit.source.length > limits.values.maximumCharacters
                )
                    stop(
                        "INVALID_SOURCE",
                        "Invalid source identity or character count",
                        invalid = true,
                    )
                try {
                    sha(unit.source)
                    sha(unit.id)
                } catch (failure: CharacterCodingException) {
                    stop("SOURCE_ENCODING", "Source identity must be valid UTF-8", invalid = true)
                }
                if (
                    unit.role !in setOf(ProgramRole.EXPECTED, ProgramRole.EXPECTED_ERROR) &&
                        unit.expectedFor != null
                )
                    stop(
                        "INVALID_EXPECTATION",
                        "Only expected blocks can declare an action owner",
                        invalid = true,
                    )
                if (
                    (unit.role == ProgramRole.EXPECTED_ERROR) != (unit.expectedError != null) ||
                        (unit.format == ProgramFormat.TEXT) !=
                            (unit.role == ProgramRole.EXPECTED_ERROR)
                )
                    stop(
                        "EXPECTED_ERROR_FORMAT",
                        "Error blocks need explicit text/error metadata",
                        invalid = true,
                    )
                if (unit.format == ProgramFormat.SHELL_RESULTS && unit.role != ProgramRole.EXPECTED)
                    stop(
                        "RESULT_FORMAT",
                        "Printed shell results are expected-output only",
                        invalid = true,
                    )
                unit.namespace?.let { namespace ->
                    if (
                        !validCollection(namespace.collection) ||
                            namespace.database?.let { !validDatabase(it) } == true
                    )
                        stop(
                            "NAMESPACE_NAME",
                            "Invalid declared database or collection name",
                            invalid = true,
                        )
                    if (
                        namespace.collection.length > 512 || (namespace.database?.length ?: 0) > 512
                    )
                        stop(
                            "NAMESPACE_LIMIT",
                            "Logical namespace exceeds resource bound",
                            invalid = true,
                        )
                    try {
                        sha(namespace.collection)
                        namespace.database?.let { sha(it) }
                    } catch (failure: CharacterCodingException) {
                        stop(
                            "NAMESPACE_ENCODING",
                            "Namespace must have valid UTF-8 identity",
                            invalid = true,
                        )
                    }
                }
                if (
                    unit.namespace != null &&
                        (unit.format != ProgramFormat.LEGACY_DOCUMENTS ||
                            unit.role == ProgramRole.EXPECTED)
                )
                    stop(
                        "INVALID_NAMESPACE",
                        "Only declared legacy setup/action fragments take a namespace",
                        invalid = true,
                    )
            }
        }

        fun parse(source: String, value: Boolean): AstRoot {
            val options =
                CompilerEnvirons().apply {
                    languageVersion = Context.VERSION_ECMASCRIPT
                    isXmlAvailable = false
                    setIdeMode(false)
                    setRecoverFromErrors(false)
                }
            return Parser(options)
                .parse(if (value) "(" + source + "\n)" else source, current().id, 1)
        }

        fun evaluator(
            unit: ProgramBlock,
            offset: Int,
            scope: Map<String, Bound>,
            dependencies: MutableSet<String>,
            offsetsReliable: Boolean = true,
        ): ShellValueLowering {
            val bindings =
                object : ShellValueBindings {
                    override fun contains(name: String) = name in scope

                    override fun published(
                        name: String,
                        depth: Int,
                        reference: ShellSpan?,
                    ): PublishedValue? {
                        val binding = scope[name] ?: return null
                        if (binding.record.kind != ProgramBindingKind.VALUE)
                            throw ShellLiterals.UnsupportedLiteral(
                                "BINDING_TYPE",
                                "$name is a namespace, not a value",
                                reference,
                            )
                        dependencies += binding.record.id
                        return try {
                            binding.expression?.let {
                                evaluator(binding.block, 0, binding.captured, dependencies)
                                    .published(it, depth + 1, ::snapshot)
                            }
                                ?: throw ShellLiterals.UnsupportedLiteral(
                                    "EXPECTED_TYPE_AMBIGUOUS",
                                    "Uninitialized binding has no proven stored BSON type",
                                    reference,
                                )
                        } catch (failure: ShellLiterals.UnsupportedLiteral) {
                            throw ShellLiterals.UnsupportedLiteral(
                                failure.code,
                                "Binding $name: ${failure.message}",
                                reference,
                            )
                        }
                    }

                    override fun resolve(
                        name: String,
                        use: ShellValueUse,
                        depth: Int,
                        reference: ShellSpan?,
                    ): BsonValue? {
                        val binding = scope[name] ?: return null
                        if (binding.record.kind != ProgramBindingKind.VALUE)
                            throw ShellLiterals.UnsupportedLiteral(
                                "BINDING_TYPE",
                                "$name is a namespace, not a value",
                                reference,
                            )
                        dependencies += binding.record.id
                        return try {
                            binding.expression?.let {
                                evaluator(binding.block, 0, binding.captured, dependencies)
                                    .value(it, depth + 1, use)
                            }
                                ?: when {
                                    use == ShellValueUse.EXPECTED ->
                                        throw ShellLiterals.UnsupportedLiteral(
                                            "EXPECTED_TYPE_AMBIGUOUS",
                                            "Uninitialized binding has no proven stored BSON type",
                                            reference,
                                        )
                                    use != ShellValueUse.STORAGE -> BsonUndefined()
                                    request.dialect == ShellDialect.MONGOSH -> BsonNull.VALUE
                                    request.dialect == ShellDialect.LEGACY_MONGO -> BsonUndefined()
                                    else ->
                                        throw ShellLiterals.UnsupportedLiteral(
                                            "SHELL_DIALECT_REQUIRED",
                                            "Undefined storage needs a dialect",
                                            reference,
                                        )
                                }
                        } catch (failure: ShellLiterals.UnsupportedLiteral) {
                            throw ShellLiterals.UnsupportedLiteral(
                                failure.code,
                                "Binding $name: ${failure.message}",
                                reference,
                            )
                        }
                    }
                }
            return ShellValueLowering(
                unit.source,
                request.dialect,
                limits.values,
                spanOffsetsReliable = offsetsReliable,
                prefix = offset,
                bindings = bindings,
                budget = budget,
            )
        }

        fun value(
            node: AstNode,
            dependencies: MutableSet<String>,
            use: ShellValueUse = ShellValueUse.STORAGE,
            scope: Map<String, Bound> = environment,
        ): BsonValue = evaluator(current(), prefix, scope, dependencies).value(node, 0, use)

        fun snapshot(value: BsonValue): BsonSnapshot {
            val result =
                try {
                    BsonSnapshot.of(value, limits.maximumSnapshotBytes)
                } catch (failure: IllegalArgumentException) {
                    stop("BSON_ENCODING", failure.message ?: "Value is not losslessly encodable")
                }
            storedBytes += result.byteLength
            if (storedBytes > limits.maximumStoredValueBytes)
                stop("STORED_VALUE_LIMIT", "Program exceeds stored value budget")
            return result
        }

        fun storePublished(value: PublishedValue) {
            val remaining = (limits.maximumStoredValueBytes - storedBytes).toInt()
            if (remaining <= 0) stop("STORED_VALUE_LIMIT", "Program exceeds stored value budget")
            try {
                // Count the entire root once, including JSON escaping, keys and type metadata.
                // Snapshot bytes already retained by exact leaves share this conservative budget.
                storedBytes += AuditJson.requireMaximumSize(value, remaining)
            } catch (failure: AuditJson.ArtifactSizeLimit) {
                stop("STORED_VALUE_LIMIT", "Program exceeds stored value budget")
            }
        }

        fun document(node: AstNode, dependencies: MutableSet<String>): BsonDocument =
            (value(node, dependencies) as? BsonDocument)
                ?: stop("DOCUMENT_REQUIRED", "Expected a document", node)

        fun string(node: AstNode, dependencies: MutableSet<String>): String =
            (value(node, dependencies, ShellValueUse.ARGUMENT) as? BsonString)?.value
                ?: stop("STRING_REQUIRED", "Expected a pure string", node)

        fun integer(
            node: AstNode,
            dependencies: MutableSet<String>,
            minimum: Long = Int.MIN_VALUE.toLong(),
        ): Long {
            val v =
                (value(node, dependencies, ShellValueUse.ARGUMENT) as? BsonDouble)?.value
                    ?: stop("INTEGER_REQUIRED", "Expected a primitive integer", node)
            if (!v.isFinite() || v != kotlin.math.floor(v) || v < minimum || v > Int.MAX_VALUE)
                stop("INTEGER_RANGE", "Integer option is outside its supported range", node)
            return v.toLong()
        }

        // Pinned mongosh helpers.ts predicates, with the normalizer's existing blank-name guard.
        fun validCollection(name: String) =
            name.isNotBlank() && name.none { it == '$' || it == '\u0000' }

        fun validDatabase(name: String) = name.isNotBlank() && name.none { it in "/\\. \"\u0000" }

        fun namespace(database: Database, name: String, node: AstNode): LogicalNamespace {
            if (!validCollection(name) || name.length > 512)
                stop("NAMESPACE_NAME", "Invalid collection name", node)
            return LogicalNamespace(database.name, name)
        }

        fun database(
            node: AstNode,
            dependencies: MutableSet<String>,
            scope: Map<String, Bound> = environment,
            depth: Int = 0,
        ): Database? {
            take(node, depth)
            if (node is ParenthesizedExpression)
                return database(node.expression, dependencies, scope, depth + 1)
            if (node is Name) {
                if (node.identifier == "db") return Database(null)
                val bound = scope[node.identifier] ?: return null
                if (bound.record.kind == ProgramBindingKind.DATABASE) {
                    dependencies += bound.record.id
                    return bound.database
                }
                return null
            }
            if (node is FunctionCall && node.target is PropertyGet) {
                val member = node.target as PropertyGet
                if (member.property.identifier != "getSiblingDB") return null
                val owner = database(member.target, dependencies, scope, depth + 1) ?: return null
                arity(node, 1, 1)
                val name =
                    (evaluator(current(), prefix, scope, dependencies)
                            .value(node.arguments[0], 0, ShellValueUse.ARGUMENT) as? BsonString)
                        ?.value
                        ?: stop("NAMESPACE_NAME", "Database name must be a pure string", node)
                if (!validDatabase(name) || name.length > 512)
                    stop("NAMESPACE_NAME", "Invalid database name", node)
                return Database(name)
            }
            return null
        }

        fun collection(
            node: AstNode,
            dependencies: MutableSet<String>,
            scope: Map<String, Bound> = environment,
            depth: Int = 0,
        ): LogicalNamespace? {
            take(node, depth)
            if (node is ParenthesizedExpression)
                return collection(node.expression, dependencies, scope, depth + 1)
            if (node is Name) {
                val bound = scope[node.identifier] ?: return null
                if (bound.record.kind == ProgramBindingKind.COLLECTION) {
                    dependencies += bound.record.id
                    return bound.namespace
                }
                return null
            }
            if (node is PropertyGet) {
                val owner = database(node.target, dependencies, scope, depth + 1) ?: return null
                val name = node.property.identifier
                reservedCollectionProperty(name, node)
                return namespace(owner, name, node)
            }
            if (node is ElementGet) {
                val owner = database(node.target, dependencies, scope, depth + 1) ?: return null
                val name =
                    (evaluator(current(), prefix, scope, dependencies)
                            .value(node.element, 0, ShellValueUse.ARGUMENT) as? BsonString)
                        ?.value
                        ?: stop("NAMESPACE_NAME", "Collection property must be a pure string", node)
                reservedCollectionProperty(name, node)
                return namespace(owner, name, node)
            }
            if (node is FunctionCall && node.target is PropertyGet) {
                val member = node.target as PropertyGet
                if (member.property.identifier != "getCollection") return null
                val owner = database(member.target, dependencies, scope, depth + 1) ?: return null
                arity(node, 1, 1)
                val name =
                    (evaluator(current(), prefix, scope, dependencies)
                            .value(node.arguments[0], 0, ShellValueUse.ARGUMENT) as? BsonString)
                        ?.value
                        ?: stop("NAMESPACE_NAME", "Collection name must be a pure string", node)
                return namespace(owner, name, node)
            }
            return null
        }

        fun reservedCollectionProperty(name: String, node: AstNode) {
            if (name.startsWith('_') || name in databaseMembers)
                stop(
                    "DATABASE_PROPERTY",
                    "Use getCollection for a reserved database property: $name",
                    node,
                )
        }

        fun statement(node: AstNode) {
            take(node)
            when (node) {
                is EmptyStatement -> Unit
                is VariableDeclaration -> {
                    val declaration =
                        when (node.type) {
                            Token.CONST -> ProgramDeclaration.CONST
                            Token.LET -> ProgramDeclaration.LET
                            else -> ProgramDeclaration.VAR
                        }
                    for (variable in node.variables) {
                        val name =
                            (variable.target as? Name)?.identifier
                                ?: stop("BINDING_TARGET", "Destructuring is unsupported", variable)
                        bind(name, variable.initializer, declaration, variable)
                    }
                }
                is ExpressionStatement -> {
                    val expression = node.expression
                    if (expression is Assignment) {
                        if (expression.operator != Token.ASSIGN || expression.left !is Name)
                            stop(
                                "ASSIGNMENT_TARGET",
                                "Only direct pure binding assignment is supported",
                                expression,
                            )
                        bind(
                            (expression.left as Name).identifier,
                            expression.right,
                            ProgramDeclaration.ASSIGNMENT,
                            expression,
                        )
                    } else add(command(expression, linkedSetOf()), expression)
                }
                else ->
                    stop(
                        "PROGRAM_STATEMENT",
                        "Unsupported statement: ${node.javaClass.simpleName}",
                        node,
                    )
            }
        }

        fun bind(
            name: String,
            expression: AstNode?,
            declaration: ProgramDeclaration,
            node: AstNode,
        ) {
            if (records.size >= limits.maximumBindings)
                stop("BINDING_LIMIT", "Too many binding events", node)
            if (name in reservedBindings)
                stop("RESERVED_BINDING", "Builtin binding cannot be shadowed: $name", node)
            val previous = environment[name]
            if (
                declaration == ProgramDeclaration.ASSIGNMENT &&
                    (previous == null || !previous.mutable)
            )
                stop("ASSIGNMENT_BINDING", "Assignment needs an existing mutable binding", node)
            if (declaration != ProgramDeclaration.ASSIGNMENT && previous != null) {
                if (
                    declaration != ProgramDeclaration.VAR ||
                        previous.originalDeclaration != ProgramDeclaration.VAR
                )
                    stop("DUPLICATE_BINDING", "Lexical binding redeclaration is unsupported", node)
                if (expression == null) return
            }
            val captured = environment.toMap()
            val dependencies = linkedSetOf<String>()
            val db = expression?.let { database(it, dependencies, captured) }
            val coll =
                if (db == null) expression?.let { collection(it, dependencies, captured) } else null
            if (db == null && coll == null && expression != null)
                value(expression, dependencies, ShellValueUse.INITIALIZER, captured)
            val kind =
                when {
                    db != null -> ProgramBindingKind.DATABASE
                    coll != null -> ProgramBindingKind.COLLECTION
                    else -> ProgramBindingKind.VALUE
                }
            val record =
                ProgramBinding(
                    "binding/" +
                        AuditJson.hash(listOf(current().id, records.size.toString(), name)),
                    name,
                    kind,
                    declaration,
                    requireNotNull(location(node)),
                    dependencies.toList(),
                )
            records += record
            environment[name] =
                Bound(
                    record,
                    previous?.mutable ?: (declaration != ProgramDeclaration.CONST),
                    previous?.originalDeclaration ?: declaration,
                    current(),
                    expression,
                    captured,
                    db,
                    coll,
                )
        }

        fun arity(call: FunctionCall, min: Int, max: Int) {
            if (call.isOptionalCall || call is NewExpression || call.arguments.size !in min..max)
                stop("CALL_FORM", "Unsupported call form or argument count", call)
        }

        fun pipeline(value: BsonValue, node: AstNode): BsonArray {
            val array =
                value as? BsonArray
                    ?: stop("PIPELINE_REQUIRED", "Expected an ordered pipeline array", node)
            if (
                array.any {
                    it !is BsonDocument || it.size != 1 || !it.keys.single().startsWith('$')
                }
            )
                stop("PIPELINE_STAGE", "Each pipeline stage must have one operator field", node)
            return array
        }

        data class Planned(
            val namespace: LogicalNamespace,
            val operation: ShellOperation,
            val dependencies: List<String>,
            val materialized: Boolean = false,
        )

        fun command(node: AstNode, dependencies: MutableSet<String>, depth: Int = 0): Planned {
            take(node, depth)
            val call =
                node as? FunctionCall
                    ?: stop("ACTION_REQUIRED", "Expected an explicit MQL command", node)
            val target =
                call.target as? PropertyGet
                    ?: stop("COMMAND_TARGET", "Only collection commands are supported", node)
            val method = target.property.identifier
            if (method in setOf("pretty", "toArray")) {
                arity(call, 0, 0)
                val previous = command(target.target, dependencies, depth + 1)
                if (
                    previous.operation !is FindOperation &&
                        previous.operation !is AggregateOperation
                )
                    stop(
                        "CURSOR_REQUIRED",
                        "Materialization requires a query or aggregate cursor",
                        call,
                    )
                if (previous.materialized)
                    stop("CURSOR_MATERIALIZED", "A materialized result is not a cursor", call)
                return previous.copy(materialized = method == "toArray")
            }
            val modifierKind = modifiers[method]
            if (modifierKind != null) {
                arity(call, 1, 1)
                val previous = command(target.target, dependencies, depth + 1)
                if (previous.materialized)
                    stop("CURSOR_MATERIALIZED", "A materialized result cannot be configured", call)
                val find =
                    previous.operation as? FindOperation
                        ?: stop("CURSOR_REQUIRED", "Modifier needs a find cursor", call)
                val modifier =
                    if (modifierKind in numericModifiers)
                        QueryModifier(
                            modifierKind,
                            null,
                            integer(
                                call.arguments[0],
                                dependencies,
                                if (modifierKind == QueryModifierKind.LIMIT) Int.MIN_VALUE.toLong()
                                else 0,
                            ),
                        )
                    else {
                        val v = value(call.arguments[0], dependencies)
                        if (
                            v !is BsonDocument &&
                                !(modifierKind == QueryModifierKind.HINT && v is BsonString)
                        )
                            stop(
                                "MODIFIER_TYPE",
                                "Cursor option requires a document or named hint",
                                call,
                            )
                        QueryModifier(modifierKind, snapshot(v), null)
                    }
                return previous.copy(
                    operation = find.copy(modifiers = find.modifiers + modifier),
                    dependencies = dependencies.toList(),
                )
            }
            val namespace =
                collection(target.target, dependencies)
                    ?: stop(
                        "COLLECTION_REQUIRED",
                        "Command receiver is not a known collection",
                        target.target,
                    )
            fun option(index: Int) =
                call.arguments.getOrNull(index)?.let { snapshot(document(it, dependencies)) }
            val operation: ShellOperation =
                when (method) {
                    "find" -> {
                        arity(call, 0, 2)
                        FindOperation(
                            snapshot(
                                call.arguments.firstOrNull()?.let { document(it, dependencies) }
                                    ?: BsonDocument()
                            ),
                            option(1),
                            emptyList(),
                        )
                    }
                    "aggregate" -> {
                        arity(call, 1, 2)
                        val options =
                            call.arguments.getOrNull(1)?.let { document(it, dependencies) }
                        if (options?.containsKey("explain") == true)
                            stop(
                                "AGGREGATE_EXPLAIN",
                                "Explain output requires a separate command model",
                                call,
                            )
                        AggregateOperation(
                            snapshot(
                                pipeline(value(call.arguments[0], dependencies), call.arguments[0])
                            ),
                            options?.let { snapshot(it) },
                        )
                    }
                    "insertOne",
                    "insertMany",
                    "insert" -> {
                        arity(call, 1, 2)
                        val input = value(call.arguments[0], dependencies)
                        val docs =
                            if (input is BsonDocument && method != "insertMany") listOf(input)
                            else if (
                                input is BsonArray &&
                                    method != "insertOne" &&
                                    input.all { it is BsonDocument }
                            )
                                input.map { it.asDocument() }
                            else
                                stop(
                                    "INSERT_DOCUMENTS",
                                    "Insert requires one document or a document array",
                                    call,
                                )
                        if (docs.isEmpty())
                            stop("INSERT_DOCUMENTS", "Empty insert batches are not supported", call)
                        InsertOperation(
                            method,
                            input is BsonArray,
                            docs.map { snapshot(it) },
                            option(1),
                        )
                    }
                    "createIndex" -> {
                        arity(call, 1, 2)
                        val keys = document(call.arguments[0], dependencies)
                        if (keys.isEmpty()) stop("INDEX_KEYS", "Index keys cannot be empty", call)
                        IndexOperation(snapshot(keys), option(1))
                    }
                    "updateOne",
                    "updateMany",
                    "update",
                    "replaceOne" -> {
                        arity(call, 2, 3)
                        val filter = document(call.arguments[0], dependencies)
                        val update = value(call.arguments[1], dependencies)
                        if (update !is BsonDocument) {
                            if (method == "replaceOne")
                                stop("REPLACEMENT_DOCUMENT", "Replacement must be a document", call)
                            pipeline(update, call.arguments[1])
                        }
                        UpdateOperation(method, snapshot(filter), snapshot(update), option(2))
                    }
                    else ->
                        stop(
                            "COMMAND_METHOD",
                            "Unrecognized or unsupported collection command: $method",
                            call,
                        )
                }
            return Planned(namespace, operation, dependencies.toList())
        }

        fun add(planned: Planned, node: AstNode? = null) {
            if (commands.size >= limits.maximumCommands)
                stop("COMMAND_LIMIT", "Too many commands", node)
            commands +=
                ShellCommand(
                    CommandSelector(current().id, commandOrdinal++),
                    current().role,
                    planned.namespace,
                    planned.operation,
                    requireNotNull(location(node)),
                    planned.dependencies,
                )
        }

        fun legacy(unit: ProgramBlock) {
            val namespace =
                unit.namespace
                    ?: stop("FRAGMENT_NAMESPACE", "Legacy setup/action needs an explicit namespace")
            val parsed = LegacyDocuments.parse(unit.source, limits.values)
            if (parsed.state != ShellLiteralState.NORMALIZED)
                stop(
                    "LEGACY_DOCUMENTS",
                    parsed.problems.joinToString { "${it.code}: ${it.message}" },
                )
            val op =
                if (unit.role == ProgramRole.SETUP)
                    InsertOperation("seed", true, parsed.documents.map { snapshot(it) }, null)
                else
                    AggregateOperation(
                        snapshot(pipeline(BsonArray(parsed.documents), EmptyExpression())),
                        null,
                    )
            add(Planned(namespace, op, emptyList()))
        }

        fun expected(unit: ProgramBlock) {
            if (unit.role == ProgramRole.EXPECTED_ERROR) {
                val error = requireNotNull(unit.expectedError)
                if (
                    error.code?.let {
                        !Regex("(?<![0-9])" + it + "(?![0-9])").containsMatchIn(unit.source)
                    } == true ||
                        error.codeName?.let {
                            !Regex("(?<![A-Za-z0-9_])" + it + "(?![A-Za-z0-9_])")
                                .containsMatchIn(unit.source)
                        } == true ||
                        error.message?.let { it !in unit.source } == true
                )
                    stop(
                        "EXPECTED_ERROR_EVIDENCE",
                        "Declared error assertion is absent from its source block",
                    )
                pending += PendingExpectation(unit, null, emptyList())
                return
            }
            val dependencies = linkedSetOf<String>()
            if (unit.format == ProgramFormat.LEGACY_DOCUMENTS) {
                val parsed = LegacyDocuments.parse(unit.source, limits.values)
                if (parsed.state != ShellLiteralState.NORMALIZED)
                    stop("EXPECTED_DOCUMENTS", parsed.problems.joinToString { it.code })
                pending +=
                    PendingExpectation(
                        unit,
                        parsed.documents.map { snapshot(it) },
                        dependencies.toList(),
                    )
            } else {
                val fragments =
                    if (unit.format == ProgramFormat.SHELL_RESULTS)
                        ShellResultFraming.frames(unit.source, limits.values, budget)
                    else listOf(ShellSpan(0, unit.source.length))
                val docs = mutableListOf<PublishedDocument>()
                for (fragment in fragments) {
                    prefix = 1 - fragment.start
                    val fragmentSource = unit.source.substring(fragment.start, fragment.end)
                    val root = parse(fragmentSource, true)
                    val wrapper =
                        (root.statements.singleOrNull() as? ExpressionStatement)?.expression
                            as? ParenthesizedExpression
                            ?: stop("EXPECTED_VALUE", "Expected one result value")
                    val v =
                        evaluator(
                                unit,
                                prefix,
                                environment,
                                dependencies,
                                offsetsReliable =
                                    unit.format != ProgramFormat.SHELL_RESULTS ||
                                        fragmentSource.none { Character.isSurrogate(it) },
                            )
                            .published(wrapper.expression, 0, ::snapshot)
                    when (v) {
                        is PublishedDocument -> docs += v
                        is PublishedArray ->
                            docs +=
                                v.items.map {
                                    it as? PublishedDocument
                                        ?: stop(
                                            "EXPECTED_DOCUMENTS",
                                            "Expected results must contain documents",
                                        )
                                }
                        else -> stop("EXPECTED_DOCUMENTS", "Expected a document or document array")
                    }
                    storePublished(v)
                }

                pending += PendingExpectation(unit, null, dependencies.toList(), docs)
            }
        }

        fun resolveExpectations(): List<ProgramExpectation> {
            val action = commands.filter { it.role == ProgramRole.ACTION }
            val default = action.singleOrNull()
            val result =
                pending.map { pending ->
                    block = pending.block
                    val selected =
                        pending.block.expectedFor?.let { selector ->
                            action.find { it.id == selector }
                        } ?: if (pending.block.expectedFor == null) default else null
                    if (selected == null)
                        stop(
                            "EXPECTED_OWNER",
                            "Expected output needs one unambiguous explicit action owner",
                        )
                    if (pending.published != null) {
                        when (val operation = selected.operation) {
                            is FindOperation -> Unit
                            is AggregateOperation -> {
                                val options = operation.options?.value()?.asDocument()
                                if (options != null && options.keys.any { it in decodingOptions })
                                    stop(
                                        "PUBLISHED_PROFILE",
                                        "Source overrides the pinned mongosh BSON decoding profile",
                                    )
                            }
                            else ->
                                stop(
                                    "PUBLISHED_PROFILE",
                                    "Published mongosh expectations require a find or aggregate read result",
                                )
                        }
                    }
                    ProgramExpectation(
                        command = selected.id,
                        documents = pending.values,
                        published = pending.published,
                        error = pending.block.expectedError,
                        order = request.resultOrder,
                        source = ProgramLocation(pending.block.id, null),
                        dependencies = pending.dependencies,
                    )
                }
            if (result.map { it.command }.distinct().size != result.size)
                stop(
                    "DUPLICATE_EXPECTATION",
                    "Multiple output blocks need an explicit grouped expectation",
                )
            return result
        }
    }

    private val decodingOptions =
        setOf(
            "promoteLongs",
            "promoteValues",
            "promoteBuffers",
            "useBigInt64",
            "bsonRegExp",
            "raw",
            "fieldsAsRaw",
            "enableUtf8Validation",
        )

    private val modifiers =
        mapOf(
            "projection" to QueryModifierKind.PROJECT,
            "sort" to QueryModifierKind.SORT,
            "limit" to QueryModifierKind.LIMIT,
            "skip" to QueryModifierKind.SKIP,
            "hint" to QueryModifierKind.HINT,
            "collation" to QueryModifierKind.COLLATION,
            "batchSize" to QueryModifierKind.BATCH_SIZE,
            "maxTimeMS" to QueryModifierKind.MAX_TIME_MS,
        )
    private val numericModifiers =
        setOf(
            QueryModifierKind.LIMIT,
            QueryModifierKind.SKIP,
            QueryModifierKind.BATCH_SIZE,
            QueryModifierKind.MAX_TIME_MS,
        )
    private val reservedBindings =
        setOf(
            "db",
            "NaN",
            "Infinity",
            "undefined",
            "NumberInt",
            "Int32",
            "NumberLong",
            "Long",
            "NumberDecimal",
            "Decimal128",
            "Double",
            "ObjectId",
            "ISODate",
            "Date",
            "Timestamp",
            "BinData",
            "HexData",
            "UUID",
            "RegExp",
            "BSONRegExp",
        )
    // Public database methods from mongosh bd8133d2, database.ts blob 83815dda.
    // Underscore-prefixed internal members are rejected separately; inherited names stay reserved.
    private val databaseMembers =
        setOf(
            "adminCommand",
            "aggregate",
            "auth",
            "changeUserPassword",
            "checkMetadataConsistency",
            "cloneCollection",
            "cloneDatabase",
            "commandHelp",
            "constructor",
            "copyDatabase",
            "createCollection",
            "createEncryptedCollection",
            "createRole",
            "createUser",
            "createView",
            "currentOp",
            "dropAllRoles",
            "dropAllUsers",
            "dropDatabase",
            "dropRole",
            "dropUser",
            "fsyncLock",
            "fsyncUnlock",
            "getCollection",
            "getCollectionInfos",
            "getCollectionNames",
            "getLastError",
            "getLastErrorObj",
            "getLogComponents",
            "getMongo",
            "getName",
            "getProfilingLevel",
            "getProfilingStatus",
            "getReplicationInfo",
            "getRole",
            "getRoles",
            "getSiblingDB",
            "getUser",
            "getUsers",
            "grantPrivilegesToRole",
            "grantRolesToRole",
            "grantRolesToUser",
            "hasOwnProperty",
            "hello",
            "help",
            "hostInfo",
            "isMaster",
            "isPrototypeOf",
            "killOp",
            "listCommands",
            "logout",
            "printCollectionStats",
            "printReplicationInfo",
            "printSecondaryReplicationInfo",
            "printShardingStatus",
            "printSlaveReplicationInfo",
            "propertyIsEnumerable",
            "prototype",
            "revokePrivilegesFromRole",
            "revokeRolesFromRole",
            "revokeRolesFromUser",
            "rotateCertificates",
            "runCommand",
            "serverBits",
            "serverBuildInfo",
            "serverCmdLineOpts",
            "serverStatus",
            "setLogLevel",
            "setProfilingLevel",
            "setSecondaryOk",
            "shutdownServer",
            "sql",
            "stats",
            "then",
            "toLocaleString",
            "toString",
            "updateRole",
            "updateUser",
            "valueOf",
            "version",
            "watch",
        )
}
