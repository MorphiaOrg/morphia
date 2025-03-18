package util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.OutputStream
import java.io.StringWriter
import java.net.URI
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager.Location
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.JavaFileObject.Kind.*
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardJavaFileManager
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
        val output = files.map { compile(it.first, it.second) }
        println("**************** output = ${output}")
    }

    private fun mycompile(src: File, javaClass: JavaClass<*>): MutableMap<String, ByteArray> {
        TODO()
        /*
                val jc =
                    ToolProvider.getSystemJavaCompiler() ?: throw RuntimeException("Compiler unavailable")
                val fileObject = InMemoryFileObject(src)
                val classpath = System.getProperty("java.class.path")
                val options = listOf("-g", "-target", "17", "-source", "17", "-cp", classpath)
                val output = StringWriter()
                val fileManager = InMemoryFileManager(jc.getStandardFileManager(null, null, null))
                if (jc.getTask(output, fileManager, null, options, null, listOf(fileObject)).call()) {
                    return fileManager.classBytes
                } else {
                    throw RuntimeException("Compilation failed :" + output)
                }
        */
    }

    private fun compile(
        src: File,
        javaClass: JavaClass<*>
    ): MutableMap<String, InMemoryFileObject> {
        // Extract all string fields annotated with BeforeTemplate
        val templates = loadTemplate(javaClass)
        // Create a dummy class with the extracted methods
        val dummyClass = Roaster.create(JavaClassSource::class.java)
        dummyClass.setPackage(javaClass.`package`)
        dummyClass.name = "DummyClass${System.currentTimeMillis()}"
        javaClass
            .toString()
            .lines()
            .filter { it.startsWith("import ") }
            .forEach { dummyClass.addImport(it.substringAfter(" ").substringBefore(";")) }
        templates.forEach {
            dummyClass.addMethod(
                """
            // Method from field: ${it.second}\n"
            ${it.first}
            """
                    .trimIndent()
            )
        }

        // Compile the dummy class
        val fileObject =
            object : SimpleJavaFileObject(URI.create("string:///${dummyClass.name}.java"), SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                    return dummyClass.toString()
                }
            }

        val options =
            listOf(
                "-g",
                "-target",
                "17",
                "-source",
                "17",
                "-cp",
                morphia2x + File.pathSeparator + dependencies
            )
        val output = StringWriter()
        val fileManager = InMemoryFileManager(jc.getStandardFileManager(null, null, null))
        fileManager.classBytes[dummyClass.qualifiedName] =
            InMemoryFileObject(dummyClass.name, dummyClass.toString())

        if (jc.getTask(output, fileManager, null, options, null, listOf(fileObject)).call()) {
            return fileManager.classBytes
        } else {
            throw RuntimeException("Compilation failed: $output")
        }
    }

    private fun loadTemplate(javaClass: JavaClass<*>): List<Pair<String, String>> {
        val templates =
            javaClass.fields
                .filter { field ->
                    field.hasAnnotation("BeforeTemplate") && field.type.isType(String::class.java)
                }
                .map { field ->
                    // Extract the field value (remove quotes if present)
                    val rawValue = field.stringInitializer ?: ""
                    val methodSource = rawValue.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
                    // Use field name or generate a name for the method
                    val methodName = field.name
                    methodSource to methodName
                }
        return templates
    }
}

class InMemoryFileManager : ForwardingJavaFileManager<StandardJavaFileManager> {
    val classBytes = mutableMapOf<String, InMemoryFileObject>()

    constructor(fileManager: StandardJavaFileManager) : super(fileManager)

    override fun getJavaFileForOutput(
        location: Location,
        className: String,
        kind: Kind,
        sibling: FileObject
    ): JavaFileObject {
        val fileObject = classBytes[className]
        return fileObject as JavaFileObject
    }
}

class InMemoryFileObject(name: String, val source: String) :
    SimpleJavaFileObject(
        URI.create("string:///" + name.replace('.', '/') + CLASS.extension),
        CLASS
    ) {
    private val content = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return content
    }

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return source
    }

    fun getBytes(): ByteArray {
        return content.toByteArray()
    }
}
