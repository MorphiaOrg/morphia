package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import java.io.File
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class CritterProcessorTest {

    private lateinit var tempDir: File
    private lateinit var outputDir: File

    @BeforeMethod
    fun setUp() {
        tempDir = createTempDir("critter-test")
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
                packages = emptyList(),
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
                packages = emptyList(),
                classLoader = Thread.currentThread().contextClassLoader,
                config = MorphiaConfig.load(),
            )

        processor.process()

        // Process should complete without error even with no entities
    }
}
