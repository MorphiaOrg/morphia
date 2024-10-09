package dev.morphia.critter.parser.gizmo

import dev.morphia.annotations.Entity
import dev.morphia.annotations.internal.AnnotationNodeExtensions.toMorphiaAnnotation
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import kotlin.use
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class GizmoEntityModelGenerator(
    type: Class<*>,
    val classNode: ClassNode,
    val properties: List<PropertyModelGenerator>
) : BaseGizmoGenerator(type) {
    var annotations: List<AnnotationNode>
    var morphiaAnnotations: List<Annotation>

    init {
        generatedType = "${baseName}.${entity.simpleName}EntityModel"
        annotations = classNode.visibleAnnotations ?: emptyList()
        morphiaAnnotations = annotations.map { it.toMorphiaAnnotation() }
    }

    fun <T> annotation(type: Class<T>) = morphiaAnnotations.filterIsInstance(type).firstOrNull()

    fun emit(): GizmoEntityModelGenerator {
        builder.superClass(CritterEntityModel::class.java)

        creator.use {
            ctor()
            discriminator()
        }

        return this
    }

    private fun discriminator() {
        creator.getMethodCreator("discriminator", String::class.java).use {
            val value = annotation(Entity::class.java)?.value
            val name =
                if (value != null && value != Mapper.IGNORED_FIELDNAME) value else entity.simpleName

            it.returnValue(it.load(name))
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
                constructor.loadClass(entity)
            )
            constructor.setParameterNames(arrayOf("mapper"))
            registerAnnotations(constructor)
            constructor.returnVoid()
        }
    }

    private fun registerAnnotations(constructor: MethodCreator) {
        val annotationMethod = ofMethod(generatedType, "annotation", "void", Annotation::class.java)
        annotations.forEach { annotation ->
            constructor.invokeVirtualMethod(
                annotationMethod,
                constructor.`this`,
                annotation.annotationBuilder(constructor)
            )
        }
    }
}
