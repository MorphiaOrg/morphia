package dev.morphia.critter.maven

import dev.morphia.annotations.Entity
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.Generators
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator
import io.github.classgraph.ClassGraph
import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Processes compiled .class files and generates critter bytecode. */
class CritterProcessor(
    private val classesDirectory: File,
    private val outputDirectory: File,
    private val packages: List<String>,
    private val classLoader: ClassLoader,
    private val config: MorphiaConfig,
) {

    private val logger: Logger = LoggerFactory.getLogger(CritterProcessor::class.java)
    private val critterClassLoader = CritterClassLoader()

    fun process() {
        // Configure Generators with the loaded MorphiaConfig
        Generators.configFile = findConfigFile()

        val entityClasses = findEntityClasses()

        if (entityClasses.isEmpty()) {
            logger.info("No @Entity classes found in ${classesDirectory.absolutePath}")
            return
        }

        logger.info("Found ${entityClasses.size} @Entity classes")
        logger.info("Using discriminator function: ${config.discriminator()}")
        logger.info("Using collection naming: ${config.collectionNaming()}")

        for (entityClass in entityClasses) {
            processClass(entityClass)
        }

        writeGeneratedClasses()
    }

    private fun findConfigFile(): String {
        // Check if morphia-config.properties exists in the classes directory
        val configFile = File(classesDirectory, "META-INF/morphia-config.properties")
        return if (configFile.exists()) {
            "META-INF/morphia-config.properties"
        } else {
            // Return default location - Generators will use defaults if not found
            "META-INF/morphia-config.properties"
        }
    }

    private fun findEntityClasses(): List<Class<*>> {
        val packagesToScan =
            packages.ifEmpty {
                // Use packages from config if none specified
                config.packages().filter { it != ".*" }
            }

        val scanResult =
            ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .overrideClassLoaders(classLoader)
                .apply {
                    if (packagesToScan.isNotEmpty()) {
                        acceptPackages(*packagesToScan.toTypedArray())
                    }
                }
                .scan()

        return scanResult.use { result ->
            result
                .getClassesWithAnnotation(Entity::class.java)
                .filter { !it.isAbstract && !it.isInterface }
                .map { classLoader.loadClass(it.name) }
        }
    }

    private fun processClass(entityClass: Class<*>) {
        logger.info("Generating critter code for: ${entityClass.name}")
        CritterGizmoGenerator.generate(entityClass, critterClassLoader)
    }

    private fun writeGeneratedClasses() {
        val classesField =
            critterClassLoader.javaClass.superclass.getDeclaredField("typeDefinitions")
        classesField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val typeDefinitions = classesField.get(critterClassLoader) as Map<String, ByteArray>

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        for ((className, bytecode) in typeDefinitions) {
            val classFile = File(outputDirectory, className.replace('.', '/') + ".class")
            classFile.parentFile.mkdirs()
            classFile.writeBytes(bytecode)
            logger.debug("Wrote: ${classFile.absolutePath}")
        }

        logger.info("Generated ${typeDefinitions.size} classes to ${outputDirectory.absolutePath}")
    }
}
