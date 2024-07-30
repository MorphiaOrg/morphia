package dev.morphia.critter.parser

import dev.morphia.config.PropertyAnnotationProvider
import dev.morphia.critter.firstToLowerCase
import dev.morphia.critter.parser.generators.AddFieldAccessorMethods
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser
import dev.morphia.mapping.Mapper
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class PropertyFinder(mapper: Mapper, val classLoader: CritterClassLoader) {
    val providerMap: Map<Class<out Any>, PropertyAnnotationProvider<*>>

    init {
        providerMap = mapper.config.propertyAnnotationProviders().associateBy { it.provides() }
    }

    fun find(type: Class<*>, classNode: ClassNode) {
        var properties = discoverPropertyMethods(classNode)
        var methodsFound = properties.isNotEmpty()
        if (!methodsFound) {
            properties = discoverFields(classNode)
            classLoader.register(type.name, AddFieldAccessorMethods(type, properties).emit())
        }
        // generate property models now.  will need more data than just "Type"
    }

    private fun isPropertyAnnotated(
        annotationNodes: MutableList<AnnotationNode>?,
        allowUnannotated: Boolean
    ): Boolean {
        val annotations = annotationNodes ?: listOf()
        val keys = providerMap.keys.map { Type.getType(it).descriptor }
        return (allowUnannotated && annotations.isEmpty()) ||
            annotations
                .map { a -> a.desc }
                .any { desc -> desc in keys && desc !in CritterParser.transientAnnotations() }
    }

    private fun discoverFields(classNode: ClassNode): Map<String, Type> {
        return classNode.fields
            .filter { isPropertyAnnotated(it.visibleAnnotations, true) }
            .associate { field -> field.name to Type.getType(field.desc) }
    }

    private fun discoverPropertyMethods(classNode: ClassNode): Map<String, Type> {
        return classNode.methods
            .filter { it.name.startsWith("get") && it.parameters.isEmpty() }
            .filter { isPropertyAnnotated(it.visibleAnnotations, false) }
            .associate { method ->
                method.name.drop(3).firstToLowerCase() to Type.getMethodType(method.desc)
            }
    }
}
