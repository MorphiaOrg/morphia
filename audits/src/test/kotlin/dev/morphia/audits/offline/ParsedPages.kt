package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.SourceChannel
import dev.morphia.audits.coverage.SourceRole
import dev.morphia.audits.documents.DocumentNode
import dev.morphia.audits.documents.DocumentOrigin
import dev.morphia.audits.documents.DocumentPage
import dev.morphia.audits.documents.DocumentParser
import dev.morphia.audits.documents.DocumentParsing
import dev.morphia.audits.documents.DocumentPrecision
import dev.morphia.audits.documents.DocumentReport
import dev.morphia.audits.documents.DocumentState
import dev.morphia.audits.sources.GitResource
import dev.morphia.audits.sources.GitResourceKind
import dev.morphia.audits.sources.GitSnapshot
import dev.morphia.audits.sources.GitSourceManifest
import dev.morphia.audits.sources.GitSourceRequest
import dev.morphia.audits.sources.SourceBundle
import dev.morphia.audits.sources.SourceState
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Synthetic parsed manual pages in a verified source bundle, as `DocumentsMainKt` reports real
 * ones: each page is a tree of sections and code blocks, optionally declaring an operator.
 */
class ParsedPages(private val directory: Path, val commit: String = "c".repeat(40)) {
    private val pages = linkedMapOf<String, (Page) -> DocumentNode>()

    /** Builds one page's nodes, each carrying the page's origin. */
    class Page(val origin: DocumentOrigin) {
        fun node(
            kind: String,
            text: String? = null,
            name: String? = null,
            domain: String? = null,
            children: List<DocumentNode> = emptyList(),
            options: Map<String, String> = emptyMap(),
            attributes: Map<String, String> = emptyMap(),
        ) =
            DocumentNode(
                kind,
                origin,
                text,
                name,
                domain,
                emptyList(),
                options,
                attributes,
                emptyList(),
                children,
            )

        fun section(title: String, vararg children: DocumentNode) =
            node(
                "section",
                children =
                    listOf(
                        node(
                            "heading",
                            children = listOf(node("text", text = title)),
                            attributes = mapOf("id" to json(anchor(title))),
                        )
                    ) + children,
            )

        fun code(source: String, language: String = "javascript") =
            node("code", text = source.trimIndent(), attributes = mapOf("lang" to json(language)))

        /** The `.. expression:: $token` declaration of an operator page. */
        fun target(kind: String, token: String) =
            node(
                "target",
                name = kind,
                domain = "mongodb",
                children =
                    listOf(node("target_identifier", children = listOf(node("text", text = token)))),
            )

        private fun anchor(title: String) =
            title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

        private fun json(value: String) = AuditJson.write(value).trim()
    }

    /** Adds the page `reference/operator/<folder>/<name>.txt` built by [tree]. */
    fun page(folder: String, name: String, tree: Page.() -> DocumentNode) {
        pages["reference/operator/$folder/$name.txt"] = { it.tree() }
    }

    /** An operator page: its title section holds the declaration and then [sections]. */
    fun operator(
        name: String,
        kind: String,
        folder: String = "aggregation",
        sections: Page.() -> List<DocumentNode>,
    ) =
        page(folder, name) {
            node(
                "root",
                children =
                    listOf(
                        section(
                            name,
                            *(listOf(target(kind, "\$$name")) + sections()).toTypedArray(),
                        )
                    ),
            )
        }

    fun build(): Pair<VerifiedSourceBundle, DocumentReport> {
        val bundleDirectory = Files.createTempDirectory(directory, "bundle-")
        Files.createDirectory(bundleDirectory.resolve("blobs"))
        val contents =
            pages.keys.associate { "$ROOT/source/$it" to "Synthetic page $it\n" } +
                mapOf("$ROOT/snooty.toml" to "name='fixture'\n")
        val resources =
            contents.map { (path, text) ->
                val content = text.toByteArray()
                val digest = sha(content)
                Files.write(bundleDirectory.resolve("blobs/$digest"), content)
                val git = MessageDigest.getInstance("SHA-1")
                git.update("blob ${content.size}\u0000".toByteArray(Charsets.US_ASCII))
                git.update(content)
                GitResource(
                    path,
                    GitResourceKind.FILE,
                    HexFormat.of().formatHex(git.digest()),
                    digest,
                    content.size.toLong(),
                    null,
                )
            }
        val request =
            GitSourceRequest(
                id = "fixture",
                uri = "https://example.invalid/docs.git",
                revision = commit,
                projectRoot = ROOT,
                label = "8.0",
                role = SourceRole.CURRENT,
                channel = SourceChannel.RELEASED,
                roots = listOf(ROOT),
                allowedSymlinkRoots = emptyList(),
                maximumFiles = 1000,
                maximumFileBytes = 262144,
                maximumTotalBytes = 1048576,
                timeoutSeconds = 10,
            )
        val bundle =
            VerifiedSourceBundle(
                bundleDirectory,
                GitSourceManifest(
                    request = request,
                    tree = "0".repeat(40),
                    gitVersion = "fixture",
                    contentSha256 = GitSnapshot.contentHash(request, "0".repeat(40), resources),
                    state = SourceState.READY,
                    resources = resources,
                    problems = emptyList(),
                ),
            )
        Files.writeString(
            bundleDirectory.resolve(SourceBundle.MANIFEST),
            AuditJson.write(bundle.manifest),
        )
        val verified = SourceBundle.open(bundleDirectory, bundle.manifest.contentSha256)
        val parsed =
            pages.map { (id, tree) ->
                val path = "$ROOT/source/$id"
                val origin =
                    DocumentOrigin(
                        path,
                        resources.single { it.path == path }.sha256,
                        1,
                        DocumentPrecision.PARSER_REPORTED,
                        listOf(path),
                    )
                val root = tree(Page(origin))
                DocumentPage(id, root, root, emptyList(), true)
            }
        val report =
            DocumentReport(
                sourceSha256 = verified.manifest.contentSha256,
                inputSha256 = AuditJson.hash(DocumentParsing.export(verified, pages.keys.toList())),
                parser =
                    DocumentParser(
                        "snooty",
                        DocumentParsing.PARSER_REVISION,
                        "fixture",
                        "3.12.16",
                        "d".repeat(64),
                        "e".repeat(64),
                        emptyMap(),
                    ),
                state = DocumentState.READY,
                pages = parsed,
                diagnostics = emptyList(),
                unresolvedUrls = emptyList(),
            )
        return verified to report
    }

    companion object {
        const val ROOT = "content/manual/manual"

        private fun sha(bytes: ByteArray) =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }
}
