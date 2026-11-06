package dev.morphia.critter.maven

import dev.morphia.annotations.Entity
import dev.morphia.annotations.ExternalEntity
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.generator.CritterGenerator
import dev.morphia.mapping.ReflectiveMapper
import dev.morphia.mapping.codec.pojo.ClassHierarchy
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Processes compiled .class files and generates critter bytecode. */
class CritterProcessor(
    private val classesDirectory: File,
    private val outputDirectory: File,
    private val classLoader: ClassLoader,
    private val config: MorphiaConfig,
) {

    private val logger: Logger = LoggerFactory.getLogger(CritterProcessor::class.java)
    private val critterClassLoader = CritterClassLoader()
    private val generator = CritterGenerator(ReflectiveMapper(config, critterClassLoader))

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

    internal fun findEntityClasses(): List<Class<*>> {
        val packagesToScan = config.packages().filter { it != ".*" }

        val scanResult =
            ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .enableClassLoaders(classLoader)
                // interfaces outside the scanned packages can carry @Entity
                .enableExternalClasses()
                .apply {
                    if (packagesToScan.isNotEmpty()) {
                        acceptPackages(*packagesToScan.toTypedArray())
                    }
                }
                .scan()

        return scanResult.use { result ->
            result.allClasses
                .filter { mayBeEntity(it) }
                .map { classLoader.loadClass(it.name) }
                .filter { isEntity(it) }
        }
    }

    /**
     * Whether the class or an interface it implements carries `@Entity`, as the runtime package
     * scan checks before loading a class.
     */
    private fun mayBeEntity(type: ClassInfo): Boolean =
        !type.isExternalClass &&
            !type.isInterface &&
            !type.isAnnotation &&
            (type.hasAnnotation(ENTITY) || type.allSuperinterfaces.any { it.hasAnnotation(ENTITY) })

    /**
     * Whether the mappers treat the class as an entity: it collects `@Entity` from itself, a
     * superclass or an interface. An `@ExternalEntity` stand-in is left out: its model is named
     * after its target, so it would replace the target's model, and stand-ins are generated at
     * runtime.
     */
    private fun isEntity(type: Class<*>): Boolean {
        val annotations = ClassHierarchy.annotations(type)
        return Entity::class.java in annotations && ExternalEntity::class.java !in annotations
    }

    private fun processClass(entityClass: Class<*>) {
        logger.info("Generating critter code for: ${entityClass.name}")
        try {
            generator.generate(entityClass, critterClassLoader, false)
        } catch (e: Exception) {
            logger.warn("Skipping ${entityClass.name}: ${e.message}", e)
        }
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

    private companion object {
        val ENTITY: String = Entity::class.java.name
    }
}
