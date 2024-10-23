package dev.morphia.audits.model

import dev.morphia.audits.rst.OperatorExample

data class Results(
    val created: List<Operator>,
    val noExamples: List<OperatorExample>,
    val noTest: List<Operator>,
    val noServerRelease: List<String>
)
