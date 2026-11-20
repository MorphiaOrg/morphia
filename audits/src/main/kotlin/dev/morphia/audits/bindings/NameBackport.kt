package dev.morphia.audits.bindings

import dev.morphia.audits.CorpusInventory
import dev.morphia.audits.naming.DescriptiveNames
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/** The authored page title recovered for a folder whose examples carry only a generic label. */
data class PageTitle(val folder: String, val title: String, val source: String)

/** A reviewed stem for a case whose title collides with a sibling or is not descriptive. */
data class ReviewedStem(val resource: String, val stem: String, val reason: String)

data class NameBackportReview(
    val schemaVersion: Int,
    val pageTitles: List<PageTitle>,
    val reviewedStems: List<ReviewedStem>,
)

enum class TitleSource {
    /** The example's own `name` file title. */
    NAME_FILE,
    /** The pinned documentation page title, for a generic example label. */
    PAGE_TITLE,
    /** An explicit reviewed name. */
    REVIEWED,
}

/**
 * One legacy-to-descriptive rename. [type]/[oldMethod]/[newMethod] are null for a fixture that has
 * no test method, and [oldResource]/[newResource] are null for a manual method without a fixture.
 */
data class RenamedExample(
    val type: String?,
    val oldMethod: String?,
    val newMethod: String?,
    val oldResource: String?,
    val newResource: String?,
    val displayTitle: String?,
    val titleSource: TitleSource,
    val evidence: String,
)

data class NameBackportLedger(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val renames: List<RenamedExample>,
)

/**
 * The one-time backport of ordinal `testExampleN`/`exampleN` names to descriptive names. Fixture
 * bytes, `name` files, display titles, bodies and options are preserved; only the method name, the
 * example directory segment and the method's "test data" Javadoc line change, together.
 */
object NameBackport {
    private val ordinal = Regex("example[0-9]+")
    private val ordinalMethod = Regex("testExample[0-9]+")
    /** A "test data" Javadoc line and the method declaration it documents. */
    private val testData =
        Regex("""(?s)test data: (dev/morphia/test/\S+)[^/]*?\*/(?:(?!\*/).)*?\bvoid\s+(\w+)\s*\(""")
    private val between =
        Regex(
            """(?s)(\s*@[\w.]+(\s*\((?:[^()"]|"(?:[^"\\]|\\.)*"|\([^()]*\))*\))?)*\s*""" +
                """(?:(?:public|protected|private|static|final|synchronized)\s+)*"""
        )

    /**
     * Legacy titles still carry RST inline markup and the generated pages' operator substitution.
     */
    fun plainTitle(raw: String, operator: String): String {
        val plain =
            raw.replace(Regex(""":[\w:-]+:`(~?)([^`<]*?)\s*(?:<[^>]*>)?`""")) {
                    val target = it.groupValues[2]
                    if (it.groupValues[1] == "~") target.substringAfterLast('.') else target
                }
                .replace("|operatorName|", "$$operator")
                .replace(Regex("``([^`]*)``"), "$1")
                .replace(Regex("""^\s*Example\s+[0-9]+\s*:\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s*\[[0-9]+]\s*$"""), "")
                .trim()
        // Legacy "section :: tab" titles keep only the tab when the section is a generic label.
        val section = plain.substringBefore(" :: ", "")
        return if (section.isNotEmpty() && DescriptiveNames.isGeneric(section)) {
            plain.substringAfter(" :: ").trim()
        } else plain
    }

