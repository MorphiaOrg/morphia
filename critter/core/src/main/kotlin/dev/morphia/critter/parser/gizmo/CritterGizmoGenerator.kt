package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.Critter
import dev.morphia.critter.parser.Generators
import dev.morphia.critter.parser.PropertyFinder
import dev.morphia.critter.parser.asm.AddFieldAccessorMethods
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

object CritterGizmoGenerator {
    fun generate(type: Class<*>): GizmoEntityModelGenerator {
        val classNode = ClassNode()
        ClassReader(type.name).accept(classNode, 0)
        val propertyFinder = PropertyFinder(Generators.mapper, Critter.Companion.critterClassLoader)

        return entityModel(type, classNode, propertyFinder.find(type, classNode))
    }

    fun fieldAccessors(entityType: Class<*>, fields: List<FieldNode>) =
        AddFieldAccessorMethods(entityType, fields).emit()

    fun accessor(entityType: Class<*>, field: FieldNode) =
        PropertyAccessorGenerator(entityType, field).emit()

    fun propertyModelGenerator(entityType: Class<*>, field: FieldNode) =
        PropertyModelGenerator(Generators.config, entityType, field).emit()

    fun entityModel(
        type: Class<*>,
        classNode: ClassNode,
        properties: List<PropertyModelGenerator>
    ): GizmoEntityModelGenerator {
        return GizmoEntityModelGenerator(type, classNode, properties).emit()
    }
}
