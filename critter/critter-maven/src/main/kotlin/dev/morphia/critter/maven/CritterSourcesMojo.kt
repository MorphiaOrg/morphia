package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.streams.asSequence
import org.apache.maven.api.Language
import org.apache.maven.api.ProjectScope.MAIN
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Mojo that processes source files and generates critter source code. Bound to the generate-sources
 * phase by default.
 */
@Mojo(name = "generate-criteria", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterSourcesMojo : AbstractMojo() {

    private val logger: Logger = LoggerFactory.getLogger(CritterSourcesMojo::class.java)

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter(
        property = "critter.sourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-sources/critter",
        readonly = true,
    )
    private lateinit var sourceOutputDirectory: File

    @Parameter(
        property = "critter.resourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-resources/critter",
        readonly = true,
    )
    private lateinit var resourceOutputDirectory: File

    //    @Parameter(property = "critter.format", defaultValue = "true")
    private var format: Boolean = true

    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            val sourceDirectories =
                project.getEnabledSourceRoots(MAIN, Language.JAVA_FAMILY).map {
                    it.directory().toFile()
                }

            val existingDirectories = sourceDirectories.filter { it.exists() }.toList()

            if (existingDirectories.isEmpty()) {
                logger.info("No source directories found")
                return
            }
            val processor =
                CritterSourceProcessor(
                    sourceDirectories = existingDirectories,
                    outputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    format = format,
                    config = loadMorphiaConfig(),
                )

            processor.process()

            // Add generated sources to compile source roots
            project.addCompileSourceRoot(sourceOutputDirectory.absolutePath)
            logger.info("Added ${sourceOutputDirectory.absolutePath} to compile source roots")
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to process source files", e)
        }
    }

    private fun loadMorphiaConfig(): MorphiaConfig {
        val configFile =
            project
                .getEnabledSourceRoots(MAIN, null)
                .asSequence()
                .mapNotNull { it.directory().resolve("resources/morphia-config.properties") }
                .firstOrNull { path: Path -> path.exists() }

        return if (configFile?.exists() == true) {
            logger.info("Loading morphia config from: ${configFile.toFile().absolutePath}")
            // Load config by adding the resources dir to classpath temporarily
            val urls = arrayOf(configFile.toUri().toURL())
            val tempClassLoader = URLClassLoader(urls, Thread.currentThread().contextClassLoader)
            val originalClassLoader = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().contextClassLoader = tempClassLoader
                MorphiaConfig.load()
            } finally {
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        } else {
            logger.info("No morphia-config.properties found, using default configuration")
            MorphiaConfig.load() // Returns default config when file not found
        }
    }
}