    fun plan(
        bindings: LegacyBindingCatalog,
        corpus: CorpusInventory,
        review: NameBackportReview,
        inherited: Set<String>,
    ): NameBackportLedger {
        require(review.schemaVersion == 1) { "Unsupported review schema ${review.schemaVersion}" }
        val prefix = "core/src/test/resources/dev/morphia/test/"
        val titles = corpus.examples.associate { it.path.removePrefix(prefix) to it.displayName }
        val pages = review.pageTitles.associateBy { it.folder }
        val reviewed = review.reviewedStems.associateBy { it.resource }
        require(pages.size == review.pageTitles.size) { "Duplicate page title entry" }
        require(reviewed.size == review.reviewedStems.size) { "Duplicate reviewed stem" }
        val problems = mutableListOf<String>()
        val usedPages = mutableSetOf<String>()
        val usedReviews = mutableSetOf<String>()

        fun name(
            resource: String?,
            rawTitle: String?,
            operator: String,
        ): Triple<String, TitleSource, String>? {
            val key = resource ?: return null
            reviewed[key]?.let {
                usedReviews += key
                return Triple(it.stem, TitleSource.REVIEWED, it.reason)
            }
            val plain = rawTitle?.let { plainTitle(it, operator) }
            if (plain != null && !DescriptiveNames.isGeneric(plain)) {
                return Triple(
                    DescriptiveNames.stem(plain),
                    TitleSource.NAME_FILE,
                    "name file: $rawTitle",
                )
            }
            val folder = key.substringBeforeLast('/')
            val page = pages[folder]
            if (page == null) {
                problems += "Generic title for $key needs a page title or reviewed name"
                return null
            }
            usedPages += folder
            return Triple(
                DescriptiveNames.stem(plainTitle(page.title, operator)),
                TitleSource.PAGE_TITLE,
                "page title: ${page.title.trim()} (${page.source})",
            )
        }

        val renames = mutableListOf<RenamedExample>()
        bindings.methods
            .filter { it.disposition != BindingDisposition.INDEPENDENT }
            .forEach { method ->
                val resource = method.resource
                val renameFixture =
                    resource != null && ordinal.matches(resource.substringAfterLast('/'))
                val renameMethod = ordinalMethod.matches(method.method)
                if (!renameFixture && !renameMethod) return@forEach
                if (resource != null && renameFixture != renameMethod) {
                    problems +=
                        "Method and fixture naming disagree for ${method.type}#${method.method}"
                    return@forEach
                }
                val folder = resources(method.type).removePrefix("dev/morphia/test/")
                val operator = folder.substringAfterLast('/')
                val key = resource ?: "$folder/${LegacyBindings.derived(method.method)}"
                val raw = if (resource != null) titles[resource] else method.displayName
                val (stem, source, evidence) = name(key, raw, operator) ?: return@forEach
                renames +=
                    RenamedExample(
                        type = method.type,
                        oldMethod = method.method,
                        newMethod = DescriptiveNames.method(stem),
                        oldResource = resource,
                        newResource = resource?.let { "$folder/$stem" },
                        displayTitle = raw,
                        titleSource = source,
                        evidence = evidence,
                    )
            }
        bindings.unboundResources.forEach { unbound ->
            if (!ordinal.matches(unbound.resource.substringAfterLast('/'))) return@forEach
            val folder = unbound.resource.substringBeforeLast('/')
            val raw = titles[unbound.resource]
            val (stem, source, evidence) =
                name(unbound.resource, raw, folder.substringAfterLast('/')) ?: return@forEach
            renames +=
                RenamedExample(
                    null,
                    null,
                    null,
                    unbound.resource,
                    "$folder/$stem",
                    raw,
                    source,
                    evidence,
                )
        }

        // Collisions are checked against every surviving name, case-folded for folders.
        val renamedResources = renames.mapNotNull { it.oldResource }.toSet()
        val renamedMethods = renames.mapNotNull { m -> m.type?.let { it to m.oldMethod } }.toSet()
        val folders =
            (titles.keys.filter { it !in renamedResources } + renames.mapNotNull { it.newResource })
                .groupBy { it.lowercase(Locale.ROOT) }
        folders
            .filterValues { it.size > 1 }
            .forEach { (_, paths) ->
                problems += "Fixture name collision: ${paths.sorted().joinToString()}"
            }
        val methods =
            bindings.methods
                .filter { (it.type to it.method) !in renamedMethods }
                .map { it.type to it.method } +
                renames.mapNotNull { r -> r.type?.let { it to r.newMethod!! } }
        methods
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
            .forEach { (type, method) -> problems += "Method name collision: $type#$method" }
        renames.forEach { r ->
            if (r.newMethod != null && r.newMethod in inherited) {
                problems += "${r.type}#${r.newMethod} would hide an inherited method"
            }
            r.newResource?.let {
                try {
                    DescriptiveNames.resourcePath(
                        "core/src/test/resources/dev/morphia/test/" + it.substringBeforeLast('/'),
                        it.substringAfterLast('/'),
                    )
                } catch (e: IllegalArgumentException) {
                    problems += "${e.message}: $it"
                }
            }
        }
        (pages.keys - usedPages).forEach { problems += "Unused page title for $it" }
        (reviewed.keys - usedReviews).forEach { problems += "Unused reviewed stem for $it" }
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return NameBackportLedger(
            repositoryRevision = bindings.repositoryRevision,
            renames =
                renames.sortedWith(
                    compareBy({ it.oldResource ?: "" }, { it.type ?: "" }, { it.oldMethod ?: "" })
                ),
        )
    }

