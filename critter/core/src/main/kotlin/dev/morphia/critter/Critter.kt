package dev.morphia.critter

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KSPJvmConfig.Builder
import com.google.devtools.ksp.processing.KspGradleLogger
import dev.morphia.annotations.Entity
import dev.morphia.critter.parser.CritterProcessorProvider
import java.io.File

class Critter(val root: File) {

    private val outputDir = File(root, "target")
    private val ksp = File(outputDir, "ksp")

    fun build(): KSPConfig {
        val builder =
            Builder().apply {
                apiVersion = "2.0"
                cachesDir = File(ksp, "cache")
                classOutputDir = File(ksp, "class-output")
                javaOutputDir = File(ksp, "generated")
                jvmTarget = "17"
                kotlinOutputDir = File(ksp, "kotlin-output")
                languageVersion = "2.0"
                moduleName = ""
                outputBaseDir = outputDir
                projectBaseDir = root
                resourceOutputDir = File(ksp, "resource-output")
                sourceRoots = listOf(File(root, "src/test/kotlin"))
                libraries = listOf(File(Entity::class.java.loadPath()))
            }

        return builder.build()
    }

    fun process() {
        KotlinSymbolProcessing(
                build(),
                listOf(CritterProcessorProvider()),
                KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)
            )
            .execute()
    }

    private fun Class<Entity>.loadPath() =
        getProtectionDomain().getCodeSource().getLocation().toURI()
}
