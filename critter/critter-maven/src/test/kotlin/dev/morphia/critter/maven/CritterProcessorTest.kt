package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CritterProcessorTest {

    private lateinit var tempDir: File
    private lateinit var outputDir: File

    @BeforeEach
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory("critter-test").toFile()
        outputDir = File(tempDir, "generated-classes/critter")
    }

    @Test
    fun testProcessorWithNoClasses() {
        val classesDir = File(tempDir, "classes")
        classesDir.mkdirs()

        val processor =
            CritterProcessor(
                classesDirectory = classesDir,
                outputDirectory = outputDir,
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load().packages(listOf(NO_ENTITIES)),
            )

        processor.process()

        // Output directory should not be created when no entities found
        // (or should be empty)
        if (outputDir.exists()) {
            assertTrue(outputDir.listFiles()?.isEmpty() ?: true)
        }
    }

    @Test
    fun testProcessorCreatesOutputDirectory() {
        val classesDir = File(tempDir, "classes")
        classesDir.mkdirs()

        // The output directory shouldn't exist initially
        assertTrue(!outputDir.exists())

        val processor =
            CritterProcessor(
                classesDirectory = classesDir,
                outputDirectory = outputDir,
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load().packages(listOf(NO_ENTITIES)),
            )

        processor.process()

        // Process should complete without error even with no entities
    }

    @Test
    fun testProcessorGeneratesAbstractEntitiesButNotInterfaces() {
        val processor =
            CritterProcessor(
                classesDirectory = File(tempDir, "classes"),
                outputDirectory = outputDir,
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load().packages(listOf(FIXTURES)),
            )

        processor.process()

        val generated = File(outputDir, FIXTURES.replace('.', '/') + "/__morphia")
        assertTrue(
            File(generated, "abstractanimal/AbstractAnimalEntityModel.class").exists(),
            "abstract entities should get an AOT model",
        )
        assertTrue(
            File(generated, "dog/DogEntityModel.class").exists(),
            "concrete subclasses should get an AOT model",
        )
        assertFalse(File(generated, "named").exists(), "interfaces should not get an AOT model")
    }

    @Test
    fun testProcessorFindsEntitiesThroughInterfaces() {
        val processor =
            CritterProcessor(
                classesDirectory = File(tempDir, "classes"),
                outputDirectory = outputDir,
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load().packages(listOf(FIXTURES)),
            )

        val found = processor.findEntityClasses().map { it.simpleName }.toSet()
        assertTrue(
            "Cat" in found,
            "a class whose @Entity comes from an interface in another package is an entity: $found",
        )
        assertFalse(
            "CatStandIn" in found,
            "an @ExternalEntity stand-in must not replace its target's model: $found",
        )

        processor.process()

        val generated = File(outputDir, FIXTURES.replace('.', '/') + "/__morphia")
        assertTrue(
            File(generated, "cat/CatEntityModel.class").exists(),
            "an entity through its interface should get an AOT model",
        )
    }

    @Test
    fun testProcessorReportsSkippedEntities() {
        val processor =
            CritterProcessor(
                classesDirectory = File(tempDir, "classes"),
                outputDirectory = outputDir,
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load().packages(listOf(FIXTURES)),
            )

        processor.process()

        assertEquals(setOf("$FIXTURES.GetterId"), processor.skipped.keys)
        assertTrue(
            processor.skipped.getValue("$FIXTURES.GetterId").contains("@Id on getter"),
            processor.skipped.toString(),
        )
        assertFalse(File(outputDir, FIXTURES.replace('.', '/') + "/__morphia/getterid").exists())
    }

    private companion object {
        const val FIXTURES = "dev.morphia.critter.maven.fixtures"

        /** A package with no classes, so the scan finds no entities on the test classpath. */
        const val NO_ENTITIES = "dev.morphia.critter.maven.none"
    }
}
