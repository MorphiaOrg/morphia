package dev.morphia.audits.repairs

import com.sun.source.tree.ClassTree
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.NewClassTree
import com.sun.source.util.JavacTask
import com.sun.source.util.TreeScanner
import com.sun.source.util.Trees
import dev.morphia.audits.bindings.BindingDescriptor
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/** How a category of legacy correction is expected to be retired. */
data class RepairCategory(val name: String, val description: String, val resolution: String)

/**
 * Reviewed classification of every distinct lock reason line (trimmed, lower-cased). A reason that
 * is not listed, or a listed reason no lock uses any more, fails the inventory.
 */
data class RepairReview(
    val schemaVersion: Int,
    val categories: List<RepairCategory>,
    val reasons: Map<String, String>,
    val exclusionCategories: List<RepairCategory>,
    /**
     * Operator folder (relative to `dev/morphia/test`) holding an `ignored` file, to its category.
     */
    val exclusions: Map<String, String>,
)

/** A whole operator excluded from example testing, with its recorded reason. */
data class CuratedExclusion(val folder: String, val reason: String, val category: String)

data class RepairMarker(val name: String, val text: String, val sha256: String)

data class OptionCall(val name: String, val argument: String)

/** What a test actually compares when it runs. */
enum class AssertionPath {
    /** The generated command is compared with the fixture's action. */
    SERIALIZATION,
    /** The command runs against a server and its result is compared with the fixture's expected. */
    BEHAVIOR,
    /** Handwritten assertions that do not go through the template helpers. */
    CUSTOM,
}

enum class RepairDisposition {
    /** No lock: the fixture is whatever the legacy extractor produced. */
    UNCORRECTED,
    /**
     * The curated fixture is preserved as the baseline; its upstream match is unknown until an
     * extracted replacement proves equivalence.
     */
    PRESERVED_CURATED,
    /** A marker-only fixture with no test; nothing executes. */
    NOT_EXECUTED,
}

data class CuratedExample(
    val fixture: String,
    val type: String?,
    val method: String?,
    val markers: List<RepairMarker>,
    val categories: List<String>,
    val disposition: RepairDisposition,
    /** A `@Disabled` test executes nothing, whatever its body would compare. */
    val disabled: Boolean,
    val helpers: List<String>,
    val options: List<OptionCall>,
    val assertions: List<AssertionPath>,
)

data class CuratedRepairReport(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val categories: List<RepairCategory>,
    val exclusionCategories: List<RepairCategory>,
    val examples: List<CuratedExample>,
    val exclusions: List<CuratedExclusion>,
    val summary: Map<String, Int>,
)

/**
 * Inventories the curated corrections (lock markers) and the per-test options and assertion paths
 * of the bound operator examples. Read-only: it parses sources without attribution or execution.
 */
object CuratedRepairs {
    private val markerNames = setOf("lock", "lockd")
    private val helperNames = setOf("testPipeline", "testQuery", "testUpdate")
    private val setters =
        setOf(
            "findOptions",
            "minDriver",
            "orderMatters",
            "removeIds",
            "serverVersion",
            "skipActionCheck",
            "skipDataCheck",
            "skipIndex",
            "updateOptions",
        )

    fun reason(line: String): String = line.trim().lowercase(Locale.ROOT)

