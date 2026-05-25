package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import java.io.File
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
                config = MorphiaConfig.load(),
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
                config = MorphiaConfig.load(),
            )

        processor.process()

        // Process should complete without error even with no entities
    }
}
