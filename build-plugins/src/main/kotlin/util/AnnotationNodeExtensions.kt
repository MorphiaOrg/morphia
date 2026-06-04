package util

import java.io.File
import java.io.FileFilter
import java.io.FileWriter
import java.io.IOException
import java.util.TreeMap
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.jboss.forge.roaster.ParserException
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaAnnotationSource

@Mojo(name = "morphia-annotation-node", defaultPhase = GENERATE_SOURCES)
class AnnotationNodeExtensions : AbstractMojo() {
    companion object {
        fun find(path: String, filter: FileFilter): List<File> {
            val array = File(path).listFiles(filter)
            return if (array != null) listOf(*array) else listOf()
        }
    }

    private val builders: MutableMap<String, JavaAnnotationSource> = TreeMap()

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private val project: MavenProject? = null
    private var generated: File? = null
    private val filter = FileFilter { pathname: File ->
        (pathname.name.endsWith(".java") || pathname.name.endsWith(".kt")) &&
            !pathname.name.endsWith("Handler.java") &&
            !pathname.name.endsWith("Helper.java") &&
            pathname.name != "package-info.java"
    }

    @Throws(MojoExecutionException::class)
    override fun execute() {
        val files: MutableList<File> = ArrayList()
        generated =
            File(project!!.basedir.toString() + "/target/generated-sources/morphia-annotations/")
        val path = annotations().toString() + "/src/main/java/dev/morphia/annotations"
        files.addAll(find(path, filter))
        project.addCompileSourceRoot(generated!!.absolutePath)

        try {
            for (file in files) {
                try {
                    val source = Roaster.parse(JavaAnnotationSource::class.java, file)
                    if (source.isPublic) {
                        builders[source.name] = source
                    }
                } catch (e: ParserException) {
                    throw MojoExecutionException("Could not parse $file", e)
                }
            }
            emitJavaFactory()
        } catch (e: Exception) {
            throw MojoExecutionException(e.message, e)
        }
    }

    private fun annotations(): File {
        var dir = project!!.basedir
        while (!File(dir, ".git").exists()) {
            dir = dir.parentFile
        }
        return File(dir, "annotations")
    }

    @Throws(Exception::class)
    private fun emitJavaFactory() {
        val pkg = builders.values.iterator().next().getPackage() + ".internal"
        val body = StringBuilder()

        body.appendLine("package $pkg;")
        body.appendLine()
        body.appendLine("@dev.morphia.annotations.internal.MorphiaInternal")
        body.appendLine("@SuppressWarnings(\"unchecked\")")
        body.appendLine("public final class AnnotationNodeExtensions {")
        body.appendLine()
        body.appendLine(
            "    public static final AnnotationNodeExtensions INSTANCE = new AnnotationNodeExtensions();"
        )
        body.appendLine()
        body.appendLine("    private AnnotationNodeExtensions() {}")
        body.appendLine()

        body.appendLine("}")

        val outputFile = File(generated, pkg.replace('.', '/') + "/AnnotationNodeExtensions.java")
        if (!outputFile.parentFile.mkdirs() && !outputFile.parentFile.exists()) {
            throw IOException(
                String.format("Could not create directory: %s", outputFile.parentFile)
            )
        }
        // Delete any stale Kotlin file from previous builds
        File(generated, pkg.replace('.', '/') + "/AnnotationNodeExtensions.kt").delete()
        FileWriter(outputFile).use { out -> out.write(body.toString()) }
    }
}
