package dev.morphia.critter.maven

import dev.morphia.annotations.Entity
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator
import dev.morphia.mapping.ReflectiveMapper
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
    private val gizmoGenerator = CritterGizmoGenerator(ReflectiveMapper(config, critterClassLoader))

    fun process() {
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
        gizmoGenerator.generate(entityClass, critterClassLoader, false)
    }

    private fun writeGeneratedClasses() {
        val typeDefinitions = critterClassLoader.getTypeDefinitions()

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
