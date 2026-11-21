package dev.morphia.audits.bindings

import com.sun.source.tree.AnnotationTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MethodTree
import com.sun.source.util.JavacTask
import com.sun.source.util.SourcePositions
import com.sun.source.util.Trees
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
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/** Reviewed exceptions to one-method-per-fixture ownership. Stale entries fail the import. */
data class BindingPolicy(
    val schemaVersion: Int,
    val unboundResources: List<UnboundResource>,
    val manualMethods: List<ManualMethod>,
)

/** [resource] is relative to `core/src/test/resources/dev/morphia/test`. */
data class UnboundResource(val resource: String, val reason: String)

data class ManualMethod(val type: String, val method: String, val reason: String)

enum class BindingDisposition {
    /** Owns exactly one fixture directory. */
    BOUND,
    /** A reviewed test in a templated class that owns no fixture. */
    MANUAL,
    /** A test in a class that does not use templated fixtures at all. */
    INDEPENDENT,
}

/**
 * Declared source evidence for one JUnit method. This is not execution coverage: an empty or
 * disabled method stays visible as such rather than being counted as support.
 */
data class LegacyMethod(
    val type: String,
    val method: String,
    val source: String,
    val disposition: BindingDisposition,
    val resource: String?,
    val reason: String?,
    val displayName: String?,
    val disabled: Boolean,
    val emptyBody: Boolean,
    val bodySha256: String,
)

data class LegacyBindingCatalog(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val packages: List<String>,
    val methods: List<LegacyMethod>,
    val unboundResources: List<UnboundResource>,
) {
    /** The runtime descriptor read by core's `ExampleBindings`. */
    fun descriptor(): BindingDescriptor =
        BindingDescriptor(
            packages = packages.map { "$ROOT_PACKAGE.${it.replace('/', '.')}" },
            classes =
                methods
                    .filter { it.disposition != BindingDisposition.INDEPENDENT }
                    .groupBy { it.type }
                    .toSortedMap()
                    .map { (type, methods) ->
                        val resources = resources(type)
                        val folder = resources.removePrefix("$ROOT_PATH/")
                        DescriptorClass(
                            type = type,
                            resources = resources,
                            bindings =
                                methods
                                    .filter { it.disposition == BindingDisposition.BOUND }
                                    .map {
                                        DescriptorBinding(
                                            it.method,
                                            it.resource!!.substringAfterLast('/'),
                                        )
                                    }
                                    .sortedBy { it.method },
                            manual =
                                methods
                                    .filter { it.disposition == BindingDisposition.MANUAL }
                                    .map { DescriptorManual(it.method, it.reason!!) }
                                    .sortedBy { it.method },
                            unbound =
                                unboundResources
                                    .filter { it.resource.substringBeforeLast('/') == folder }
                                    .map {
                                        DescriptorUnbound(
                                            it.resource.substringAfterLast('/'),
                                            it.reason,
                                        )
                                    }
                                    .sortedBy { it.resource },
                        )
                    },
        )
}

data class BindingDescriptor(
    val schemaVersion: Int = 1,
    val packages: List<String>,
    val classes: List<DescriptorClass>,
)

data class DescriptorClass(
    val type: String,
    val resources: String,
    val bindings: List<DescriptorBinding>,
    val manual: List<DescriptorManual>,
    val unbound: List<DescriptorUnbound>,
)

data class DescriptorBinding(val method: String, val resource: String)

data class DescriptorManual(val method: String, val reason: String)

data class DescriptorUnbound(val resource: String, val reason: String)

const val ROOT_PACKAGE = "dev.morphia.test"
private const val ROOT_PATH = "dev/morphia/test"
private const val TEMPLATE = "TemplatedTestBase"

/** Files an unbound, marker-only fixture directory may hold. */
private val MARKERS = setOf("name", "lock", "lockd")

/** Mirrors `TemplatedTestBase.prefix()`, including its replacement of every "Test". */
fun resources(type: String): String {
    val simple = type.substringAfterLast('.').replace("Test", "")
    return type.substringBeforeLast('.').replace('.', '/') +
        "/" +
        simple.first().lowercaseChar() +
        simple.substring(1)
}

/**
 * Imports actual method/fixture ownership from the operator test sources with the JDK parser (no
 * processors, no implicit sources, no attribution or execution). The legacy method-name convention
 * proposes a pairing only here; afterwards the descriptor is the source of truth.
 */
object LegacyBindings {
    val packages =
        listOf("aggregation/expressions", "aggregation/stages", "query/filters", "query/updates")

