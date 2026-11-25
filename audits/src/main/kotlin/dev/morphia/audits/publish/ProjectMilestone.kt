package dev.morphia.audits.publish

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** The milestone audit issues join: the release the root pom's version is building. */
object ProjectMilestone {
    private val RELEASE = Regex("""\d+\.\d+\.\d+""")

    /** `3.0.1-SNAPSHOT` in [pom]'s own `project/version` gives `3.0.1`. */
    fun of(pom: Path): String {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val project = Files.newInputStream(pom).use { factory.newDocumentBuilder().parse(it) }
        // The project's own version, not its parent's or a dependency's.
        val version =
            project.documentElement.childNodes
                .let { nodes -> (0 until nodes.length).map { nodes.item(it) } }
                .filterIsInstance<Element>()
                .singleOrNull { it.tagName == "version" }
                ?.textContent
                ?.trim() ?: throw IllegalArgumentException("$pom has no project version")
        val release = version.removeSuffix("-SNAPSHOT")
        require(RELEASE.matches(release)) { "$pom's version $version is not major.minor.patch" }
        return release
    }
}
