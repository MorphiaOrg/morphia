package dev.morphia.critter.parser

import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.accessor
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.fieldAccessors
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.propertyModelGenerator
import dev.morphia.critter.parser.gizmo.PropertyModelGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser
import dev.morphia.mapping.Mapper
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class PropertyFinder(mapper: Mapper, val classLoader: CritterClassLoader) {
    private val providerMap =
        mapper.config.propertyAnnotationProviders().associateBy { it.provides() }

    fun find(entityType: Class<*>, classNode: ClassNode): List<PropertyModelGenerator> {
        val models = mutableListOf<PropertyModelGenerator>()
        val methods = discoverPropertyMethods(classNode)
        if (methods.isEmpty()) {
            val fields = discoverFields(classNode)
            classLoader.register(entityType.name, fieldAccessors(entityType, fields))
            fields.forEach { field ->
                accessor(entityType, field)
                models += propertyModelGenerator(entityType, field)
            }
        } else {
            TODO()
        }

        return models
    }

    private fun isPropertyAnnotated(
        annotationNodes: MutableList<AnnotationNode>?,
        allowUnannotated: Boolean
    ): Boolean {
        val annotations = annotationNodes ?: listOf()
        val keys = providerMap.keys.map { Type.getType(it).descriptor }
        return allowUnannotated || annotations.any { a -> a.desc in keys }
    }

    private fun discoverFields(classNode: ClassNode) =
        classNode.fields
            .filter { field ->
                (field.visibleAnnotations ?: listOf())
                    .map { a -> a.desc }
                    .none { desc -> desc in CritterParser.transientAnnotations() }
            }
            .filter { isPropertyAnnotated(it.visibleAnnotations, true) }

    private fun discoverPropertyMethods(classNode: ClassNode) =
        classNode.methods
            .filter { it.name.startsWith("get") && it.parameters.isEmpty() }
            .filter { isPropertyAnnotated(it.visibleAnnotations, false) }
}
