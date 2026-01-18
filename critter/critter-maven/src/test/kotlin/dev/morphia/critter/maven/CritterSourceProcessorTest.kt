package dev.morphia.critter.maven

import dev.morphia.config.MorphiaConfig
import java.io.File
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class CritterSourceProcessorTest {

    private lateinit var tempDir: File
    private lateinit var sourceOutputDir: File
    private lateinit var resourceOutputDir: File

    @BeforeMethod
    fun setUp() {
        tempDir = createTempDir("critter-source-test")
        sourceOutputDir = File(tempDir, "generated-sources/critter")
        resourceOutputDir = File(tempDir, "generated-resources/critter")
    }

    @Test
    fun testProcessorWithNoSources() {
        val sourceDir = File(tempDir, "src/main/java")
        sourceDir.mkdirs()

        val processor =
            CritterSourceProcessor(
                sourceDirectories = listOf(sourceDir),
                outputDirectory = sourceOutputDir,
                resourceOutputDirectory = resourceOutputDir,
                packages = emptyList(),
                criteriaPackage = null,
                format = true,
                config = MorphiaConfig.load(),
            )

        processor.process()

        // Output directories should not contain files when no entities found
        if (sourceOutputDir.exists()) {
            assertTrue(sourceOutputDir.listFiles()?.isEmpty() ?: true)
        }
    }

    @Test
    fun testProcessorWithSimpleEntity() {
        val sourceDir = File(tempDir, "src/main/java/com/example")
        sourceDir.mkdirs()

        // Create a simple entity source file
        val entityFile = File(sourceDir, "TestEntity.java")
        entityFile.writeText(
            """
            package com.example;

            import dev.morphia.annotations.Entity;
            import dev.morphia.annotations.Id;
            import org.bson.types.ObjectId;

            @Entity
            public class TestEntity {
                @Id
                private ObjectId id;
                private String name;

                public ObjectId getId() { return id; }
                public void setId(ObjectId id) { this.id = id; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
        """
                .trimIndent()
        )

        val processor =
            CritterSourceProcessor(
                sourceDirectories = listOf(File(tempDir, "src/main/java")),
                outputDirectory = sourceOutputDir,
                resourceOutputDirectory = resourceOutputDir,
                packages = listOf("com.example"),
                criteriaPackage = null,
                format = true,
                config = MorphiaConfig.load(),
            )

        processor.process()

        // Processor should find the entity
        // Note: Full processing would generate files, but the TODO implementation
        // currently only scans and logs
    }

    @Test
    fun testProcessorWithMorphiaConfig() {
        val sourceDir = File(tempDir, "src/main/java")
        sourceDir.mkdirs()

        // Create a morphia-config.properties file
        val resourcesDir = File(tempDir, "src/main/resources/META-INF")
        resourcesDir.mkdirs()
        val configFile = File(resourcesDir, "morphia-config.properties")
        configFile.writeText(
            """
            morphia.database=testdb
            morphia.collection-naming=snakeCase
            morphia.discriminator=className
        """
                .trimIndent()
        )

        // Load config manually for testing
        val config = MorphiaConfig.load()

        val processor =
            CritterSourceProcessor(
                sourceDirectories = listOf(sourceDir),
                outputDirectory = sourceOutputDir,
                resourceOutputDirectory = resourceOutputDir,
                packages = emptyList(),
                criteriaPackage = null,
                format = true,
                config = config,
            )

        processor.process()
        // Processor should use config settings
    }
}