    /**
     * Applies a planned ledger to a working tree: moves each fixture directory, renames each method
     * declaration and rewrites its "test data" Javadoc line. Every edit is checked before any file
     * changes, so a mismatch leaves the tree untouched.
     */
    fun apply(repository: Path, ledger: NameBackportLedger) {
        val root = repository.toRealPath()
        val resources = root.resolve("core/src/test/resources/dev/morphia/test")
        val sources = root.resolve("core/src/test/java")
        val moves =
            ledger.renames
                .filter { it.oldResource != null }
                .map { resources.resolve(it.oldResource!!) to resources.resolve(it.newResource!!) }
        moves.forEach { (from, to) ->
            require(from.isDirectory()) { "Missing fixture directory $from" }
            require(!to.exists()) { "Fixture destination already exists: $to" }
        }
        val edits =
            ledger.renames
                .filter { it.type != null }
                .groupBy { it.type!! }
                .map { (type, renames) ->
                    val file = sources.resolve(type.replace('.', '/') + ".java")
                    val original = Files.readString(file)
                    var text = original
                    renames.forEach { rename -> text = renameMethod(text, type, rename) }
                    file to text
                }
        moves.forEach { (from, to) -> Files.move(from, to) }
        edits.forEach { (file, text) -> Files.writeString(file, text) }
    }

    private fun renameMethod(text: String, type: String, rename: RenamedExample): String {
        val old = rename.oldMethod!!
        val declaration = Regex("""(\bvoid\s+)${Regex.escape(old)}(\s*\()""")
        val matches = declaration.findAll(text).toList()
        require(matches.size == 1) {
            "Expected one declaration of $type#$old, found ${matches.size}"
        }
        val match = matches.single()
        require(
            !Regex("""\b${Regex.escape(old)}\b""").containsMatchIn(text.removeRange(match.range))
        ) {
            "$type#$old is referenced outside its declaration"
        }
        var result =
            text.replaceRange(
                match.range,
                match.groupValues[1] + rename.newMethod + match.groupValues[2],
            )
        val newResource = rename.newResource ?: return result
        // The Javadoc, when present, is followed only by annotations and modifiers.
        val head = result.substring(0, match.range.first)
        val docEnd = head.lastIndexOf("*/")
        val docStart = head.lastIndexOf("/**")
        if (docStart >= 0 && docEnd > docStart && between.matches(head.substring(docEnd + 2))) {
            val doc = head.substring(docStart, docEnd)
            val updated =
                doc.replace(Regex("""test data: dev/morphia/test/\S+""")) {
                    "test data: dev/morphia/test/$newResource"
                }
            result = result.replaceRange(docStart, docEnd, updated)
        }
        return result
    }

    /**
     * The legacy catalog as it reads after the ledger's renames; bodies and evidence are unchanged.
     */
    fun project(
        legacy: LegacyBindingCatalog,
        ledger: NameBackportLedger,
        revision: String,
    ): LegacyBindingCatalog {
        val methods =
            ledger.renames.filter { it.type != null }.associateBy { it.type to it.oldMethod }
        val fixtures =
            ledger.renames.filter { it.oldResource != null }.associateBy { it.oldResource }
        // Every rename must name an entry of the pre-backport import; otherwise the snapshot was
        // replaced.
        val known = legacy.methods.map { it.type to it.method }.toSet()
        val resources =
            (legacy.methods.mapNotNull { it.resource } +
                    legacy.unboundResources.map { it.resource })
                .toSet()
        val unmatched =
            methods.keys.filter { it !in known }.map { "${it.first}#${it.second}" } +
                fixtures.keys.filter { it !in resources }
        require(unmatched.isEmpty()) { "Ledger renames missing from the legacy import: $unmatched" }
        return legacy.copy(
            repositoryRevision = revision,
            methods =
                legacy.methods
                    .map { method ->
                        val rename = methods[method.type to method.method]
                        method.copy(
                            method = rename?.newMethod ?: method.method,
                            resource = method.resource?.let { fixtures[it]?.newResource ?: it },
                        )
                    }
                    .sortedWith(compareBy({ it.type }, { it.method })),
            unboundResources =
                legacy.unboundResources
                    .map { it.copy(resource = fixtures[it.resource]?.newResource ?: it.resource) }
                    .sortedBy { it.resource },
        )
    }

