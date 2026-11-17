package dev.morphia.audits.scenarios

import dev.morphia.audits.shell.*

/**
 * A reviewed commands-then-result layout plus closed parsing establishes roles, never proximity
 * alone.
 */
internal object SectionPrograms {
    private val setupKinds = setOf(ShellOperationKind.INSERT, ShellOperationKind.INDEX)

    data class Assignment(val blocks: List<ScenarioCode>, val problem: String?)

    fun shared(blocks: List<ScenarioCode>, dialect: ShellDialect): Assignment {
        if (blocks.isEmpty()) return Assignment(blocks, "EMPTY_SHARED_SETUP")
        val inspection = inspect(blocks, dialect)
        if (
            inspection.state != ShellLiteralState.NORMALIZED ||
                inspection.operations.any { it.second !in setupKinds }
        )
            return Assignment(blocks, "UNSUPPORTED_SHARED_SETUP")
        return Assignment(blocks.map { it.copy(role = ProgramRole.SETUP) }, null)
    }

    fun assign(
        shared: List<ScenarioCode>,
        own: List<ScenarioCode>,
        dialect: ShellDialect,
    ): Assignment {
        val all = shared + own
        if (own.size < 2) return Assignment(all, "MISSING_COMMAND_OR_RESULT")
        // The selected policy declares a single trailing published result. No parser fallback
        // chooses it.
        val input = all.dropLast(1)
        val inspection = inspect(input, dialect)
        if (inspection.state != ShellLiteralState.NORMALIZED)
            return Assignment(all, "UNSUPPORTED_SECTION_INPUT")
        val byBlock = inspection.operations.groupBy({ it.first }, { it.second })
        val actionIds = inspection.operations.filter { it.second !in setupKinds }.map { it.first }
        if (actionIds.size != 1) return Assignment(all, "AMBIGUOUS_SECTION_ACTION")
        val action = actionIds.single()
        val actionIndex = input.indices.single { "block-$it" == action }
        if (byBlock.getValue(action).any { it in setupKinds } || actionIndex != input.lastIndex)
            return Assignment(all, "MIXED_SECTION_ROLES")
        return Assignment(
            all.mapIndexed { i, block ->
                block.copy(
                    role =
                        when {
                            i == all.lastIndex -> ProgramRole.EXPECTED
                            i == actionIndex -> ProgramRole.ACTION
                            else -> ProgramRole.SETUP
                        }
                )
            },
            null,
        )
    }

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
