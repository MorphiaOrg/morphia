package dev.morphia.critter.parser

import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassFile.ACC_PUBLIC
import io.github.dmlloyd.classfile.constantpool.ConstantPoolBuilder
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs.CD_String
import java.lang.constant.ConstantDescs.CD_boolean
import java.lang.constant.MethodTypeDesc

val INIT = "<init>"
val OBJECT = ClassDesc.of("java.lang", "Object")
val NO_ARG_VOID = MethodTypeDesc.ofDescriptor("()V")
val constantPoolBuilder = ConstantPoolBuilder.of()

fun ClassBuilder.noArgCtor() {
    withMethod(INIT, NO_ARG_VOID, ACC_PUBLIC) { method ->
        method.withCode { code ->
            code.aload(0)
            code.invokespecial(OBJECT, INIT, NO_ARG_VOID)
            code.return_()
        }
    }
}

fun ClassBuilder.booleanMethod(name: String, value: Boolean) {
    withMethod(name, MethodTypeDesc.of(CD_boolean), ACC_PUBLIC) { method ->
        method.withCode { code ->
            if (value) code.iconst_1() else code.iconst_0()
            code.ireturn()
        }
    }
}

fun ClassBuilder.stringMethod(name: String, value: String) {
    withMethod(name, MethodTypeDesc.of(CD_String), ACC_PUBLIC) { method ->
        method.withCode { code ->
            code.ldc(constantPoolBuilder.stringEntry(value))
            code.areturn()
        }
    }
}
