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
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Mojo that processes compiled .class files and generates critter code. Bound to the
 * process-classes phase by default.
 */
@Mojo(
    name = "generate-models",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.TEST,
)
class CritterMojo : AbstractMojo() {

    private val logger: Logger = LoggerFactory.getLogger(CritterMojo::class.java)

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter(
        property = "critter.outputDirectory",
        defaultValue = "\${project.build.directory}/generated-classes/critter",
        readonly = true,
    )
    private lateinit var outputDirectory: File

    override fun execute() {
        try {
            val classesDirectory = File(project.build.outputDirectory)

            if (!classesDirectory.exists()) {
                logger.error("Classes directory does not exist: ${classesDirectory.absolutePath}")
                return
            }

            CritterProcessor(
                    classesDirectory = classesDirectory,
                    outputDirectory = outputDirectory,
                    classLoader = buildClassLoader(),
                    config = loadMorphiaConfig(),
                )
                .process()
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to generate critter code", e)
        }
    }

    private fun loadMorphiaConfig(): MorphiaConfig {
        val resourcesDir = File(project.build.outputDirectory)

        val configFile = File(resourcesDir, MORPHIA_CONFIG_PROPERTIES)

        return if (configFile.exists()) {
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

    private fun buildClassLoader(): ClassLoader {
        val urls = project.compileClasspathElements.map { File(it).toURI().toURL() }.toTypedArray()

        return URLClassLoader(urls, Thread.currentThread().contextClassLoader)
    }
}
