package dev.morphia.critter.parser.java

import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassFile
import io.github.dmlloyd.classfile.ClassFile.ACC_PUBLIC
import io.github.dmlloyd.classfile.CodeBuilder
import io.github.dmlloyd.classfile.attribute.InnerClassInfo
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute
import io.github.dmlloyd.classfile.constantpool.ConstantPoolBuilder
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag.SYNTHETIC
import io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion.RELEASE_17
import java.io.File
import java.io.FileOutputStream
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc
import java.util.Optional
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader

object CritterParser {
    val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)
    private const val INIT = "<init>"
    private val NO_ARG_VOID = MethodTypeDesc.ofDescriptor("()V")
    private val OBJECT = ClassDesc.of("java.lang", "Object")

    fun parser(input: File): Class<*> {
        val model = ClassFile.of().parse(input.readBytes())
        val modelName = model.thisClass().name().stringValue()
        val name = "$modelName\$InternalCritter"
        val innerClassDesc = ClassDesc.of(name.replace('/', '.'))
        val outerClassDesc = ClassDesc.of(modelName.replace('/', '.'))
        val b = ClassFile.of().build(
            innerClassDesc
        ) { builder: ClassBuilder ->
            val constantPoolBuilder = ConstantPoolBuilder.of()
            val stringEntry = constantPoolBuilder.stringEntry("hi")
            builder.withVersion(RELEASE_17.major(), 0)
            builder.with(InnerClassesAttribute.of(InnerClassInfo.of(innerClassDesc, Optional.of(outerClassDesc),
                Optional.of(innerClassDesc.displayName()), SYNTHETIC)))
            builder.noArgCtor()

            builder.withMethod("privateAccess", MethodTypeDesc.of(ClassDesc.of("java.lang.String")), ACC_PUBLIC) { method ->
                method.withCode { code: CodeBuilder ->
                    code.ldc(stringEntry)
                    code.areturn()
                }
            }
        }

        println("**************** modelName = ${modelName}")
        println("**************** model.methods() = ${model.methods()}")
        println("**************** name = ${name}")
        FileOutputStream("target/test.class").write(b)
        critterClassLoader.register(name.replace('/', '.'), b);
        return critterClassLoader.loadClass(name.replace('/', '.'))
    }

    private fun ClassBuilder.noArgCtor() {
        withMethod(INIT, NO_ARG_VOID, ACC_PUBLIC) { method ->
            method.withCode { code ->
                code.aload(0)
                code.invokespecial(OBJECT, INIT, NO_ARG_VOID)
                code.return_()
            }
        }
    }
}

class CritterClassLoader(parent: ClassLoader): ByteArrayClassLoader(parent, mapOf()) {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes;
    }
}