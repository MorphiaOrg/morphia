package dev.morphia.audits.model

import dev.morphia.audits.rst.RstDocument
import java.io.File

/** Read-only input to the legacy parser, retained while the pinned AST adapter is built. */
class Operator(val type: OperatorType, val source: File) {
    val name: String = source.nameWithoutExtension
    val examples by lazy { RstDocument.read(this, source).examples }
}
