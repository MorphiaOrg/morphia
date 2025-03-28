@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package util

import com.sun.tools.javac.parser.ParserFactory
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.util.Context
import java.io.File
import java.io.FileFilter
import javax.tools.JavaFileManager
import javax.tools.ToolProvider
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.Field
import org.jboss.forge.roaster.model.JavaClass
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.objectweb.asm.tree.ClassNode
import org.openrewrite.java.template.RecipeDescriptor
import org.openrewrite.java.template.processor.RefasterTemplateProcessor
import util.refaster.InMemoryFileManager
import util.refaster.TemplateAnnotation.AFTER
import util.refaster.TemplateAnnotation.BEFORE

@Mojo(name = "morphia-refaster-builder", defaultPhase = PROCESS_CLASSES)
class RefasterBuilder : AbstractMojo() {
    companion object {
        val m2 = File(System.getProperty("user.home"), ".m2/repository")

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
                .map { it.second as JavaClass<*> }
                .filter { it.hasAnnotation("dev.morphia.rewrite.refaster.TemplateDescriptor") }
        val output = files.map { source(it) }
        compile(output)
        /*
                merged.forEach {
                    val out = ClassWriter(0)
                    it.accept(out)
                    val output = File(project.build.outputDirectory, it.name + ".class")
                    output.parentFile.mkdirs()
                    FileOutputStream(output).use { fos -> fos.write(out.toByteArray()) }
                }
        */

    }

    private fun source(refasterTemplate: JavaClass<*>): JavaClassSource {
        // Extract all string fields annotated with BeforeTemplate
        val jc =
            ToolProvider.getSystemJavaCompiler() ?: throw RuntimeException("Compiler unavailable")
        val fileManager = InMemoryFileManager(jc.getStandardFileManager(null, null, null))
        return buildSingleRefasterSource(refasterTemplate)
    }

    private fun merge(beforeNodes: List<ClassNode>, afterNodes: List<ClassNode>): List<ClassNode> {
        afterNodes
            .sortedBy { it.name }
            .zip(beforeNodes.sortedBy { it.name })
            .forEach { (after, before) ->
                after.methods
                    .filter { !it.name.contains("<init>") }
                    .forEach { before.methods.add(it) }
            }
        return beforeNodes
    }

    private fun buildSingleRefasterSource(template: JavaClass<*>): JavaClassSource {
        val synthesized = Roaster.create(JavaClassSource::class.java)
        synthesized.setPackage(template.`package`)
        synthesized.name = template.name.removeSuffix("Template")

        replaceAnnotations(template, synthesized)
        template.nestedTypes
            .map { nested -> buildSingleRefasterSource(nested as JavaClass<*>) }
            .forEach { synthesized.addNestedType(it) }
        template.fields
            .filter { field -> isAnnotated(field) && field.type.isType(String::class.java) }
            .map { field ->
                // Extract the field value (remove quotes if present)
                val rawValue = field.stringInitializer ?: ""
                val methodSource = rawValue.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
                val fieldAnn = field.annotations.first().name
                val method =
                    synthesized.addMethod(
                        """
                    // Method from field: ${field.name}\n"
                    $methodSource
                    """
                            .trimIndent()
                    )
                when (fieldAnn) {
                    "RefasterBeforeTemplate" -> method.addAnnotation(BEFORE.errorProne())
                    "RefasterAfterTemplate" -> method.addAnnotation(AFTER.errorProne())
                }
            }

        return synthesized
    }

    private fun isAnnotated(field: Field<out JavaClass<*>>) =
        field.hasAnnotation(AFTER.shortName()) || field.hasAnnotation(BEFORE.shortName())

    private val context = Context()

    private fun compile(templates: List<JavaClassSource>) {
        context.put(
            JavaFileManager::class.java,
            InMemoryFileManager(
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)
            )
        )
        val instance = ParserFactory.instance(context)
        val (processor, function) = refasterProcessor()
        templates.forEach { synthesized ->
            val compilationUnit =
                instance
                    .newParser(synthesized.toString(), false, false, false)
                    .parseCompilationUnit()
            val call = function.call(processor, compilationUnit)
            println("**************** call = ${call}")
        }
    }

    private fun refasterProcessor(): Pair<RefasterTemplateProcessor, KFunction<*>> {
        val processor = RefasterTemplateProcessor()
        processor.init(JavacProcessingEnvironment.instance(context))
        val function =
            processor::class
                .declaredMemberFunctions
                .filter { m -> m.name == "maybeGenerateTemplateSources" }
                .map { m ->
                    m.javaMethod!!.setAccessible(true)
                    m
                }
                .first()
        return Pair(processor, function)
    }

    private fun replaceAnnotations(template: JavaClass<*>, synthesized: JavaClassSource) {
        template
            .toString()
            .lines()
            .filter { it.startsWith("import ") }
            .filterNot {
                it.contains("RefasterBeforeTemplate") ||
                    it.contains("RefasterAfterTemplate") ||
                    it.contains("TemplateDescriptor")
            }
            .forEach { synthesized.addImport(it.substringAfter(" ").substringBefore(";")) }
        var templateAnnotation =
            template.getAnnotation("dev.morphia.rewrite.refaster.TemplateDescriptor")
        val descriptor = synthesized.addAnnotation(RecipeDescriptor::class.java)
        descriptor.setStringValue("name", templateAnnotation.getStringValue("name"))
        descriptor.setStringValue("description", templateAnnotation.getStringValue("description"))
    }
}
