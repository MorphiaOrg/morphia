package dev.morphia.audits

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class CorpusFile(val path: String, val sha256: String, val size: Long)

data class LegacyExample(
    val path: String,
    val displayName: String?,
    val files: List<CorpusFile>,
    val markers: Map<String, String>,
)

data class LegacyExclusion(val file: CorpusFile, val reason: String)

/** A preservation baseline, not a claim that any of these examples or APIs are supported. */
data class CorpusInventory(
    val schemaVersion: Int = 1,
    val repositoryRevision: String,
    val examples: List<LegacyExample>,
    val exclusions: List<LegacyExclusion>,
    val testSources: List<CorpusFile>,
    val additionalResources: List<CorpusFile>,
) {
    fun toJson(): String = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(this) + "\n"

    companion object {
        private val JSON =
            JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build()

        private val categories =
            listOf(
                "aggregation/expressions",
                "aggregation/stages",
                "query/filters",
                "query/updates",
            )

        fun capture(repository: Path, revision: String): CorpusInventory {
            require(revision.isNotBlank()) { "A repository revision is required" }
            val root = repository.toRealPath()
            val resources = root.resolve("core/src/test/resources/dev/morphia/test")
            val sources = root.resolve("core/src/test/java/dev/morphia/test")
            val examples = mutableListOf<LegacyExample>()
            val exclusions = mutableListOf<LegacyExclusion>()
            categories.forEach { category ->
                val directory = resources.resolve(category)
                requireDirectory(directory)
                requireDirectory(sources.resolve(category))
                children(directory)
                    .filter { it.isDirectory() }
                    .forEach { operator ->
                        val ignored = operator.resolve("ignored")
                        if (ignored.isRegularFile()) {
                            requireRegularFile(ignored)
                            exclusions +=
                                LegacyExclusion(describe(root, ignored), ignored.readText())
                        }
                        children(operator)
                            .filter { it.isDirectory() }
                            .forEach { example ->
                                val files = regularFiles(example)
                                val name = example.resolve("name")
                                val displayName =
                                    if (name in files) name.readText().lineSequence().firstOrNull()
                                    else null
                                examples +=
                                    LegacyExample(
                                        relative(root, example),
                                        displayName,
                                        files.map { describe(root, it) },
                                        files
                                            .filter {
                                                it.parent == example && it.name.startsWith("lock")
                                            }
                                            .associate { it.name to it.readText() },
                                    )
                            }
                    }
            }
            val covered =
                examples.flatMap { it.files }.map { it.path }.toSet() +
                    exclusions.map { it.file.path }
            val additional =
                listOf("aggregation", "query").flatMap { family ->
                    regularFiles(resources.resolve(family))
                        .filter { relative(root, it) !in covered }
                        .map { describe(root, it) }
                }
            val testSources =
                regularFiles(sources)
                    .filter { it.name.endsWith(".java") }
                    .map { describe(root, it) }
            return CorpusInventory(
                repositoryRevision = revision,
                examples = examples.sortedBy { it.path },
                exclusions = exclusions.sortedBy { it.file.path },
                testSources = testSources.sortedBy { it.path },
                additionalResources = additional.sortedBy { it.path },
            )
        }

        private fun children(directory: Path): List<Path> {
            requireDirectory(directory)
            return Files.list(directory).use { entries ->
                entries.sorted().toList().onEach {
                    require(!Files.isSymbolicLink(it)) {
                        "Corpus entries must not be symlinks: $it"
                    }
                }
            }
        }

        private fun regularFiles(directory: Path): List<Path> {
            requireDirectory(directory)
            return Files.walk(directory).use { entries ->
                entries
                    .sorted()
                    .toList()
                    .onEach {
                        require(!Files.isSymbolicLink(it)) {
                            "Corpus entries must not be symlinks: $it"
                        }
                    }
                    .filter { it.isRegularFile() }
            }
        }

        private fun requireDirectory(directory: Path) {
            require(directory.isDirectory()) { "Missing corpus directory: $directory" }
            require(directory.toRealPath() == directory) {
                "Corpus directories must not contain symlinks: $directory"
            }
        }

        private fun describe(root: Path, path: Path): CorpusFile {
            requireRegularFile(path)
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return CorpusFile(
                relative(root, path),
                HexFormat.of().formatHex(digest.digest()),
                Files.size(path),
            )
        }

        private fun requireRegularFile(path: Path) {
            require(!Files.isSymbolicLink(path) && path.isRegularFile()) {
                "Not a regular corpus file: $path"
            }
        }

        private fun relative(root: Path, path: Path): String =
            root.relativize(path).joinToString("/")
    }
}