    fun capture(repository: Path, revision: String, policy: BindingPolicy): LegacyBindingCatalog {
        require(revision.isNotBlank()) { "A repository revision is required" }
        require(policy.schemaVersion == 1) { "Unsupported binding policy ${policy.schemaVersion}" }
        val root = repository.toRealPath()
        val javaRoot = root.resolve("core/src/test/java/$ROOT_PATH")
        val resourceRoot = root.resolve("core/src/test/resources/$ROOT_PATH")
        val problems = mutableListOf<String>()
        val unbound = policy.unboundResources.associateBy { it.resource }
        val manual = policy.manualMethods.associateBy { it.type to it.method }
        require(unbound.size == policy.unboundResources.size) { "Duplicate unbound policy entry" }
        require(manual.size == policy.manualMethods.size) { "Duplicate manual policy entry" }
        val usedUnbound = mutableSetOf<String>()
        val usedManual = mutableSetOf<Pair<String, String>>()
        val methods = mutableListOf<LegacyMethod>()
        val owned = mutableSetOf<String>()
        packages.forEach { category ->
            val sources = children(javaRoot.resolve(category)).filter { it.name.endsWith(".java") }
            val folders = children(resourceRoot.resolve(category)).filter { it.isDirectory() }
            parse(root, sources).forEach { parsed ->
                val type = "$ROOT_PACKAGE.${category.replace('/', '.')}.${parsed.simpleName}"
                val templated = parsed.superclass == TEMPLATE
                val folder = resources(type).removePrefix("$ROOT_PATH/")
                if (templated) owned += folder
                parsed.methods.forEach { method ->
                    val resource = "$folder/${derived(method.name)}"
                    val policyEntry = manual[type to method.name]
                    val bound =
                        templated &&
                            method.name.startsWith("test") &&
                            resourceRoot.resolve(resource).isDirectory()
                    val disposition =
                        when {
                            !templated -> BindingDisposition.INDEPENDENT
                            bound -> BindingDisposition.BOUND
                            policyEntry != null -> BindingDisposition.MANUAL
                            else -> {
                                problems +=
                                    "Unowned test method without a fixture: $type#${method.name}"
                                return@forEach
                            }
                        }
                    if (policyEntry != null) {
                        if (disposition == BindingDisposition.MANUAL)
                            usedManual += type to method.name
                        else problems += "Stale manual policy for bound $type#${method.name}"
                    }
                    methods +=
                        LegacyMethod(
                            type = type,
                            method = method.name,
                            source = parsed.source,
                            disposition = disposition,
                            resource = resource.takeIf { bound },
                            reason = policyEntry?.reason?.takeIf { !bound },
                            displayName = method.displayName,
                            disabled = method.disabled,
                            emptyBody = method.emptyBody,
                            bodySha256 = method.bodySha256,
                        )
                }
            }
            owned
                .filter { it.startsWith("$category/") && !resourceRoot.resolve(it).isDirectory() }
                .forEach { problems += "Missing fixture directory for templated class: $it" }
            val bound = methods.mapNotNull { it.resource }.toSet()
            folders.forEach { operator ->
                val folder = "$category/${operator.name}"
                val examples = children(operator).filter { it.isDirectory() }
                if (folder !in owned) {
                    if (!operator.resolve("ignored").isRegularFile()) {
                        problems += "No test class owns $folder"
                    }
                    examples.forEach { problems += "No test owns fixture $folder/${it.name}" }
                    return@forEach
                }
                examples.forEach { example ->
                    val resource = "$folder/${example.name}"
                    when {
                        resource in bound ->
                            if (resource in unbound) {
                                problems += "Stale unbound policy for bound fixture $resource"
                            }
                        resource in unbound -> {
                            usedUnbound += resource
                            val files = children(example).map { it.name }
                            if (!MARKERS.containsAll(files)) {
                                problems +=
                                    "Unbound fixture $resource holds more than markers: $files"
                            }
                        }
                        else -> problems += "Fixture without a test method: $resource"
                    }
                }
            }
        }
        methods
            .mapNotNull { it.resource }
            .groupBy { it.lowercase(Locale.ROOT) }
            .filterValues { it.size > 1 }
            .forEach { (_, owners) -> problems += "Duplicate fixture owners: $owners" }
        (manual.keys - usedManual).forEach {
            if (methods.none { m -> m.type == it.first && m.method == it.second }) {
                problems += "Stale manual policy for missing ${it.first}#${it.second}"
            }
        }
        (unbound.keys - usedUnbound).forEach { problems += "Stale unbound policy for $it" }
        policy.unboundResources.forEach {
            if (it.reason.isBlank()) problems += "Unbound fixture needs a reason: ${it.resource}"
        }
        policy.manualMethods.forEach {
            if (it.reason.isBlank())
                problems += "Manual method needs a reason: ${it.type}#${it.method}"
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return LegacyBindingCatalog(
            repositoryRevision = revision,
            packages = packages,
            methods = methods.sortedWith(compareBy({ it.type }, { it.method })),
            unboundResources = policy.unboundResources.sortedBy { it.resource },
        )
    }

    /** Mirrors `TemplatedTestBase.discoverResourceName()`. */
    fun derived(method: String): String {
        if (!method.startsWith("test")) return method
        val name = method.substring(4)
        return name.substring(0, 1).lowercase(Locale.ROOT) + name.substring(1)
    }

    private class ParsedMethod(
        val name: String,
        val displayName: String?,
        val disabled: Boolean,
        val emptyBody: Boolean,
        val bodySha256: String,
        val body: String,
    )

    private class ParsedClass(
        val simpleName: String,
        val superclass: String?,
        val source: String,
        val methods: List<ParsedMethod>,
    )

    /**
     * The exact source text of each test method body in [source] (a path relative to [repository]),
     * the text whose SHA-256 a catalog records as `bodySha256`.
     */
    fun bodies(repository: Path, source: String): Map<String, String> {
        val root = repository.toRealPath()
        return parse(root, listOf(root.resolve(source))).single().methods.associate {
            it.name to it.body
        }
    }

    private fun parse(root: Path, files: List<Path>): List<ParsedClass> {
        if (files.isEmpty()) return emptyList()
        val texts =
            files.associateWith {
                Files.readAllBytes(it).decodeToString(throwOnInvalidSequence = true)
            }
        val inputs =
            texts.map { (path, text) ->
                object : SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
                    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = text
                }
            }
        val compiler =
            requireNotNull(ToolProvider.getSystemJavaCompiler()) {
                "Binding import requires a full JDK 17 or newer"
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
                    inputs,
                ) as JavacTask
            val units = task.parse().toList()
            val errors = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
            require(errors.isEmpty()) {
                "Invalid Java source: " +
                    errors.joinToString("; ") {
                        "${it.source?.name}:${it.lineNumber}: ${it.getMessage(Locale.ROOT)}"
                    }
            }
            val positions = Trees.instance(task).sourcePositions
            units.flatMap { unit ->
                val path = Path.of(unit.sourceFile.toUri())
                val text = texts.getValue(path)
                unit.typeDecls.filterIsInstance<ClassTree>().map { type ->
                    require(type.simpleName.toString() + ".java" == path.name) {
                        "Unexpected top-level type ${type.simpleName} in $path"
                    }
                    ParsedClass(
                        type.simpleName.toString(),
                        type.extendsClause?.toString()?.substringAfterLast('.'),
                        root.relativize(path).joinToString("/"),
                        type.members
                            .filterIsInstance<MethodTree>()
                            .filter { method -> annotation(method, "Test") != null }
                            .map { method -> method(unit, positions, text, method) },
                    )
                }
            }
        }
    }

    private fun method(
        unit: com.sun.source.tree.CompilationUnitTree,
        positions: SourcePositions,
        text: String,
        method: MethodTree,
    ): ParsedMethod {
        val body = requireNotNull(method.body) { "Test method ${method.name} has no body" }
        val start = positions.getStartPosition(unit, body).toInt()
        val end = positions.getEndPosition(unit, body).toInt()
        val displayName =
            annotation(method, "DisplayName")?.let { annotation ->
                val value = annotation.arguments.singleOrNull()
                val literal = (value as? com.sun.source.tree.AssignmentTree)?.expression ?: value
                require(literal is LiteralTree && literal.value is String) {
                    "DisplayName on ${method.name} must be a string literal"
                }
                literal.value as String
            }
        return ParsedMethod(
            name = method.name.toString(),
            displayName = displayName,
            disabled = annotation(method, "Disabled") != null,
            emptyBody = body.statements.isEmpty(),
            bodySha256 =
                HexFormat.of()
                    .formatHex(
                        MessageDigest.getInstance("SHA-256")
                            .digest(text.substring(start, end).toByteArray(UTF_8))
                    ),
            body = text.substring(start, end),
        )
    }

    private fun annotation(method: MethodTree, name: String): AnnotationTree? =
        method.modifiers.annotations.firstOrNull {
            it.annotationType.toString().substringAfterLast('.') == name
        }

    private fun children(directory: Path): List<Path> {
        require(directory.isDirectory()) { "Missing directory: $directory" }
        return Files.list(directory).use { entries ->
            entries.sorted().toList().onEach {
                require(!Files.isSymbolicLink(it)) { "Entries must not be symlinks: $it" }
            }
        }
    }
}
