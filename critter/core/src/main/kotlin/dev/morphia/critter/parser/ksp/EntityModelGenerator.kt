package dev.morphia.critter.parser.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.parser.ksp.extensions.entityAnnotation
import dev.morphia.critter.parser.ksp.extensions.name
import dev.morphia.critter.parser.ksp.extensions.toTypeName
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.EntityModel

class EntityModelGenerator(source: KSClassDeclaration) : Generator(source) {

    fun build(): Generator {
        val entityAnnotation = source.entityAnnotation()
        type =
            TypeSpec.classBuilder("${source.name()}EntityModel").superclass(EntityModel::class.java)

        val ctor =
            MethodSpec.constructorBuilder()
                .addParameter(Mapper::class.java, "mapper")
                .addCode("super(mapper, \$T.class);", source.toTypeName())
        type.addMethod(ctor.build())

        val discriminator =
            MethodSpec.methodBuilder("discriminator")
                .returns(String::class.java)
                .addCode("return ${entityAnnotation.discriminator};")
        type.addMethod(discriminator.build())

        return this
    }
}
