package dev.morphia.audits.generate

import dev.morphia.audits.api.ApiInventory
import dev.morphia.audits.api.DeclarationKind

/**
 * Where an MQL operator appears: as a pipeline stage, inside an expression, in a query filter, or
 * as an update operator.
 */
enum class BindingPosition {
    STAGE,
    EXPRESSION,
    FILTER,
    UPDATE,
}

/** How an operator's BSON value becomes the factory's arguments. */
enum class BindingForm {
    /**
     * `{$op: [a, b, …]}`, or `{$op: a}` for one argument: the values in order, the last parameter
     * taking the rest when it is varargs.
     */
    POSITIONAL,
    /**
     * `{$op: {k: v, …}}`: the [DslBinding.arguments] keys, in parameter order, are the factory's
     * arguments; every other key is an [DslBinding.options] call on its result.
     */
    NAMED,
    /**
     * `{$project: {field: value, …}}`: the factory takes no arguments, and each field is one of the
     * [DslBinding.fields] calls: `include` for `1` or `true`, `exclude` for `0` or `false`,
     * `suppressId` for an `_id` of `0` or `false`, and `value` for anything else.
     */
    PROJECTION,
    /** `{$addFields: {field: value, …}}`: no arguments, then the `value` call per field. */
    FIELDS,
    /**
     * `{$match: {field: predicate, …}}`: one varargs parameter taking a filter per predicate. A
     * field's `{$op: v, …}` is a [FIELD] filter per operator; any other value is `$eq`.
     */
    FILTERS,
    /**
     * An operator on a field: a query operator, `{field: {$op: v}}`, or an update operator, `{$op:
     * {field: v}}`. The factory takes the field name and, when it has a second parameter, `v`. A
     * one-parameter query factory (`$exists`) takes only `true`.
     */
    FIELD,
    /**
     * `{$group: {_id: v, field: value, …}}`: the factory takes the [DslBinding.fields] `id` call of
     * `v` (a static factory of the stage's class), then the `value` call per field.
     */
    GROUP,
    /**
     * `{$sort: {field: 1 | -1, …}}`: no arguments, then the `ascending` or `descending` call per
     * field, in order.
     */
    SORT,
}

/** A method called on a factory's result, by name and erased parameter types. */
data class MethodRef(val method: String, val parameters: List<String>)

/**
 * A reviewed mapping from an MQL operator to the Morphia factory that builds it. Parameter types
 * are erased, fully qualified names (`java.lang.Object[]` for varargs) exactly as
 * `MorphiaApiInventory` records them.
 */
data class DslBinding(
    val operator: String,
    val position: BindingPosition,
    val form: BindingForm,
    val owner: String,
    val method: String,
    val parameters: List<String>,
    val varargs: Boolean = false,
    val arguments: List<String> = emptyList(),
    val options: Map<String, MethodRef> = emptyMap(),
    val fields: Map<String, MethodRef> = emptyMap(),
)

data class DslBindingCatalog(val schemaVersion: Int = 1, val bindings: List<DslBinding>)

object DslBindings {
    const val PATH = "audits/catalog/dsl-bindings.json"
    private val projectionFields = setOf("include", "exclude", "suppressId", "value")
    private const val FILTER = "dev.morphia.query.filters.Filter"

    /**
     * Checks every binding against [api]: its factory is a public static method of its owner with
     * exactly its parameter types, and every option or field call is a public method of the
     * factory's return type or one of its supertypes. Returns the problems; none means current.
     */
    fun verify(catalog: DslBindingCatalog, api: ApiInventory): List<String> {
        val methods =
            api.declarations
                .filter { it.kind == DeclarationKind.METHOD && "public" in it.modifiers }
                .associateBy { it.id }
        val supertypes =
            api.declarations
                .filter { it.kind == DeclarationKind.TYPE }
                .associate { it.owner to it.supertypes.map(::erase) }
        fun hierarchy(type: String): List<String> =
            listOf(type) + supertypes[type].orEmpty().flatMap(::hierarchy)
        val problems = mutableListOf<String>()
        catalog.bindings
            .groupBy { it.position to it.operator }
            .filterValues { it.size > 1 }
            .keys
            .forEach { (position, operator) -> problems += "$operator: bound twice as $position" }
        catalog.bindings.forEach { binding ->
            val label = "${binding.operator} (${binding.owner}#${binding.method})"
            val factory = methods[id(binding.owner, binding.method, binding.parameters)]
            if (factory == null || "static" !in factory.modifiers) {
                problems += "$label: no public static ${binding.method}${binding.parameters}"
                return@forEach
            }
            if (binding.varargs != (factory.parameters.lastOrNull()?.varargs == true)) {
                problems += "$label: varargs is ${!binding.varargs} in the source"
            }
            when (binding.form) {
                BindingForm.POSITIONAL ->
                    if (binding.arguments.isNotEmpty() || binding.options.isNotEmpty())
                        problems += "$label: a positional binding names no keys"
                BindingForm.NAMED ->
                    if (binding.arguments.size != binding.parameters.size)
                        problems +=
                            "$label: ${binding.arguments.size} keys for ${binding.parameters.size} parameters"
                BindingForm.FIELDS ->
                    if (binding.fields.keys != setOf("value") || binding.parameters.isNotEmpty())
                        problems += "$label: a fields stage takes no arguments and binds [value]"
                BindingForm.PROJECTION ->
                    if (binding.fields.keys != projectionFields || binding.parameters.isNotEmpty())
                        problems +=
                            "$label: a projection takes no arguments and binds $projectionFields"
                BindingForm.FILTERS ->
                    if (!binding.varargs || binding.parameters != listOf("$FILTER[]"))
                        problems += "$label: a filters stage takes only $FILTER varargs"
                BindingForm.GROUP ->
                    if (binding.fields.keys != setOf("id", "value") || binding.parameters.size != 1)
                        problems += "$label: a group stage takes its id and binds [id, value]"
                BindingForm.SORT ->
                    if (
                        binding.fields.keys != setOf("ascending", "descending") ||
                            binding.parameters.isNotEmpty()
                    )
                        problems +=
                            "$label: a sort stage takes no arguments and binds [ascending, descending]"
                BindingForm.FIELD ->
                    if (
                        binding.position !in
                            setOf(BindingPosition.FILTER, BindingPosition.UPDATE) ||
                            binding.parameters.firstOrNull() != "java.lang.String" ||
                            binding.parameters.size !in 1..2
                    )
                        problems +=
                            "$label: a field operator takes the field name and at most a value"
            }
            val result = erase(factory.returnType ?: "")
            (binding.options + binding.fields).forEach { (key, call) ->
                if (
                    hierarchy(result).none { owner ->
                        methods[id(owner, call.method, call.parameters)] != null
                    }
                )
                    problems +=
                        "$label: $key: $result has no public ${call.method}${call.parameters}"
            }
        }
        return problems
    }

    private fun id(owner: String, method: String, parameters: List<String>) =
        "$owner#$method(${parameters.joinToString(",")})"

    private fun erase(type: String): String = type.substringBefore('<')
}
