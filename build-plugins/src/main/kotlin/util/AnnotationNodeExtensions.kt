package util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ClassName.Companion.bestGuess
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.objectBuilder
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import java.io.File
import java.io.FileFilter
import java.io.FileWriter
import java.io.IOException
import java.text.MessageFormat
import java.util.Arrays
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
import org.jboss.forge.roaster.model.source.Import
import org.jboss.forge.roaster.model.source.JavaAnnotationSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.util.Types
import org.objectweb.asm.Type as AsmType
import org.objectweb.asm.tree.AnnotationNode

@OptIn(DelicateKotlinPoetApi::class)
@Mojo(name = "morphia-annotation-node", defaultPhase = GENERATE_SOURCES)
class AnnotationNodeExtensions : AbstractMojo() {
    private val builders: MutableMap<String, JavaAnnotationSource> = TreeMap()
    lateinit var fileBuilder: FileSpec.Builder

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private val project: MavenProject? = null
    lateinit var factory: TypeSpec.Builder
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
        files.addAll(find(path))
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
            fileBuilder =
                FileSpec.builder(
                    builders.values.iterator().next().getPackage() + ".internal",
                    "AnnotationNodeExtensions"
                )
            fileBuilder.addAnnotation(
                AnnotationSpec.builder(Suppress::class.java)
                    .addMember("%S", "UNCHECKED_CAST")
                    .useSiteTarget(FILE)
                    .build()
            )

            fileBuilder.addImport("dev.morphia.mapping", "MappingException")
            fileBuilder.addImport("java.util", "Objects")
            emitFactory()
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
    private fun emitFactory() {
        factory = createFactory()
        genericConverter()
        for (source in builders.values) {
            annotationConverters(source)
            //            annotationExtractors(source)
            //            annotationCodeBuilders(source)
        }

        fileBuilder.addType(factory.build())
        val fileSpec: FileSpec = fileBuilder.build()
        val outputFile = File(generated, fileSpec.relativePath)
        if (!outputFile.parentFile.mkdirs() && !outputFile.parentFile.exists()) {
            throw IOException(
                String.format("Could not create directory: %s", outputFile.parentFile)
            )
        }
        FileWriter(outputFile).use { out -> fileSpec.writeTo(out) }
    }

    private fun genericConverter() {
        val typeVariable = TypeVariableName("T", Annotation::class.asClassName())
        val method =
            FunSpec.builder("toMorphiaAnnotation")
                .addTypeVariable(typeVariable)
                .receiver(AnnotationNode::class.asClassName())
                .returns(typeVariable)

        method.beginControlFlow("return when (desc)")
        for (source in builders.values) {
            val type = bestGuess(source.qualifiedName).toType()
            method.addStatement(""""${type.descriptor}" -> to${source.name}()""")
        }
        method.addStatement(
            """else -> throw %T("Unknown annotation type: ${"$"}{desc}")""",
            IllegalArgumentException::class
        )

        method.endControlFlow()
        method.addStatement("as T")

        factory.addFunction(method.build())
    }

    private fun annotationConverters(source: JavaAnnotationSource) {
        val builderName = "${source.name}Builder"
        fileBuilder.addImport(
            fileBuilder.packageName,
            "$builderName.${AnnotationBuilders.methodCase(builderName)}"
        )
        val method =
            FunSpec.builder("to" + source.name)
                .receiver(AnnotationNode::class.asClassName())
                .returns(bestGuess(source.qualifiedName))
        var code = ""
        if (source.annotationElements.isNotEmpty()) {
            code +=
                """
                val map = (values?.windowed(2, 2) ?: emptyList())
                    .map { it -> (it[0] ?: "value") to it[1] }
                    .toMap()
                    
            """
                    .trimIndent()
        }
        code +=
            """
                return ${AnnotationBuilders.methodCase(source.name)}Builder().apply {
                
            """
                .trimIndent()

        for (element in source.annotationElements) {
            val name = element.name
            var cast = processType(element.type)
            /*
                        if (element.type.isArray) {
                            cast = "*(${cast})"
                        }
            */
            code += ("map[\"${name}\"]?.let { ${name}(${cast}) }\n")
        }

        code += "}\n.build()"
        method.addCode(code)

        factory.addFunction(method.build())
    }

