package dev.morphia.audits.scenarios

import dev.morphia.audits.shell.*

/**
 * A reviewed commands-then-result layout plus closed parsing establishes roles, never proximity
 * alone. Each block is classified by what the shell grammar makes of it in the context of the
 * blocks accepted before it: setup (inserts, indexes and declarations), an action, the published
 * result directly after an action, a sample document, or source the program skips (a syntax
 * template, a second command in one block), recorded with its reason.
 *
 * A sample document seeds the collection of the action that follows it only where nothing the
 * scenario runs inserts data; otherwise it illustrates and is skipped.
 */
internal object SectionPrograms {
    private val setupKinds = setOf(ShellOperationKind.INSERT, ShellOperationKind.INDEX)
    // Syntax templates name their operands in angle brackets: `{ $add: [ <expression1>, ... ] }`.
    private val placeholder = Regex("<[A-Za-z][^<>\n]*>")
    private val quoted = Regex("""'(?:[^'\\\n]|\\.)*'|"(?:[^"\\\n]|\\.)*"""")

    // Only source the grammar rejects is a template, and a quoted `'<value>'` is a string.
    private fun template(source: String) = placeholder.containsMatchIn(source.replace(quoted, ""))

    data class Assignment(
        val blocks: List<ScenarioCode>,
        val problem: String?,
        val skipped: List<SkippedCode> = emptyList(),
    )

    private sealed interface Kind {
        data class Setup(val inserts: Boolean) : Kind

        /**
         * [update] is an update command; [writes] also covers a pipeline that writes its output.
         */
        data class Action(
            val update: Boolean,
            val namespace: LogicalNamespace,
            val writes: Boolean = update,
        ) : Kind

        data object Document : Kind

        data class Skip(val reason: String) : Kind
    }

    /**
     * The setup an examples section shares; its commands and anything unreadable are skipped, with
     * reasons. Its documents are candidate seeds; [resolveSeeds] decides per scenario.
     */
    fun shared(blocks: List<ScenarioCode>, dialect: ShellDialect): Assignment {
        if (blocks.isEmpty()) return Assignment(blocks, "EMPTY_SHARED_SETUP")
        val kinds = classify(emptyList(), blocks, dialect)
        val setup =
            blocks.mapIndexedNotNull { i, block ->
                when (kinds[i]) {
                    is Kind.Setup -> block.copy(role = ProgramRole.SETUP)
                    Kind.Document -> seed(block)
                    else -> null
                }
            }
        val skipped =
            blocks.mapIndexedNotNull { i, block ->
                when (val kind = kinds[i]) {
                    is Kind.Skip -> SkippedCode(block, kind.reason)
                    is Kind.Action -> SkippedCode(block, "SHARED_SECTION_COMMAND")
                    else -> null
                }
            }
        // Shared data the grammar can't read leaves every consumer without it.
        return Assignment(
            setup,
            if (kinds.any { it == Kind.Skip("UNSUPPORTED_SETUP") }) "UNSUPPORTED_SHARED_SETUP"
            else null,
            skipped,
        )
    }

    /**
     * One assignment per action the section owns. An action's result is the document-shaped block
     * directly after it; without one the scenario has no published result. The documents before an
     * action are its candidate seeds.
     */
    fun assign(
        shared: List<ScenarioCode>,
        own: List<ScenarioCode>,
        dialect: ShellDialect,
    ): List<Assignment> {
        val kinds = classify(shared.filter { it.format == null }, own, dialect)
        val actions = own.indices.filter { kinds[it] is Kind.Action }
        // What a page prints after an update is the read it runs next, not the update's result.
        fun updated(i: Int) = (kinds.getOrNull(i - 1) as? Kind.Action)?.update == true
        fun result(i: Int) = kinds[i] == Kind.Document && kinds.getOrNull(i - 1) is Kind.Action
        // Setup and seeds only reach the actions after them.
        fun trailing(i: Int) = actions.none { it > i }
        val skipped =
            own.mapIndexedNotNull { i, block ->
                when (val kind = kinds[i]) {
                    is Kind.Skip -> SkippedCode(block, kind.reason)
                    Kind.Document ->
                        when {
                            // The update's own scenario records what it printed.
                            updated(i) -> null
                            result(i) -> null
                            trailing(i) -> SkippedCode(block, "SAMPLE_DOCUMENT")
                            else -> null
                        }
                    is Kind.Setup ->
                        if (actions.isNotEmpty() && trailing(i))
                            SkippedCode(block, "TRAILING_SETUP")
                        else null
                    is Kind.Action -> null
                }
            }
        if (actions.isEmpty())
            return listOf(Assignment(shared + own, "MISSING_COMMAND_OR_RESULT", skipped))
        return actions.map { action ->
            // Setup the section shows before this action; earlier actions are other scenarios.
            val setup =
                own.indices
                    .filter { it < action }
                    .mapNotNull { i ->
                        when {
                            kinds[i] is Kind.Setup -> own[i].copy(role = ProgramRole.SETUP)
                            kinds[i] == Kind.Document && !result(i) -> seed(own[i])
                            else -> null
                        }
                    }
            val result =
                own.getOrNull(action + 1)
                    ?.takeIf { result(action + 1) && !updated(action + 1) }
                    ?.copy(role = ProgramRole.EXPECTED)
            // A read after an update in the same section sees the update's effect, which this
            // scenario doesn't run; the update's own read will cover it.
            val afterWrite = actions.any { it < action && (kinds[it] as Kind.Action).writes }
            val lostSetup = (0 until action).any { kinds[it] == Kind.Skip("UNSUPPORTED_SETUP") }
            Assignment(
                shared +
                    setup +
                    own[action].copy(role = ProgramRole.ACTION) +
                    listOfNotNull(result),
                when {
                    lostSetup -> "UNSUPPORTED_SETUP"
                    afterWrite -> "AFTER_EARLIER_WRITE"
                    else -> null
                },
                skipped +
                    listOfNotNull(
                        own.getOrNull(action + 1)
                            ?.takeIf { result(action + 1) && updated(action + 1) }
                            ?.let { SkippedCode(it, "UPDATE_RESULT") }
                    ),
            )
        }
    }

    /**
     * Settles a scenario's candidate seeds against the setup it actually runs. Where that setup
     * inserts, the documents only illustrate and are skipped; otherwise they seed the action's
     * collection.
     */
    fun resolveSeeds(
        blocks: List<ScenarioCode>,
        dialect: ShellDialect,
    ): Pair<List<ScenarioCode>, List<SkippedCode>> {
        val seeds = blocks.filter { it.format != null && it.namespace == null }
        if (seeds.isEmpty()) return blocks to emptyList()
        val program = blocks.filter { it.format == null && it.role != ProgramRole.EXPECTED }
        val inspection = inspect(program, dialect)
        val action = program.indexOfLast { it.role == ProgramRole.ACTION }
        val namespace = inspection.namespaces.lastOrNull { it.first == "block-$action" }?.second
        if (
            inspection.state != ShellLiteralState.NORMALIZED ||
                namespace == null ||
                inspection.operations.any { it.second == ShellOperationKind.INSERT }
        )
            return blocks.filterNot { it in seeds } to
                seeds.map { SkippedCode(it, "SAMPLE_DOCUMENT") }
        return blocks.map { if (it in seeds) it.copy(namespace = namespace) else it } to emptyList()
    }

    private fun seed(block: ScenarioCode) =
        block.copy(role = ProgramRole.SETUP, format = ProgramFormat.LEGACY_DOCUMENTS)

    private fun classify(
        shared: List<ScenarioCode>,
        blocks: List<ScenarioCode>,
        dialect: ShellDialect,
    ): List<Kind> {
        val accepted = shared.toMutableList()
        return blocks.map { block ->
            val inspection = inspect(accepted + block, dialect)
            val id = "block-${accepted.size}"
            val own = inspection.operations.filter { it.first == id }
            when {
                inspection.state != ShellLiteralState.NORMALIZED ->
                    when {
                        template(block.source) -> Kind.Skip("SYNTAX_TEMPLATE")
                        expression(block) -> Kind.Skip("OPERATOR_EXPRESSION")
                        isDocument(block) -> Kind.Document
                        // Data the page loads but the grammar can't read: what runs after it
                        // would run without it.
                        setupCall.containsMatchIn(block.source) -> Kind.Skip("UNSUPPORTED_SETUP")
                        else -> Kind.Skip("UNSUPPORTED_SOURCE")
                    }
                own.isEmpty() ->
                    if (expression(block)) Kind.Skip("OPERATOR_EXPRESSION")
                    else if (isDocument(block)) Kind.Document
                    else Kind.Setup(false).also { accepted += block }
                own.all { it.second in setupKinds } ->
                    Kind.Setup(own.any { it.second == ShellOperationKind.INSERT }).also {
                        accepted += block
                    }
                own.count { it.second !in setupKinds } > 1 -> Kind.Skip("SEVERAL_COMMANDS")
                // One role per block: setup beside an action would be read as part of it.
                own.any { it.second in setupKinds } -> Kind.Skip("MIXED_SECTION_ROLES")
                // An action isn't setup for the actions after it, which are other scenarios.
                else ->
                    own.any { it.second == ShellOperationKind.UPDATE }
                        .let { update ->
                            Kind.Action(
                                update,
                                inspection.namespaces.last { it.first == id }.second,
                                update || output.containsMatchIn(block.source),
                            )
                        }
            }
        }
    }

    // Another language's code can open with a bracket too: a C# `[BsonIgnoreExtraElements]`.
    private val documentLanguages =
        setOf(null, "none", "json", "javascript", "js", "shell", "mongosh", "bash", "sh")

    private val setupCall = Regex("""\.(?:insert\w*|createIndex\w*)\s*\(""")

    /** The setup in [blocks] through the last insert into each collection, by collection. */
    fun inserts(
        blocks: List<ScenarioCode>,
        dialect: ShellDialect,
    ): Map<LogicalNamespace, List<ScenarioCode>> {
        val setup = shared(blocks, dialect).blocks.filter { it.format == null }
        if (setup.isEmpty()) return emptyMap()
        val inspection = inspect(setup, dialect)
        if (inspection.state != ShellLiteralState.NORMALIZED) return emptyMap()
        return inspection.operations
            .filter { it.second == ShellOperationKind.INSERT }
            .groupBy({ (id, _) -> inspection.namespaces.first { it.first == id }.second }) { (id, _)
                ->
                id.removePrefix("block-").toInt()
            }
            .mapValues { (_, indexes) -> setup.take(indexes.max() + 1) }
    }

    // A `$merge` or `$out` stage writes a pipeline's output for a later read; a `'$out'` string
    // doesn't.
    private val output = Regex("""[{,]\s*["']?\$(?:merge|out)["']?\s*:""")

    /** A document or document list in a data or shell language: data, not program code. */
    fun isDocument(block: ScenarioCode) =
        block.language in documentLanguages &&
            block.source.replace(leadingComments, "").let {
                it.startsWith("{") || it.startsWith("[")
            }

    // `{ $regexMatch: { ... } }` shows an expression; data and results don't lead with an operator.
    private val operatorKey = Regex("""^\[?\s*\{\s*["']?\$[A-Za-z]""")

    private fun expression(block: ScenarioCode) =
        isDocument(block) && operatorKey.containsMatchIn(block.source.replace(leadingComments, ""))

    // Pages caption printed results with a comment: `// The result` above the documents.
    private val leadingComments = Regex("""^(?:\s|//[^\n]*(?:\n|$)|/\*[\s\S]*?\*/)*""")

    private fun inspect(blocks: List<ScenarioCode>, dialect: ShellDialect) =
        ShellPrograms.inspect(
            ShellProgramRequest(
                dialect = dialect,
                blocks =
                    blocks.mapIndexed { i, block ->
                        ProgramBlock("block-$i", block.source, ProgramRole.ACTION)
                    },
            )
        )
}
