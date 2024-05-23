package dev.morphia.critter.parser.java

import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassFile
import io.github.dmlloyd.classfile.attribute.InnerClassInfo
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag.SYNTHETIC
import io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion.RELEASE_17
import java.io.File
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDesc
import java.lang.constant.MethodTypeDesc
import java.util.Optional
import java.util.function.Consumer
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader

object CritterParser {
    val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)

    fun parser(input: File): Class<*> {
        val model = ClassFile.of().parse(input.readBytes())
        val modelName = model.thisClass().name().stringValue()
        val innerClassDesc = ClassDesc.of("$modelName\$InternalCritter".replace('/', '.'))
        val outerClassDesc = ClassDesc.of(modelName.replace('/', '.'))
        val b = ClassFile.of().build(
            innerClassDesc
        ) { builder: ClassBuilder ->
            builder.withVersion(RELEASE_17.major(), 0)
            builder.with(InnerClassesAttribute.of(InnerClassInfo.of(innerClassDesc, Optional.of(outerClassDesc),
                Optional.of(innerClassDesc.displayName()), SYNTHETIC)))

            builder.withMethod("privateAccess", MethodTypeDesc.of(ClassDesc.of("java.lang.String")), ClassFile.ACC_PUBLIC) { method ->
                method.withCode { code ->
//                    code.ldc("hi")
                }
                method.
            }
        }

        println("**************** b = ${b}")

        critterClassLoader.register("$modelName\$InternalCritter".replace('/', '.'), b);
        return critterClassLoader.loadClass("$modelName\$InternalCritter".replace('/', '.'))
    }
}

class CritterClassLoader(parent: ClassLoader): ByteArrayClassLoader(parent, mapOf()) {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes;
    }
}