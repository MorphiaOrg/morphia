package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import dev.morphia.config.MorphiaConfigHelper.MORPHIA_CONFIG_PROPERTIES
import java.io.File
import java.net.URLClassLoader
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
@Mojo(name = "sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterSourcesMojo : AbstractMojo() {

    private val logger: Logger = LoggerFactory.getLogger(CritterSourcesMojo::class.java)

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter(property = "critter.packages") private var packages: List<String> = emptyList()

    @Parameter(
        property = "critter.sourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-sources/critter",
    )
    private lateinit var sourceOutputDirectory: File

    @Parameter(
        property = "critter.resourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-resources/critter",
    )
    private lateinit var resourceOutputDirectory: File

    @Parameter(property = "critter.includeTestSources", defaultValue = "false")
    private var includeTestSources: Boolean = false

    @Parameter(property = "critter.criteriaPackage") private var criteriaPackage: String? = null

    @Parameter(property = "critter.format", defaultValue = "true")
    private var format: Boolean = true

    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            val sourceDirectories =
                if (includeTestSources) {
                    project.testCompileSourceRoots.map { File(it) }
                } else {
                    project.compileSourceRoots.map { File(it) }
                }

            val existingDirectories = sourceDirectories.filter { it.exists() }

            if (existingDirectories.isEmpty()) {
                logger.info("No source directories found")
                return
            }

            val config = loadMorphiaConfig()
            val processor =
                CritterSourceProcessor(
                    sourceDirectories = existingDirectories,
                    outputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    packages = packages,
                    criteriaPackage = criteriaPackage,
                    format = format,
                    config = config,
                )

            processor.process()

            // Add generated sources to compile source roots
            if (!includeTestSources) {
                project.addCompileSourceRoot(sourceOutputDirectory.absolutePath)
                logger.info("Added ${sourceOutputDirectory.absolutePath} to compile source roots")
            } else {
                project.addTestCompileSourceRoot(sourceOutputDirectory.absolutePath)
                logger.info(
                    "Added ${sourceOutputDirectory.absolutePath} to test compile source roots"
                )
            }
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to process source files", e)
        }
    }

    private fun loadMorphiaConfig(): MorphiaConfig {
        val resourcesDir =
            if (includeTestSources) {
                project.testResources.firstOrNull()?.let { File(it.directory) }
            } else {
                project.resources.firstOrNull()?.let { File(it.directory) }
            }

        val configFile = resourcesDir?.let { File(it, MORPHIA_CONFIG_PROPERTIES) }

        return if (configFile?.exists() == true) {
            logger.info("Loading morphia config from: ${configFile.absolutePath}")
            // Load config by adding the resources dir to classpath temporarily
            val urls = arrayOf(resourcesDir.toURI().toURL())
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
