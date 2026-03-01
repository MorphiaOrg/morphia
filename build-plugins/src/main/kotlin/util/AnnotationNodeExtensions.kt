package util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ClassName.Companion.bestGuess
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
import org.jboss.forge.roaster.model.Type
import org.jboss.forge.roaster.model.source.JavaAnnotationSource
import org.objectweb.asm.Type as AsmType

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
        val path = core().toString() + "/src/main/java/dev/morphia/annotations"
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

    private fun core(): File {
        var dir = project!!.basedir
        while (!File(dir, ".git").exists()) {
            dir = dir.parentFile
        }
        return File(dir, "core")
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
        body.appendLine(
            """
    private static java.util.Map<String, Object> toMap(org.objectweb.asm.tree.AnnotationNode annotationNode) {
        if (annotationNode.values == null) return java.util.Collections.emptyMap();
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        java.util.List<?> values = annotationNode.values;
        for (int i = 0; i < values.size() - 1; i += 2) {
            Object key = values.get(i);
            map.put(key != null ? key.toString() : "value", values.get(i + 1));
        }
        return map;
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
"""
        )

        // setBuilderValues dispatch
        body.appendLine(
            "    public void setBuilderValues(org.objectweb.asm.tree.AnnotationNode annotationNode, io.quarkus.gizmo.MethodCreator creator, io.quarkus.gizmo.ResultHandle local) {"
        )
        body.appendLine("        switch (annotationNode.desc) {")
        for (source in builders.values) {
            val type = bestGuess(source.qualifiedName).toType()
            body.appendLine(
                "            case \"${type.descriptor}\" -> set${source.name}Values(annotationNode, creator, local);"
            )
        }
        body.appendLine(
            "            default -> throw new IllegalArgumentException(\"Unknown annotation type: \" + annotationNode.desc);"
        )
        body.appendLine("        }")
        body.appendLine("    }")
        body.appendLine()

        // toMorphiaAnnotation dispatch
        body.appendLine(
            "    public <T extends java.lang.annotation.Annotation> T toMorphiaAnnotation(org.objectweb.asm.tree.AnnotationNode annotationNode) {"
        )
        body.appendLine("        return (T) switch (annotationNode.desc) {")
        for (source in builders.values) {
            val type = bestGuess(source.qualifiedName).toType()
            body.appendLine(
                "            case \"${type.descriptor}\" -> to${source.name}(annotationNode);"
            )
        }
        body.appendLine(
            "            default -> throw new IllegalArgumentException(\"Unknown annotation type: \" + annotationNode.desc);"
        )
        body.appendLine("        };")
        body.appendLine("    }")
        body.appendLine()

        // Per-annotation methods
        for (source in builders.values) {
            emitToAnnotationMethod(body, source)
            emitSetAnnotationValuesMethod(body, source)
        }

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

    private fun emitToAnnotationMethod(sb: StringBuilder, source: JavaAnnotationSource) {
        val builderClass =
            "${source.qualifiedName.substringBeforeLast('.')}.internal.${source.name}Builder"
        val methodName = source.name.first().lowercaseChar() + source.name.substring(1) + "Builder"

        sb.appendLine(
            "    private ${source.qualifiedName} to${source.name}(org.objectweb.asm.tree.AnnotationNode annotationNode) {"
        )
        if (source.annotationElements.isNotEmpty()) {
            sb.appendLine("        java.util.Map<String, Object> map = toMap(annotationNode);")
        }
        sb.appendLine("        var builder = $builderClass.$methodName();")

        for (element in source.annotationElements) {
            val name = element.name
            val varName = "__${name}"
            sb.appendLine("        Object $varName = map.get(\"$name\");")
            sb.appendLine("        if ($varName != null) {")
            sb.appendLine("            builder.$name(${processTypeJava(element.type, varName)});")
            sb.appendLine("        }")
        }

        sb.appendLine("        return builder.build();")
        sb.appendLine("    }")
        sb.appendLine()
    }

    private fun emitSetAnnotationValuesMethod(sb: StringBuilder, source: JavaAnnotationSource) {
        val builderClass =
            "${source.qualifiedName.substringBeforeLast('.')}.internal.${source.name}Builder"

        sb.appendLine(
            "    private void set${source.name}Values(org.objectweb.asm.tree.AnnotationNode annotationNode, io.quarkus.gizmo.MethodCreator creator, io.quarkus.gizmo.ResultHandle local) {"
        )
        if (source.annotationElements.isNotEmpty()) {
            sb.appendLine("        java.util.Map<String, Object> map = toMap(annotationNode);")
        }

        for (element in source.annotationElements) {
            val name = element.name
            val varName = "__${name}"
            sb.appendLine("        Object $varName = map.get(\"$name\");")
            sb.appendLine("        if ($varName != null) {")
            sb.appendLine(
                "            java.lang.reflect.Type type = dev.morphia.critter.parser.gizmo.GizmoExtensions.attributeType(${source.qualifiedName}.class, \"$name\");"
            )
            sb.appendLine(
                "            io.quarkus.gizmo.MethodDescriptor method = io.quarkus.gizmo.MethodDescriptor.ofMethod($builderClass.class, \"$name\", $builderClass.class, dev.morphia.critter.parser.gizmo.GizmoExtensions.rawType(type));"
            )
            sb.appendLine(
                "            creator.invokeVirtualMethod(method, local, dev.morphia.critter.parser.gizmo.GizmoExtensions.load(creator, type, $varName));"
            )
            sb.appendLine("        }")
        }

        sb.appendLine("    }")
        sb.appendLine()
    }

    private fun processTypeJava(type: Type<JavaAnnotationSource>, varName: String): String {
        val typeName = type.name

        return when {
            typeName == "boolean" -> "(Boolean) $varName"
            typeName == "String" -> "(String) $varName"
            typeName == "int" -> "(Integer) $varName"
            typeName == "long" -> "((Number) $varName).longValue()"
            typeName == "Class" && !type.isArray ->
                "loadClass(((org.objectweb.asm.Type) $varName).getClassName())"
            type.isArray -> processArrayTypeJava(type, varName)
            type.qualifiedName.startsWith("com.mongodb.client.model.") ->
                "${type.qualifiedName}.valueOf(((String[]) $varName)[1])"
            type.qualifiedName.startsWith("dev.morphia.mapping.") ->
                "${type.qualifiedName}.valueOf(((String[]) $varName)[1])"
            type.qualifiedName.startsWith("dev.morphia.annotations.") ->
                "to${type.simpleName}((org.objectweb.asm.tree.AnnotationNode) $varName)"
            else -> {
                System.out.printf(
                    "unknown type: %n\t%s %n\t%s %n\t%s %n",
                    typeName,
                    type.qualifiedName,
                    type.origin.isEnum,
                )
                "<UNKNOWN>"
            }
        }
    }

    private fun processArrayTypeJava(type: Type<JavaAnnotationSource>, varName: String): String {
        val simpleName: String = type.simpleName
        val parameterized = type.isParameterized
        val params = if (parameterized) type.typeArguments else emptyList()
        return when {
            type.qualifiedName == "java.lang.Class" -> {
                """((java.util.List<?>) $varName).stream().map(t -> loadClass(((org.objectweb.asm.Type) t).getClassName())).toArray(java.lang.Class[]::new)"""
            }
            type.qualifiedName.startsWith("dev.morphia.annotations.") ->
                "((java.util.List<?>) $varName).stream().map(a -> to${simpleName}((org.objectweb.asm.tree.AnnotationNode) a)).toArray(${type.qualifiedName}[]::new)"
            type.qualifiedName == "java.lang.String" ->
                "((java.util.List<String>) $varName).toArray(new String[0])"
            else -> TODO("unknown array type: $type")
        }
    }
}

private fun ClassName.toType() = AsmType.getType("L${canonicalName};".replace('.', '/'))
