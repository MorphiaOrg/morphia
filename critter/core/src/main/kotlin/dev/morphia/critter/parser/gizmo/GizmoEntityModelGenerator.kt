package dev.morphia.critter.parser.gizmo

import dev.morphia.annotations.Entity
import dev.morphia.annotations.internal.AnnotationNodeExtensions.toMorphiaAnnotation
import dev.morphia.critter.parser.Generators.config
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import java.lang.reflect.Modifier
import kotlin.use
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class GizmoEntityModelGenerator(
    type: Class<*>,
    critterClassLoader: dev.morphia.critter.CritterClassLoader,
    val classNode: ClassNode,
    val properties: List<PropertyModelGenerator>,
) : BaseGizmoGenerator(type, critterClassLoader) {
    var annotations: List<AnnotationNode>
    var morphiaAnnotations: List<Annotation>
    val entityAnnotation: Entity =
        entity.getAnnotation(Entity::class.java)
            ?: throw IllegalStateException("Class ${entity.name} does not have @Entity annotation")

    init {
        generatedType = "${baseName}.${entity.simpleName}EntityModel"
        annotations = classNode.visibleAnnotations ?: emptyList()
        morphiaAnnotations =
            annotations
                .filter { it.desc.startsWith("Ldev/morphia/annotations/") }
                .map { it.toMorphiaAnnotation() }
    }

    fun <T : Annotation> annotation(type: Class<T>): T? = entity.getAnnotation(type)

    fun emit(): GizmoEntityModelGenerator {
        builder.superClass(CritterEntityModel::class.java)

        creator.use {
            ctor()
            collectionName()
            discriminator()
            discriminatorKey()
            isAbstract()
            isInterface()
            useDiscriminator()
        }

        return this
    }

    private fun useDiscriminator() {
        creator.getMethodCreator("useDiscriminator", "boolean").use {
            it.returnValue(it.load(entityAnnotation.useDiscriminator))
        }
    }

    private fun isInterface() {
        creator.getMethodCreator("isInterface", "boolean").use {
            it.returnValue(it.load(entity.isInterface))
        }
    }

    private fun isAbstract() {
        creator.getMethodCreator("isAbstract", "boolean").use {
            it.returnValue(it.load(Modifier.isAbstract(entity.modifiers)))
        }
    }

    private fun discriminatorKey() {
        creator.getMethodCreator("discriminatorKey", String::class.java).use {
            val key = entityAnnotation.discriminator
            it.returnValue(
                it.load(if (key == Mapper.IGNORED_FIELDNAME) config.discriminatorKey() else key)
            )
        }
    }

    private fun discriminator() {
        creator.getMethodCreator("discriminator", String::class.java).use {
            val discriminator = config.discriminator().apply(entity, entityAnnotation.discriminator)
            it.returnValue(it.load(discriminator))
        }
    }

    private fun collectionName() {
        creator.getMethodCreator("collectionName", String::class.java).use {
            val key = entityAnnotation.value
            it.returnValue(
                it.load(
                    if (key == Mapper.IGNORED_FIELDNAME)
                        config.collectionNaming().apply(entity.simpleName)
                    else key
                )
            )
        }
    }

    private fun ctor() {
        creator.getConstructorCreator(Mapper::class.java).use { constructor ->
            constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(
                    CritterEntityModel::class.java,
                    Mapper::class.java,
                    Class::class.java,
                ),
                constructor.getThis(),
                constructor.getMethodParam(0),
                constructor.loadClass(entity),
            )
            constructor.setParameterNames(arrayOf("mapper"))

            constructor.invokeVirtualMethod(
                ofMethod(generatedType, "setType", "void", Class::class.java),
                constructor.`this`,
                constructor.loadClass(entity),
            )
            loadProperties(constructor)
            registerAnnotations(constructor)

            constructor.returnVoid()
        }
    }

    private fun loadProperties(creator: MethodCreator) {
        val addProperty =
            ofMethod(generatedType, "addProperty", "boolean", PropertyModel::class.java)
        properties.forEach { property ->
            val modelCtor =
                MethodDescriptor.ofConstructor(property.generatedType, EntityModel::class.java)

            val model = creator.newInstance(modelCtor, creator.`this`)
            creator.invokeVirtualMethod(addProperty, creator.`this`, model)
        }
    }

    private fun registerAnnotations(constructor: MethodCreator) {
        val annotationMethod = ofMethod(generatedType, "annotation", "void", Annotation::class.java)
        annotations.forEach { annotation ->
            constructor.invokeVirtualMethod(
                annotationMethod,
                constructor.`this`,
                annotation.annotationBuilder(constructor),
            )
        }
    }
}
