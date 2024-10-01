package dev.morphia.critter.parser.gizmo

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

    init {
        generatedType = "${baseName}.${entity.simpleName}EntityModel"
        println("**************** generatedType = ${generatedType}")
        annotations = classNode.visibleAnnotations ?: emptyList()
    }

    fun emit(): GizmoEntityModelGenerator {
        builder.superClass(CritterEntityModel::class.java)

        creator.use { ctor() }

        return this
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
