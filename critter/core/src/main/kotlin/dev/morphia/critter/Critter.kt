package dev.morphia.critter

import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KSPJvmConfig.Builder
import dev.morphia.annotations.Entity
import dev.morphia.annotations.Property
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.mapping.codec.pojo.PropertyModel
import java.io.File
import org.objectweb.asm.Type

class Critter(val root: File) {
    companion object {
        var outputDirectory = File("target/generated-sources/morphia")
        val critterClassLoader = CritterClassLoader(PropertyModel::class.java.classLoader)
        val propertyAnnotations = mutableListOf(Type.getType(Property::class.java))
        val transientAnnotations = mutableListOf(Type.getType(Transient::class.java))

        fun critterPackage(entity: Class<*>): String {
            return "${entity.packageName}.__morphia.${entity.simpleName.lowercase()}"
        }
    }

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
                sourceRoots = listOf(File(root, "src/test/java"), File(root, "src/test/kotlin"))
                libraries = listOf(File(Entity::class.java.loadPath()))
            }

        return builder.build()
    }

    private fun Class<Entity>.loadPath() =
        getProtectionDomain().getCodeSource().getLocation().toURI()
}

fun String.titleCase(): String {
    return first().uppercase() + substring(1)
}

fun String.identifierCase(): String {
    return first().lowercase() + substring(1)
}
