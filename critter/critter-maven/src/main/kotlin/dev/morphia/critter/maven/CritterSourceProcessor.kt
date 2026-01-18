package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import dev.morphia.mapping.Mapper
import java.io.File
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaInterfaceSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Processes source files and generates critter source code. Based on the reference implementation
 * from the critter project's generator module.
 */
class CritterSourceProcessor(
    private val sourceDirectories: List<File>,
    private val outputDirectory: File,
    private val resourceOutputDirectory: File,
    private val packages: List<String>,
    private val criteriaPackage: String?,
    private val format: Boolean,
    private val config: MorphiaConfig,
) {

    private val logger: Logger = LoggerFactory.getLogger(CritterSourceProcessor::class.java)
    private val classes: MutableMap<String, JavaSource<*>> = mutableMapOf()

    fun process() {
        // Scan source directories for Java files
        for (directory in sourceDirectories) {
            logger.info("Scanning source directory: ${directory.absolutePath}")
            scanDirectory(directory)
        }

        if (classes.isEmpty()) {
            logger.info("No source files found")
            return
        }

        logger.info("Found ${classes.size} source files")
        logger.info("Using discriminator function: ${config.discriminator()}")
        logger.info("Using collection naming: ${config.collectionNaming()}")
        logger.info("Using property naming: ${config.propertyNaming()}")

        // Find entities (classes annotated with @Entity or other mapping annotations)
        val entities = findEntities()

        if (entities.isEmpty()) {
            logger.info("No @Entity classes found in sources")
            return
        }

        logger.info("Found ${entities.size} @Entity classes")

        // Ensure output directories exist
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        if (!resourceOutputDirectory.exists()) {
            resourceOutputDirectory.mkdirs()
        }

        // Process each entity
        for ((name, source) in entities) {
            processEntity(name, source)
        }

        logger.info("Generated sources to ${outputDirectory.absolutePath}")
    }

    private fun scanDirectory(directory: File) {
        val packagesToScan =
            packages.ifEmpty {
                // Use packages from config if none specified
                config.packages().filter { it != ".*" }
            }

        directory
            .walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .filter { file ->
                // Filter by packages if specified
                if (packagesToScan.isEmpty()) {
                    true
                } else {
                    packagesToScan.any { pkg ->
                        val relativePath = file.relativeTo(directory).path
                        val packagePath =
                            relativePath
                                .replace(File.separatorChar, '.')
                                .removeSuffix(".java")
                                .substringBeforeLast('.')
                        packagePath.startsWith(pkg.removeSuffix(".*"))
                    }
                }
            }
            .forEach { file ->
                try {
                    val source = Roaster.parse(file)
                    if (source is JavaClassSource || source is JavaInterfaceSource) {
                        val qualifiedName = (source as JavaSource<*>).qualifiedName
                        classes[qualifiedName] = source
                        logger.debug("Scanned: $qualifiedName")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse ${file.absolutePath}: ${e.message}")
                }
            }
    }

    private fun findEntities(): Map<String, JavaSource<*>> {
        return classes.filter { (_, source) -> hasEntityAnnotation(source) }.toSortedMap()
    }

    private fun hasEntityAnnotation(source: JavaSource<*>): Boolean {
        val annotationNames = Mapper.MAPPING_ANNOTATIONS.map { it.simpleName }
        return when (source) {
            is JavaClassSource -> source.annotations.any { it.name in annotationNames }
            is JavaInterfaceSource -> source.annotations.any { it.name in annotationNames }
            else -> false
        }
    }

    private fun processEntity(name: String, source: JavaSource<*>) {
        logger.info("Processing entity: $name")
        // TODO: Implement source code generation based on the reference implementation
        // This would involve:
        // 1. Creating codec classes
        // 2. Creating criteria builders
        // 3. Creating instance creators
        // For now, this is a placeholder that can be expanded based on the
        // reference implementation at ~/dev/morphia-dev/critter/generator
    }

    private fun formatSource(sourceFile: File) {
        if (format && sourceFile.exists()) {
            try {
                val parsed = Roaster.parse(JavaClassSource::class.java, sourceFile)
                sourceFile.writeText(parsed.toString())
            } catch (e: Exception) {
                logger.warn("Failed to format ${sourceFile.absolutePath}: ${e.message}")
            }
        }
    }

    fun generateServiceLoader(model: Class<*>, impl: String) {
        val serviceFile = File(resourceOutputDirectory, "META-INF/services/${model.name}")
        serviceFile.parentFile.mkdirs()
        serviceFile.writeText(impl + "\n")
        logger.debug("Generated service loader: ${serviceFile.absolutePath}")
    }
}
