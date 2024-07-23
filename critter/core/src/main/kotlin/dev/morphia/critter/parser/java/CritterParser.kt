package dev.morphia.critter.parser.java

import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.conventions.CritterDefaultsConvention
import dev.morphia.critter.parser.noArgCtor
import dev.morphia.mapping.Mapper
import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassFile
import io.github.dmlloyd.classfile.attribute.InnerClassInfo
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag.SYNTHETIC
import io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion.RELEASE_17
import io.quarkus.gizmo.ClassCreator
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.constant.ClassDesc
import java.util.Optional
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor

object CritterParser {
    var outputGenerated: File? = null
    val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)

    fun asmify(bytes: ByteArray): String {
        val classReader = ClassReader(bytes)
        val traceWriter = StringWriter()
        val printWriter = PrintWriter(traceWriter)
        val traceClassVisitor = TraceClassVisitor(null, ASMifier(), printWriter)
        classReader.accept(traceClassVisitor, 0)
        return traceWriter.toString()
    }

    fun gizmo(input: File): Class<*> {
        val model = ClassFile.of().parse(input.readBytes())
        val modelName = model.thisClass().name().stringValue()
        val name = "${modelName}_InternalCritter"
        val innerClassDesc = ClassDesc.of(name.replace('/', '.'))
        val outerClassDesc = ClassDesc.of(modelName.replace('/', '.'))
        val conventions = listOf(CritterDefaultsConvention() /*,
                if (mapper.getConfig().propertyDiscovery() == FIELDS) FieldDiscovery() else MethodDiscovery(),
                ConfigureProperties()*/)
        val creator =
            ClassCreator.builder()
                .classOutput(critterClassLoader)
                .className(name)
                .superClass(Annotation::class.javaObjectType)

        val b =
            ClassFile.of().build(innerClassDesc) { builder: ClassBuilder ->
                builder.withVersion(RELEASE_17.major(), 0)
                builder.with(
                    InnerClassesAttribute.of(
                        InnerClassInfo.of(
                            innerClassDesc,
                            Optional.of(outerClassDesc),
                            Optional.of(innerClassDesc.displayName()),
                            SYNTHETIC
                        )
                    )
                )
                builder.noArgCtor()
                conventions.forEach { convention ->
                    convention.apply(Mapper(MorphiaConfig.load()), builder, model)
                }
                model.fields().forEach {}
            }

        println("**************** b = ${b.size}")
        outputGenerated?.let {
            val file =
                File(outputGenerated, "${name.substringAfterLast("/").replace("$", "\\\$")}.class")
            println("**************** file = ${file.absoluteFile}")
            file.parentFile.mkdirs()
            FileOutputStream(file).use { it.write(b) }
        }
        critterClassLoader.register(name.replace('/', '.'), b)
        return critterClassLoader.loadClass(name.replace('/', '.'))
    }

    fun parser(input: File): Class<*> {
        val model = ClassFile.of().parse(input.readBytes())
        val modelName = model.thisClass().name().stringValue()
        val name = "${modelName}_InternalCritter"
        val innerClassDesc = ClassDesc.of(name.replace('/', '.'))
        val outerClassDesc = ClassDesc.of(modelName.replace('/', '.'))
        val conventions = listOf(CritterDefaultsConvention() /*,
                if (mapper.getConfig().propertyDiscovery() == FIELDS) FieldDiscovery() else MethodDiscovery(),
                ConfigureProperties()*/)
        val b =
            ClassFile.of().build(innerClassDesc) { builder: ClassBuilder ->
                builder.withVersion(RELEASE_17.major(), 0)
                builder.with(
                    InnerClassesAttribute.of(
                        InnerClassInfo.of(
                            innerClassDesc,
                            Optional.of(outerClassDesc),
                            Optional.of(innerClassDesc.displayName()),
                            SYNTHETIC
                        )
                    )
                )
                builder.noArgCtor()
                conventions.forEach { convention ->
                    convention.apply(Mapper(MorphiaConfig.load()), builder, model)
                }
                model.fields().forEach {}
            }

        println("**************** b = ${b.size}")
        outputGenerated?.let {
            val file =
                File(outputGenerated, "${name.substringAfterLast("/").replace("$", "\\\$")}.class")
            println("**************** file = ${file.absoluteFile}")
            file.parentFile.mkdirs()
            FileOutputStream(file).use { it.write(b) }
        }
        critterClassLoader.register(name.replace('/', '.'), b)
        return critterClassLoader.loadClass(name.replace('/', '.'))
    }
}
