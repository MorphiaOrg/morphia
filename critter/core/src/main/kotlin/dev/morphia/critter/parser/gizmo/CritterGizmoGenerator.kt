package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.Generators
import dev.morphia.critter.parser.PropertyFinder
import dev.morphia.critter.parser.asm.AddFieldAccessorMethods
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

object CritterGizmoGenerator {
    fun generate(
        type: Class<*>,
        critterClassLoader: CritterClassLoader = CritterClassLoader(),
    ): GizmoEntityModelGenerator {
        val classNode = ClassNode()
        ClassReader(type.name).accept(classNode, 0)
        val propertyFinder = PropertyFinder(Generators.mapper, critterClassLoader)

        return entityModel(
            type,
            critterClassLoader,
            classNode,
            propertyFinder.find(type, classNode),
        )
    }

    fun fieldAccessors(entityType: Class<*>, fields: List<FieldNode>) =
        AddFieldAccessorMethods(entityType, fields).emit()

    fun accessor(entityType: Class<*>, critterClassLoader: CritterClassLoader, field: FieldNode) =
        PropertyAccessorGenerator(entityType, critterClassLoader, field).emit()

    fun propertyModelGenerator(
        entityType: Class<*>,
        critterClassLoader: CritterClassLoader,
        field: FieldNode,
    ) = PropertyModelGenerator(Generators.config, entityType, critterClassLoader, field).emit()

    fun entityModel(
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        classNode: ClassNode,
        properties: List<PropertyModelGenerator>,
    ): GizmoEntityModelGenerator {
        return GizmoEntityModelGenerator(type, critterClassLoader, classNode, properties).emit()
    }
}
