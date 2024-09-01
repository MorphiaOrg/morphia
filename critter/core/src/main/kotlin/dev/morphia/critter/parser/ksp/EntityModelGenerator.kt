package dev.morphia.critter.parser.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.mongodb.lang.NonNull
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.Builder
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import dev.morphia.annotations.internal.AnnotationKspFactory.entityAnnotation
import dev.morphia.annotations.internal.AnnotationKspFactory.entityCodeGen
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.parser.ksp.extensions.allAnnotations
import dev.morphia.critter.parser.ksp.extensions.name
import dev.morphia.critter.parser.ksp.extensions.toTypeName
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import javax.lang.model.element.Modifier.PUBLIC

class EntityModelGenerator(source: KSClassDeclaration, val config: MorphiaConfig) :
    Generator(source) {

    fun build(): Generator {
        val entityAnnotation = source.entityAnnotation()
        type =
            TypeSpec.classBuilder("${source.name()}EntityModel")
                .superclass(CritterEntityModel::class.java)

        val ctor =
            MethodSpec.constructorBuilder()
                .addParameter(Mapper::class.java, "mapper")
                .addCode("super(mapper, \$T.class);", source.toTypeName())
        annotations(ctor)
        type.addMethod(ctor.build())
        val fqcn = source.qualifiedName!!.asString()

        stringMethod(
            "collectionName",
            orDefault(entityAnnotation.value, config.collectionNaming().apply(source.name()))
        )
        /*
                stringMethod(
                    "discriminator",
                    config.discriminator().apply(fqcn, entityAnnotation.discriminator)
                )
        */
        stringMethod(
            "discriminatorKey",
            orDefault(entityAnnotation.discriminatorKey, config.discriminatorKey())
        )
        booleanMethod("useDiscriminator", entityAnnotation.useDiscriminator)

        return this
    }

    private fun annotations(ctor: Builder) {
        source.allAnnotations().forEach { annotation ->
            ctor.addCode("\nannotation(${annotation.entityCodeGen()});")
        }
    }

    private fun booleanMethod(methodName: String, value: Boolean) {
        val builder =
            MethodSpec.methodBuilder(methodName)
                .addAnnotation(NonNull::class.java)
                .addAnnotation(Override::class.java)
                .addModifiers(PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addCode("return $value;")
        type.addMethod(builder.build())
    }

    private fun stringMethod(methodName: String, value: String) {
        val builder =
            MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override::class.java)
                .addAnnotation(NonNull::class.java)
                .addModifiers(PUBLIC)
                .returns(String::class.java)
                .addCode("return \"$value\";")
        type.addMethod(builder.build())
    }

    private fun orDefault(value: String, defaultValue: String): String {
        return if (value != Mapper.IGNORED_FIELDNAME) value else defaultValue
    }
}