    private fun annotationCodeBuilders(source: JavaAnnotationSource) {
        val method =
            FunSpec.builder(AnnotationBuilders.methodCase(source.name) + "CodeGen")
                .receiver(KSAnnotation::class.java)
                .returns(bestGuess("kotlin.String"))
        val className: ClassName = bestGuess(builderName(source))
        fileBuilder.addImport(className.packageName, className.simpleName)
        method.addCode(
            MessageFormat.format(
                """
                 val map = arguments
                            .map '{' it -> (it.name?.asString() ?: "value") to it.value }
                            .toMap()
                var code = "{0}Builder.{1}Builder()"
                
                """
                    .trimIndent(),
                source.name,
                AnnotationBuilders.methodCase(source.name)
            )
        )

        for (element in source.annotationElements) {
            val name = element.name
            var value: String?

            method.addCode(
                """
                        map["$name"]?.let {
                    
                    """
                    .trimIndent()
            )
            val type = element.type
            if (type.qualifiedName.startsWith("dev.morphia.annotations.")) {
                val typeName = type.simpleName
                method.addCode(
                    """
                        if (!Objects.equals(${source.name}Builder.defaults.${name}, (it as AnnotationNode).to${typeName}())) {
                        
                        """
                        .trimIndent()
                )
                value = "\${it.${AnnotationBuilders.methodCase(typeName)}CodeGen()}"
            } else {
                method.addCode(
                    """
                        if (!Objects.equals(${source.name}Builder.defaults.${name}, it)) {
                        
                        """
                        .trimIndent()
                )

                value = getValue(type)
            }

            method.addCode(
                """
                        code += ".${name}(${value})"
                      }
                    }
                    
                    """
                    .trimIndent()
            )
        }

        method.addCode(
            """
                code += ".build()"
                
                """
                .trimIndent()
        )
        method.addCode("return code")

        factory.addFunction(method.build())
    }

    private fun getValue(type: Type<JavaAnnotationSource>): String {
        val typeName = type.name
        var code = "\$it"

        if (typeName == "String") {
            code = """\"${code}\""""
        } else if (typeName == "Class") {
            code += ".class"
        }
        return code
    }

    private fun processType(type: Type<JavaAnnotationSource>): String {
        val typeName = type.name
        var code: String? = "NOT SET"

        if (typeName == "boolean") {
            code = "it as Boolean"
        } else if (typeName == "String") {
            code = "it as String"
        } else if (typeName == "int") {
            code = "it as Int"
        } else if (typeName == "long") {
            code = "(it as Number).toLong()"
        } else if (typeName == "Class") {
            code = "it as Class<*>"
        } else if (type.isArray) {
            code = "${processArrayType(type)}"
        } else if (
            type.qualifiedName.startsWith("com.mongodb.client.model.") ||
                type.qualifiedName.startsWith("dev.morphia.mapping.")
        ) {
            code = "it as ${type.qualifiedName}"
        } else if (type.qualifiedName.startsWith("dev.morphia.annotations.")) {
            code = "(it as AnnotationNode).to${type.simpleName}()"
        } else {
            System.out.printf(
                "unknown type: %n\t%s %n\t%s %n\t%s %n",
                typeName,
                type.qualifiedName,
                type.origin.isEnum
            )
            code = "<UNKNOWN>"
        }

        return code
    }

    private fun createFactory(): TypeSpec.Builder {
        val extensionsFactory = objectBuilder("AnnotationNodeExtensions")
        val classBuilder =
            Roaster.create(JavaClassSource::class.java)
                .setName("AnnotationNodeExtensions")
                .setPackage(builders.values.iterator().next().getPackage() + ".internal")
                .setFinal(true)
        classBuilder.addAnnotation("dev.morphia.annotations.internal.MorphiaInternal")

        return extensionsFactory
    }

    private fun find(path: String): List<File> {
        val files = File(path).listFiles(filter)
        return if (files != null) Arrays.asList(*files) else listOf()
    }

    companion object {
        private fun builderName(builder: JavaSource<*>): String {
            val pkg = builder.getPackage() + ".internal."
            val name = builder.name + "Builder"
            return pkg + name
        }

        private fun processArrayType(type: Type<JavaAnnotationSource>): String {
            var code: String = type.simpleName
            val parameterized = type.isParameterized
            val params = if (parameterized) type.typeArguments else emptyList()
            if (!params.isEmpty()) {
                val param = params[0]
                var parameterName = Types.toSimpleName(param.qualifiedName)
                if (param.isWildcard) {
                    parameterName = parameterName.substring(parameterName.lastIndexOf(' ') + 1)
                }
                if (!Types.isQualified(parameterName)) {
                    val target = parameterName
                    val imp =
                        type.origin.imports
                            .stream()
                            .filter { i: Import -> i.simpleName == target }
                            .findFirst()
                            .orElseThrow()
                    parameterName = imp.qualifiedName
                }
                if (param.isWildcard) {
                    parameterName += "<*>"
                }

                code = "${type.simpleName}<${parameterName}>"
            }
            return "*(it as List<${code}>).toTypedArray()"
        }
    }
}

private fun ClassName.toType() = AsmType.getType("L${canonicalName};".replace('.', '/'))
