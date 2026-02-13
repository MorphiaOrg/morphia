package dev.morphia.critter.parser

import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.accessor
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.fieldAccessors
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.methodAccessors
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator.propertyModelGenerator
import dev.morphia.critter.parser.gizmo.PropertyModelGenerator
import dev.morphia.critter.parser.java.CritterParser
import dev.morphia.mapping.Mapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

class PropertyFinder(mapper: Mapper, val classLoader: CritterClassLoader) {
    private val providerMap =
        mapper.config.propertyAnnotationProviders().associateBy { it.provides() }

    fun find(entityType: Class<*>, classNode: ClassNode): List<PropertyModelGenerator> {
        val models = mutableListOf<PropertyModelGenerator>()
        val methods = discoverPropertyMethods(classNode)
        if (methods.isEmpty()) {
            val fields = discoverAllFields(entityType, classNode)
            classLoader.register(entityType.name, fieldAccessors(entityType, fields))
            fields.forEach { field ->
                accessor(entityType, classLoader, field)
                models += propertyModelGenerator(entityType, classLoader, field)
            }
        } else {
            classLoader.register(entityType.name, methodAccessors(entityType, methods))
            methods.forEach { method ->
                accessor(entityType, classLoader, method)
                models += propertyModelGenerator(entityType, classLoader, method)
            }
        }

        return models
    }

    private fun isPropertyAnnotated(
        annotationNodes: MutableList<AnnotationNode>?,
        allowUnannotated: Boolean,
    ): Boolean {
        val annotations = annotationNodes ?: listOf()
        val keys = providerMap.keys.map { Type.getType(it).descriptor }
        return allowUnannotated || annotations.any { a -> a.desc in keys }
    }

    private fun discoverAllFields(entityType: Class<*>, classNode: ClassNode): List<FieldNode> {
        val fields = mutableListOf<FieldNode>()
        val seen = mutableSetOf<String>()
        var current: Class<*>? = entityType
        var currentNode: ClassNode? = classNode

        while (current != null && current != Object::class.java) {
            val node = currentNode ?: readClassNode(current) ?: break
            discoverFields(node).filter { seen.add(it.name) }.let { fields.addAll(it) }
            current = current.superclass
            currentNode = null
        }
        return fields
    }

    private fun readClassNode(type: Class<*>): ClassNode? {
        val resourceName = type.name.replace('.', '/') + ".class"
        val inputStream = type.classLoader.getResourceAsStream(resourceName) ?: return null
        val node = ClassNode()
        ClassReader(inputStream).accept(node, 0)
        return node
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
            .filter { it.name.startsWith("get") && Type.getArgumentTypes(it.desc).isEmpty() }
            .filter { isPropertyAnnotated(it.visibleAnnotations, false) }
}
