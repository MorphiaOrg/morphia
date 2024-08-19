package dev.morphia.critter.parser

import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.gizmo.GizmoEntityModelGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.mapping.Mapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class CritterGizmoGenerator(val classLoader: CritterClassLoader, val mapper: Mapper) {
    val propertyFinder = PropertyFinder(mapper, classLoader)

    fun generate(type: Class<*>): CritterEntityModel? {
        val classNode = ClassNode()
        ClassReader(type.name).accept(classNode, 0)
        val propertyNames = propertyFinder.find(type, classNode)

        return null // entityModel(type, propertyNames)
    }

    private fun entityModel(type: Class<*>, propertyNames: List<String>): CritterEntityModel {
        val entityModelGenerator = GizmoEntityModelGenerator(type, propertyNames)
        return critterClassLoader
            .loadClass(entityModelGenerator.generatedType)
            .getConstructor(Mapper::class.java)
            .newInstance(mapper) as CritterEntityModel
    }
}
