package util

import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.StringWriter
import javax.tools.ToolProvider
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
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.openrewrite.java.template.RecipeDescriptor
import util.refaster.InMemoryFileManager
import util.refaster.InMemoryFileObject
import util.refaster.SourceFileObject
import util.refaster.TemplateAnnotation
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
        val output = files.map { compile(BEFORE, it) to compile(AFTER, it) }
        val merged = output.flatMap { merge(classNodes(it.first), classNodes(it.second)) }
        merged.forEach {
            val out = ClassWriter(0)
            it.accept(out)
            val output = File(project.build.outputDirectory, it.name + ".class")
            output.parentFile.mkdirs()
            FileOutputStream(output).use { fos -> fos.write(out.toByteArray()) }
        }
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

    private fun classNodes(beforeClasses: Map<String, InMemoryFileObject>): MutableList<ClassNode> =
        beforeClasses.entries
            .map { (key, fileObject) ->
                val classNode = ClassNode()
                val cr = ClassReader(fileObject.bytes())
                cr.accept(classNode, 0)
                classNode
            }
            .toMutableList()

    private fun compile(
        annotation: TemplateAnnotation,
        refasterTemplate: JavaClass<*>,
    ): Map<String, InMemoryFileObject> {
        // Extract all string fields annotated with BeforeTemplate
        val jc =
            ToolProvider.getSystemJavaCompiler() ?: throw RuntimeException("Compiler unavailable")
        val fileManager = InMemoryFileManager(jc.getStandardFileManager(null, null, null))
        val synthesized = buildRefasterSource(refasterTemplate, annotation, fileManager)
        val files = listOf(SourceFileObject(synthesized.name, synthesized.toString()))

        val options =
            listOf(
                "-proc:none",
                "-g",
                "-target",
                "17",
                "-source",
                "17",
                "-cp",
                when (annotation) {
                    BEFORE -> morphia2x
                    AFTER -> morphia
                } + File.pathSeparator + dependencies
            )
        val output = StringWriter()

        if (jc.getTask(output, fileManager, null, options, null, files).call()) {
            return fileManager.classBytes
        } else {
            throw RuntimeException("Compilation failed: $output")
        }
    }

    private fun buildRefasterSource(
        template: JavaClass<*>,
        annotation: TemplateAnnotation,
        fileManager: InMemoryFileManager,
    ): JavaClassSource {
        val synthesized = Roaster.create(JavaClassSource::class.java)
        synthesized.setPackage(template.`package`)
        synthesized.name = template.name.removeSuffix("Template")

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
        template.nestedTypes
            .map { nested -> buildRefasterSource(nested as JavaClass<*>, annotation, fileManager) }
            .forEach { synthesized.addNestedType(it) }
        template.fields
            .filter { field ->
                field.hasAnnotation(annotation.shortName()) && field.type.isType(String::class.java)
            }
            .map { field ->
                // Extract the field value (remove quotes if present)
                val rawValue = field.stringInitializer ?: ""
                val methodSource = rawValue.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
                val method =
                    synthesized.addMethod(
                        """
                    // Method from field: ${field.name}\n"
                    $methodSource
                    """
                            .trimIndent()
                    )
                method.addAnnotation(annotation.errorProne())
            }

        return synthesized
    }

    private fun synthesizeTemplateClass(
        annotation: TemplateAnnotation,
        javaClass: JavaClass<*>,
        fileManager: InMemoryFileManager,
    ): List<SourceFileObject> {
        val templates = loadTemplate(annotation, javaClass)
        val synthesized = Roaster.create(JavaClassSource::class.java)
        synthesized.setPackage(javaClass.`package`)
        synthesized.name = "${javaClass.name}_$annotation"
        // Create a dummy class with the extracted methods
        val classes =
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
                method.addAnnotation(annotation.errorProne())
                fileManager.classBytes[synthesized.qualifiedName] =
                    InMemoryFileObject(synthesized.name)

                synthesized
            }
        return emptyList()
    }

    private fun loadTemplate(
        annotation: TemplateAnnotation,
        javaClass: JavaClass<*>,
    ): List<Pair<String, String>> {
        val templates =
            javaClass.fields
                .filter { field ->
                    field.hasAnnotation(annotation.shortName()) &&
                        field.type.isType(String::class.java)
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
