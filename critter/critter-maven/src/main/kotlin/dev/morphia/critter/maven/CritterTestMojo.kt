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
 * Mojo that processes compiled test .class files and generates critter models into
 * target/test-classes so they are found on the test classpath as pre-generated models, eliminating
 * per-entity runtime bytecode generation overhead during tests.
 *
 * Bound to the process-test-classes phase by default.
 */
@Mojo(
    name = "generate-test-models",
    defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
    requiresDependencyResolution = ResolutionScope.TEST,
)
class CritterTestMojo : AbstractMojo() {

    private val logger: Logger = LoggerFactory.getLogger(CritterTestMojo::class.java)

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private lateinit var project: MavenProject

    override fun execute() {
        try {
            val testClassesDirectory = File(project.build.testOutputDirectory)

            if (!testClassesDirectory.exists()) {
                logger.warn(
                    "Test classes directory does not exist: ${testClassesDirectory.absolutePath}"
                )
                return
            }

            // Remove stale generated files so skipped entities don't leave orphaned models.
            testClassesDirectory
                .walkTopDown()
                .filter { it.isDirectory && it.name == "__morphia" }
                .forEach { it.deleteRecursively() }

            CritterProcessor(
                    classesDirectory = testClassesDirectory,
                    outputDirectory = testClassesDirectory,
                    classLoader = buildClassLoader(),
                    config = loadMorphiaConfig(),
                )
                .process()
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to generate critter test models", e)
        }
    }

    private fun loadMorphiaConfig(): MorphiaConfig {
        // Prefer config from test resources, fall back to main resources.
        for (dir in listOf(project.build.testOutputDirectory, project.build.outputDirectory)) {
            val configFile = File(dir, MORPHIA_CONFIG_PROPERTIES)
            if (configFile.exists()) {
                logger.info("Loading morphia config from: ${configFile.absolutePath}")
                val urls = arrayOf(File(dir).toURI().toURL())
                val tempClassLoader =
                    URLClassLoader(urls, Thread.currentThread().contextClassLoader)
                val original = Thread.currentThread().contextClassLoader
                return try {
                    Thread.currentThread().contextClassLoader = tempClassLoader
                    MorphiaConfig.load()
                } finally {
                    Thread.currentThread().contextClassLoader = original
                }
            }
        }
        logger.info("No morphia-config.properties found, using default configuration")
        return MorphiaConfig.load()
    }

    private fun buildClassLoader(): ClassLoader {
        val urls =
            buildList {
                    add(File(project.build.testOutputDirectory).toURI().toURL())
                    add(File(project.build.outputDirectory).toURI().toURL())
                    project.artifacts.mapNotNullTo(this) { it.file?.toURI()?.toURL() }
                }
                .toTypedArray()
        return URLClassLoader(urls, Thread.currentThread().contextClassLoader)
    }
}