    fun capture(
        repository: Path,
        revision: String,
        descriptor: BindingDescriptor,
        review: RepairReview,
    ): CuratedRepairReport {
        require(review.schemaVersion == 1) { "Unsupported repair review ${review.schemaVersion}" }
        val names = review.categories.map { it.name }
        require(names.toSet().size == names.size) { "Duplicate repair category" }
        val root = repository.toRealPath()
        val resources = root.resolve("core/src/test/resources")
        val problems = mutableListOf<String>()
        review.reasons.forEach { (line, category) ->
            if (category !in names) problems += "Unknown category $category for reason: $line"
            if (line != reason(line)) problems += "Reason keys must be trimmed lower case: $line"
        }
        val methods = parse(root, descriptor.classes.map { it.type })
        val used = mutableSetOf<String>()
        val examples =
            descriptor.classes.flatMap { type ->
                val owned =
                    type.bindings.map { it.resource to it.method } +
                        type.unbound.map { it.resource to null }
                owned.map { (resource, method) ->
                    val folder = resources.resolve("${type.resources}/$resource")
                    val markers =
                        markerNames
                            .sorted()
                            .map { folder.resolve(it) }
                            .filter { it.isRegularFile() }
                            .map { file ->
                                val bytes = Files.readAllBytes(file)
                                RepairMarker(
                                    file.fileName.toString(),
                                    file.readText(),
                                    sha256(bytes),
                                )
                            }
                    val categories =
                        markers
                            .flatMap { it.text.lines() }
                            .filter { it.isNotBlank() }
                            .mapNotNull { line ->
                                val key = reason(line)
                                used += key
                                review.reasons[key].also {
                                    if (it == null) {
                                        problems +=
                                            "Unreviewed lock reason in ${type.resources}/$resource: $key"
                                    }
                                }
                            }
                            .distinct()
                            .sorted()
                    if (markers.isNotEmpty() && markers.all { it.text.isBlank() }) {
                        problems += "Lock without a reason: ${type.resources}/$resource"
                    }
                    val body = method?.let { methods[type.type to it] }
                    if (method != null && body == null) {
                        problems += "Missing bound method ${type.type}#$method"
                    }
                    body?.problems?.forEach { problems += "${type.type}#$method: $it" }
                    val options = body?.options ?: emptyList()
                    fun skipped(name: String) =
                        options.any { it.name == name && it.argument == "true" }
                    val assertions =
                        when {
                            body == null || body.empty || body.disabled -> emptyList()
                            body.helpers.isEmpty() -> listOf(AssertionPath.CUSTOM)
                            else ->
                                listOfNotNull(
                                    AssertionPath.SERIALIZATION.takeIf {
                                        !skipped("skipActionCheck")
                                    },
                                    AssertionPath.BEHAVIOR.takeIf { !skipped("skipDataCheck") },
                                )
                        }
                    CuratedExample(
                        fixture = "${type.resources.removePrefix("dev/morphia/test/")}/$resource",
                        type = type.type,
                        method = method,
                        markers = markers,
                        categories = categories,
                        disposition =
                            when {
                                method == null -> RepairDisposition.NOT_EXECUTED
                                markers.isEmpty() -> RepairDisposition.UNCORRECTED
                                else -> RepairDisposition.PRESERVED_CURATED
                            },
                        disabled = body?.disabled ?: false,
                        helpers = body?.helpers ?: emptyList(),
                        options = options,
                        assertions = assertions,
                    )
                }
            }
        (review.reasons.keys - used).forEach { problems += "Stale reviewed lock reason: $it" }
        val exclusionNames = review.exclusionCategories.map { it.name }.toSet()
        val exclusions =
            listOf(
                    "aggregation/expressions",
                    "aggregation/stages",
                    "query/filters",
                    "query/updates",
                )
                .flatMap { category ->
                    val directory = resources.resolve("dev/morphia/test/$category")
                    if (!Files.isDirectory(directory)) emptyList()
                    else
                        Files.list(directory)
                            .use { it.sorted().toList() }
                            .filter { it.resolve("ignored").isRegularFile() }
                            .map {
                                "$category/${it.fileName}" to
                                    it.resolve("ignored").readText().trim()
                            }
                }
                .mapNotNull { (folder, reason) ->
                    val category = review.exclusions[folder]
                    when {
                        reason.isEmpty() -> problems += "Exclusion without a reason: $folder"
                        category == null -> problems += "Unreviewed exclusion: $folder"
                        category !in exclusionNames ->
                            problems += "Unknown exclusion category $category for $folder"
                    }
                    category?.let { CuratedExclusion(folder, reason, it) }
                }
        (review.exclusions.keys - exclusions.map { it.folder }.toSet()).forEach {
            problems += "Stale reviewed exclusion: $it"
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        val sorted = examples.sortedBy { it.fixture }
        return CuratedRepairReport(
            repositoryRevision = revision,
            categories = review.categories,
            exclusionCategories = review.exclusionCategories,
            examples = sorted,
            exclusions = exclusions,
            summary =
                summarize(sorted, names) +
                    exclusions.groupingBy { "exclusion.${it.category}" }.eachCount(),
        )
    }

    private fun summarize(
        examples: List<CuratedExample>,
        categories: List<String>,
    ): Map<String, Int> {
        val summary = sortedMapOf<String, Int>()
        summary["examples"] = examples.size
        summary["examples.locked"] = examples.count { it.markers.isNotEmpty() }
        summary["markers.lock"] = examples.count { e -> e.markers.any { it.name == "lock" } }
        summary["markers.lockd"] = examples.count { e -> e.markers.any { it.name == "lockd" } }
        RepairDisposition.values().forEach { d ->
            summary["disposition.$d"] = examples.count { it.disposition == d }
        }
        categories.forEach { c -> summary["category.$c"] = examples.count { c in it.categories } }
        AssertionPath.values().forEach { a ->
            summary["assertion.$a"] = examples.count { a in it.assertions }
        }
        summary["assertion.NONE"] =
            examples.count { it.method != null && !it.disabled && it.assertions.isEmpty() }
        summary["assertion.DISABLED"] = examples.count { it.disabled }
        examples
            .flatMap { e -> e.options.map { it.name }.distinct() }
            .groupingBy { it }
            .eachCount()
            .forEach { (name, count) -> summary["option.$name"] = count }
        return summary
    }

    private class MethodFacts(
        val helpers: List<String>,
        val options: List<OptionCall>,
        val empty: Boolean,
        val disabled: Boolean,
        val problems: List<String>,
    )

    private fun parse(root: Path, types: List<String>): Map<Pair<String, String>, MethodFacts> {
        val files =
            types.associateBy { root.resolve("core/src/test/java/${it.replace('.', '/')}.java") }
        val texts =
            files.keys.associateWith {
                Files.readAllBytes(it).decodeToString(throwOnInvalidSequence = true)
            }
        val compiler =
            requireNotNull(ToolProvider.getSystemJavaCompiler()) {
                "Repair inventory requires a full JDK 17 or newer"
            }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        return compiler.getStandardFileManager(diagnostics, Locale.ROOT, UTF_8).use { manager ->
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, emptyList())
            manager.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH, emptyList())
            val task =
                compiler.getTask(
                    null,
                    manager,
                    diagnostics,
                    listOf("-proc:none", "--release", "17", "-implicit:none"),
                    null,
                    texts.map { (path, text) ->
                        object : SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
                            override fun getCharContent(ignoreEncodingErrors: Boolean) = text
                        }
                    },
                ) as JavacTask
            val units = task.parse().toList()
            val errors = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
            require(errors.isEmpty()) {
                "Invalid Java source: ${errors.map { it.getMessage(Locale.ROOT) }}"
            }
            val positions = Trees.instance(task).sourcePositions
            val result = mutableMapOf<Pair<String, String>, MethodFacts>()
            units.forEach { unit ->
                val path = Path.of(unit.sourceFile.toUri())
                val type = files.getValue(path)
                val text = texts.getValue(path)
                unit.typeDecls.filterIsInstance<ClassTree>().forEach { declaration ->
                    declaration.members.filterIsInstance<MethodTree>().forEach { method ->
                        val body = method.body ?: return@forEach
                        val disabled =
                            method.modifiers.annotations.any {
                                it.annotationType.toString().substringAfterLast('.') == "Disabled"
                            }
                        result[type to method.name.toString()] =
                            facts(unit, positions, text, body, disabled)
                    }
                }
            }
            result
        }
    }

    private fun facts(
        unit: CompilationUnitTree,
        positions: com.sun.source.util.SourcePositions,
        text: String,
        body: com.sun.source.tree.BlockTree,
        disabled: Boolean,
    ): MethodFacts {
        val problems = mutableListOf<String>()
        val invoked = mutableListOf<String>()
        val options = mutableListOf<OptionCall>()
        object : TreeScanner<Unit, Unit>() {
                override fun visitMethodInvocation(node: MethodInvocationTree, p: Unit?) {
                    when (val select = node.methodSelect) {
                        is IdentifierTree ->
                            select.name
                                .toString()
                                .takeIf { it in helperNames }
                                ?.let { invoked += it }
                        is MemberSelectTree -> {
                            val name = select.identifier.toString()
                            if (name in helperNames) {
                                // Only direct calls are understood; anything else would be misread.
                                problems += "Unsupported helper call form: $select"
                            }
                            if (
                                name.startsWith("skip") &&
                                    name in setters &&
                                    !rootedInOptions(select.expression)
                            ) {
                                problems += "$name must be chained on new ActionTestOptions()"
                            }
                            if (name in setters && rootedInOptions(select.expression)) {
                                val argument = node.arguments.singleOrNull()
                                if (name.startsWith("skip") && argument !is LiteralTree) {
                                    problems += "Non-literal $name argument cannot be inventoried"
                                }
                                options +=
                                    OptionCall(
                                        name,
                                        when (argument) {
                                            null -> ""
                                            is LiteralTree -> argument.value.toString()
                                            else ->
                                                text.substring(
                                                    positions
                                                        .getStartPosition(unit, argument)
                                                        .toInt(),
                                                    positions.getEndPosition(unit, argument).toInt(),
                                                )
                                        },
                                    )
                            }
                        }
                    }
                    super.visitMethodInvocation(node, p)
                }
            }
            .scan(body, null)
        return MethodFacts(
            invoked.distinct().sorted(),
            options.sortedWith(compareBy({ it.name }, { it.argument })),
            body.statements.isEmpty(),
            disabled,
            problems,
        )
    }

    private fun rootedInOptions(tree: com.sun.source.tree.ExpressionTree): Boolean =
        when (tree) {
            is NewClassTree ->
                tree.identifier.toString().substringAfterLast('.') == "ActionTestOptions"
            is MethodInvocationTree ->
                (tree.methodSelect as? MemberSelectTree)?.let { rootedInOptions(it.expression) }
                    ?: false
            else -> false
        }

    private val ByteArray.hex: String
        get() = HexFormat.of().formatHex(this)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).hex
}
