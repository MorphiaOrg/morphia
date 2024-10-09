package util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ClassName.Companion.bestGuess
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.objectBuilder
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
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
import org.jboss.forge.roaster.model.source.Import
import org.jboss.forge.roaster.model.source.JavaAnnotationSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.util.Types
import org.objectweb.asm.Type as AsmType
import org.objectweb.asm.tree.AnnotationNode
import util.AnnotationBuilders.methodCase

@OptIn(DelicateKotlinPoetApi::class)
@Mojo(name = "morphia-annotation-node", defaultPhase = GENERATE_SOURCES)
class AnnotationNodeExtensions : AbstractMojo() {
    companion object {
        val RESULT_HANDLE = ClassName("io.quarkus.gizmo", "ResultHandle")
        val METHOD_CREATOR = ClassName("io.quarkus.gizmo", "MethodCreator")
        val METHOD_DESCRIPTOR = ClassName("io.quarkus.gizmo", "MethodDescriptor")
    }

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

            fileBuilder.addImport("kotlin.reflect.full", "declaredMemberProperties")
            fileBuilder.addImport("kotlin.reflect.jvm", "javaType")
            fileBuilder.addImport("dev.morphia.mapping", "MappingException")
            fileBuilder.addImport("java.util", "Objects")
            fileBuilder.addImport("dev.morphia.critter.parser", "methodCase")
            fileBuilder.addImport(
                "dev.morphia.critter.parser.gizmo",
                "load",
                "attributeType",
                "rawType"
            )
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
        genericSetters()
        genericConverter()
        for (source in builders.values) {
            annotationConverters(source)
            annotationValueSetters(source)
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

    private fun genericSetters() {
        val method =
            FunSpec.builder("setBuilderValues")
                .receiver(AnnotationNode::class.asClassName())
                .addParameter("creator", METHOD_CREATOR)
                .addParameter("local", RESULT_HANDLE)
        //                .returns(RESULT_HANDLE)

        method.beginControlFlow("return when (desc)")
        builders.values.forEach() { source ->
            val type = bestGuess(source.qualifiedName).toType()
            method.addStatement(
                """"${type.descriptor}" -> set${source.name}Values(creator, local)"""
            )
        }
        method.addStatement(
            """else -> throw IllegalArgumentException("Unknown annotation type: ${"$"}{desc}")"""
        )

        method.endControlFlow()

        factory.addFunction(method.build())
    }

    private fun annotationValueSetters(source: JavaAnnotationSource) {
        val builderName = "${source.name}Builder"
        fileBuilder.addImport(fileBuilder.packageName, "$builderName.${methodCase(builderName)}")
        val method =
            FunSpec.builder("set${source.name}Values")
                .addModifiers(KModifier.PRIVATE)
                .receiver(AnnotationNode::class.java)
                .addParameter("creator", METHOD_CREATOR)
                .addParameter("local", RESULT_HANDLE)

        if (source.annotationElements.isNotEmpty()) {
            method.addCode(
                """
                val map = (values?.windowed(2, 2) ?: emptyList())
                    .map { it -> (it[0] ?: "value") to it[1] }
                    .toMap()
                    
            """
                    .trimIndent()
            )
        }

        for (element in source.annotationElements) {
            val builderName =
                ClassName(fileBuilder.packageName, "${source.name}Builder") /*.reflectionName()*/
            val name = element.name
            method.addCode(
                """
                    
                map["${name}"]?.let { 
                  val type = attributeType(${source.name}::class, "$name")
                  val method = %T.ofMethod( %T::class.java, "${name}", %T::class.java, rawType(type) )
                  creator.invokeVirtualMethod(method, local, load(creator, type, it))
                }
                
                """
                    .trimIndent(),
                METHOD_DESCRIPTOR,
                builderName,
                builderName
            )
        }

        factory.addFunction(method.build())
    }

    private fun annotationConverters(source: JavaAnnotationSource) {
        val builderName = "${source.name}Builder"
        fileBuilder.addImport(fileBuilder.packageName, "$builderName.${methodCase(builderName)}")
        val method =
            FunSpec.builder("to" + source.name)
                .receiver(AnnotationNode::class.asClassName())
                .addModifiers(KModifier.PRIVATE)
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
                return ${methodCase(source.name)}Builder().apply {
                
            """
                .trimIndent()

        for (element in source.annotationElements) {
            val name = element.name
            code += ("map[\"${name}\"]?.let { ${name}(${processType(element.type)}) }\n")
        }

        code += "}\n.build()"
        method.addCode(code)

        factory.addFunction(method.build())
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
            code = processArrayType(type)
        } else if (type.qualifiedName.startsWith("com.mongodb.client.model.")) {
            addImport(type.qualifiedName)
            code = "${type.simpleName}.valueOf((it as Array<String>)[1])"
        } else if (type.qualifiedName.startsWith("dev.morphia.mapping.")) {
            code = "${type.qualifiedName}.valueOf(it as String)"
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

    private fun addImport(typeName: String) {
        val className = bestGuess(typeName)
        fileBuilder.addImport(className.packageName, className.simpleName)
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
        return if (files != null) listOf(*files) else listOf()
    }

    private fun processArrayType(type: Type<JavaAnnotationSource>): String {
        var code: String = type.simpleName
        val parameterized = type.isParameterized
        val params = if (parameterized) type.typeArguments else emptyList()
        when {
            type.qualifiedName == "java.lang.Class" -> {
                val param = params[0]
                var parameterName = Types.toSimpleName(param.qualifiedName)
                if (param.isWildcard) {
                    parameterName = parameterName.substringAfterLast(' ')
                }
                if (!Types.isQualified(parameterName)) {
                    parameterName =
                        type.origin.imports
                            .filter { i: Import -> i.simpleName == parameterName }
                            .map { i -> i.qualifiedName }
                            .first()
                }
                if (param.isWildcard) {
                    parameterName += "<*>"
                }

                code = "${type.simpleName}<${parameterName}>"
                return """*(it as List<org.objectweb.asm.Type>)
                    .map { Class.forName(it.className) }
                    .toTypedArray() as Array<$code>"""
                    .trimMargin()
            }
            type.qualifiedName.startsWith("dev.morphia.annotations.") ->
                return "*(it as List<AnnotationNode>).map { it.to$code() } .toTypedArray()"
            type.qualifiedName == "java.lang.String" -> return "*(it as List<$code>).toTypedArray()"
            else -> TODO("unknown type: $type")
        }
    }
}

private fun ClassName.toType() = AsmType.getType("L${canonicalName};".replace('.', '/'))
