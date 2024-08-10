package dev.morphia.critter.parser.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.Critter
import dev.morphia.critter.parser.ksp.extensions.methodName
import dev.morphia.critter.parser.ksp.extensions.packageName

open class Generator(val source: KSClassDeclaration) {
    lateinit var type: TypeSpec.Builder

    fun write() {
        JavaFile.builder(source.packageName() + ".__morphia.${source.methodName()}", type.build())
            .build()
            .writeTo(Critter.outputDirectory)
    }
}
