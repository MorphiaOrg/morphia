package util

import java.io.File
import java.io.FileFilter
import java.io.StringWriter
import javax.tools.ToolProvider
import kotlin.jvm.java
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaClass
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import util.refaster.InMemoryFileManager
import util.refaster.InMemoryFileObject
import util.refaster.SourceFileObject

@Mojo(name = "morphia-refaster-builder", defaultPhase = PROCESS_CLASSES)
class RefasterBuilder : AbstractMojo() {
    companion object {
        val m2 = File(System.getProperty("user.home"), ".m2/repository")
        val jc =
            ToolProvider.getSystemJavaCompiler() ?: throw RuntimeException("Compiler unavailable")

        fun find(root: File, filter: FileFilter): List<File> {
            val files = mutableListOf<File>()
            root
                .walkTopDown()
                .filter { it != root }
                .forEach {
                    if (it.isFile && filter.accept(it)) {
                        files.add(it)
                    } else if (it.isDirectory) {
                        files.addAll(find(it, filter))
                    }
                }
            return files
        }

        fun mavenPath(): (Dependency) -> File = {
            File(
                m2,
                "${it.groupId.replace('.', '/')}/${it.artifactId}/${it.version}/${it.artifactId}-${it.version}.jar"
            )
        }
    }

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    lateinit var project: MavenProject

    val dependencies: String by lazy {
        project.dependencies
            .filterNot { it.artifactId == "morphia-core" }
            .map(mavenPath())
            .joinToString(File.pathSeparator)
    }
    val morphia: String by lazy {
        project.dependencies
            .filter { it.artifactId == "morphia-core" }
            .map(mavenPath())
            .first()
            .absolutePath
    }
    val morphia2x: String by lazy {
        project.dependencies
            .filter { it.artifactId == "morphia-core" }
            .map {
                it.clone().also {
                    it.version = project.properties.getProperty("morphia.prior.version")
                }
            }
            .map(mavenPath())
            .first()
            .absolutePath
    }

    private val filter = FileFilter {
        (it.extension in listOf("java", "kt")) && it.name != "package-info.java"
    }

    override fun execute() {
        val src = project.basedir.resolve("src/main")
        val files =
            find(src, filter)
                .toSet()
                .map { it to Roaster.parse(JavaType::class.java, it) }
                .filter { it.second is JavaClass<*> }
                .map { it.first to it.second as JavaClass<*> }
                .filter {
                    it.second.hasAnnotation("org.openrewrite.java.template.RecipeDescriptor")
                }
        val output =
            files.flatMap {
                listOf("BeforeTemplate", "AfterTemplate").map { annotation ->
                    compile(it.second, annotation)
                }
            }
        println("**************** output = ${output}")
    }

    private fun compile(
        javaClass: JavaClass<*>,
        annotation: String
    ): MutableMap<String, InMemoryFileObject> {
        // Extract all string fields annotated with BeforeTemplate
        val fileManager = InMemoryFileManager(jc.getStandardFileManager(null, null, null))
        val templates = loadTemplate(javaClass, annotation)
        val synthesized = Roaster.create(JavaClassSource::class.java)
        synthesized.setPackage(javaClass.`package`)
        synthesized.name = "${javaClass.name}_$annotation"
        // Create a dummy class with the extracted methods
        val files =
            templates.map {
                javaClass
                    .toString()
                    .lines()
                    .filter { it.startsWith("import ") }
                    .filterNot { it.contains("BeforeTemplate") || it.contains("AfterTemplate") }
                    .forEach { synthesized.addImport(it.substringAfter(" ").substringBefore(";")) }
                val method =
                    synthesized.addMethod(
                        """
                // Method from field: ${it.second}\n"
                ${it.first}
                """
                            .trimIndent()
                    )
                when (annotation) {
                    "BeforeTemplate" ->
                        method.addAnnotation(
                            "com.google.errorprone.refaster.annotation.AfterTemplate"
                        )
                    "AfterTemplate" ->
                        method.addAnnotation(
                            "com.google.errorprone.refaster.annotation.AfterTemplate"
                        )
                    else -> throw RuntimeException("Unknown annotation: $annotation")
                }
                fileManager.classBytes[synthesized.qualifiedName] =
                    InMemoryFileObject(synthesized.name, synthesized.toString())

                SourceFileObject(synthesized.name, synthesized.toString())
            }

        val options =
            listOf(
                "-g",
                "-target",
                "17",
                "-source",
                "17",
                "-cp",
                when (annotation) {
                    "BeforeTemplate" -> morphia2x
                    "AfterTemplate" -> morphia
                    else -> throw RuntimeException("Unknown annotation: $annotation")
                } + File.pathSeparator + dependencies
            )
        val output = StringWriter()

        if (jc.getTask(output, fileManager, null, options, null, files).call()) {
            return fileManager.classBytes
        } else {
            throw RuntimeException("Compilation failed: $output")
        }
    }

    private fun loadTemplate(
        javaClass: JavaClass<*>,
        annotation: String
    ): List<Pair<String, String>> {
        val templates =
            javaClass.fields
                .filter { field ->
                    field.hasAnnotation(annotation) && field.type.isType(String::class.java)
                }
                .map { field ->
                    // Extract the field value (remove quotes if present)
                    val rawValue = field.stringInitializer ?: ""
                    val methodSource = rawValue.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
                    // Use field name or generate a name for the method
                    methodSource to field.name
                }
        return templates
    }
}