    /** The reviewed binding policy with its entries renamed alongside the tests and fixtures. */
    fun project(policy: BindingPolicy, ledger: NameBackportLedger): BindingPolicy {
        val methods =
            ledger.renames.filter { it.type != null }.associateBy { it.type to it.oldMethod }
        val fixtures =
            ledger.renames.filter { it.oldResource != null }.associateBy { it.oldResource }
        return policy.copy(
            unboundResources =
                policy.unboundResources.map {
                    it.copy(resource = fixtures[it.resource]?.newResource ?: it.resource)
                },
            manualMethods =
                policy.manualMethods.map {
                    it.copy(method = methods[it.type to it.method]?.newMethod ?: it.method)
                },
        )
    }

    /**
     * Checks a migrated tree: every preserved fixture file has its original bytes at its new path,
     * and no ordinal method, fixture directory or "test data" reference remains in scope.
     */
    fun verify(repository: Path, corpus: CorpusInventory, ledger: NameBackportLedger) {
        val root = repository.toRealPath()
        val prefix = "core/src/test/resources/dev/morphia/test/"
        val moved =
            ledger.renames
                .filter { it.oldResource != null }
                .associate { prefix + it.oldResource to prefix + it.newResource }
        val problems = mutableListOf<String>()
        corpus.examples.forEach { example ->
            val target = moved[example.path] ?: example.path
            example.files.forEach { file ->
                val path = root.resolve(target + file.path.removePrefix(example.path))
                if (!Files.isRegularFile(path)) {
                    problems += "Missing preserved fixture file $path"
                } else if (sha256(Files.readAllBytes(path)) != file.sha256) {
                    problems += "Fixture bytes changed: $path"
                }
            }
            if (target != example.path && root.resolve(example.path).exists()) {
                problems += "Legacy fixture directory still present: ${example.path}"
            }
        }
        LegacyBindings.packages.forEach { category ->
            val resources = root.resolve(prefix + category)
            Files.walk(resources).use { paths ->
                paths
                    .filter { it.isDirectory() && ordinal.matches(it.fileName.toString()) }
                    .forEach {
                        problems += "Ordinal fixture directory remains: ${root.relativize(it)}"
                    }
            }
            val sources = root.resolve("core/src/test/java/dev/morphia/test/$category")
            Files.list(sources).use { paths ->
                paths
                    .filter { it.toString().endsWith(".java") }
                    .forEach { file ->
                        val text = Files.readString(file)
                        if (
                            Regex("""\btestExample[0-9]+\b|/example[0-9]+\b""")
                                .containsMatchIn(text)
                        ) {
                            problems += "Ordinal name remains in ${root.relativize(file)}"
                        }
                        val type =
                            "dev.morphia.test.${category.replace('/', '.')}." +
                                file.fileName.toString().removeSuffix(".java")
                        if (
                            testData.findAll(text).count() !=
                                Regex("test data:").findAll(text).count()
                        ) {
                            problems += "Unparsed test data Javadoc in ${root.relativize(file)}"
                        }
                        testData.findAll(text).forEach { doc ->
                            val expected =
                                resources(type) + "/" + LegacyBindings.derived(doc.groupValues[2])
                            if (doc.groupValues[1] != expected) {
                                problems +=
                                    "${root.relativize(file)}: ${doc.groupValues[2]} documents " +
                                        "${doc.groupValues[1]}, not $expected"
                            }
                        }
                    }
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }

    /**
     * Method names declared by the template base classes, which descriptive tests must not hide.
     */
    fun inheritedMethods(repository: Path): Set<String> =
        listOf("TemplatedTestBase.java", "TestBase.java").flatMapTo(sortedSetOf()) { name ->
            val text =
                Files.readString(repository.resolve("core/src/test/java/dev/morphia/test/$name"))
            Regex(
                    """(?m)^\s*(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:<[^>]+>\s*)?[\w.<>\[\], ?]+\s+(\w+)\s*\("""
                )
                .findAll(text)
                .map { it.groupValues[1] }
                .filter { it !in setOf("if", "for", "while", "switch", "return", "new", "catch") }
                .toList()
        }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
