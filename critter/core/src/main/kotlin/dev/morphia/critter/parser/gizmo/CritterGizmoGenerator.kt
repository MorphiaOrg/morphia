package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.Generators
import dev.morphia.critter.parser.PropertyFinder
import dev.morphia.critter.parser.asm.AddFieldAccessorMethods
import dev.morphia.critter.parser.asm.AddMethodAccessorMethods
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

object CritterGizmoGenerator {
    fun generate(
        type: Class<*>,
        critterClassLoader: CritterClassLoader = CritterClassLoader(),
        runtimeMode: Boolean = false,
    ): GizmoEntityModelGenerator {
        val classNode = ClassNode()
        val resourceName = type.name.replace('.', '/') + ".class"
        val inputStream =
            type.classLoader.getResourceAsStream(resourceName)
                ?: throw IllegalArgumentException("Could not find class file for ${type.name}")
        ClassReader(inputStream).accept(classNode, 0)
        val propertyFinder = PropertyFinder(Generators.mapper, critterClassLoader, runtimeMode)

        return entityModel(
            type,
            critterClassLoader,
            classNode,
            propertyFinder.find(type, classNode),
        )
    }

    fun fieldAccessors(entityType: Class<*>, fields: List<FieldNode>) =
        AddFieldAccessorMethods(entityType, fields).emit()

    fun methodAccessors(entityType: Class<*>, methods: List<MethodNode>) =
        AddMethodAccessorMethods(entityType, methods).emit()

    fun accessor(entityType: Class<*>, critterClassLoader: CritterClassLoader, field: FieldNode) =
        PropertyAccessorGenerator(entityType, critterClassLoader, field).emit()

    fun accessor(entityType: Class<*>, critterClassLoader: CritterClassLoader, method: MethodNode) =
        PropertyAccessorGenerator(entityType, critterClassLoader, method).emit()

    fun varHandleAccessor(
        entityType: Class<*>,
        critterClassLoader: CritterClassLoader,
        field: FieldNode,
    ) = VarHandleAccessorGenerator(entityType, critterClassLoader, field).emit()

    fun varHandleAccessor(
        entityType: Class<*>,
        critterClassLoader: CritterClassLoader,
        method: MethodNode,
    ) = VarHandleAccessorGenerator(entityType, critterClassLoader, method).emit()

    fun propertyModelGenerator(
        entityType: Class<*>,
        critterClassLoader: CritterClassLoader,
        field: FieldNode,
    ) = PropertyModelGenerator(Generators.config, entityType, critterClassLoader, field).emit()

    fun propertyModelGenerator(
        entityType: Class<*>,
        critterClassLoader: CritterClassLoader,
        method: MethodNode,
    ) = PropertyModelGenerator(Generators.config, entityType, critterClassLoader, method).emit()

    fun entityModel(
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        classNode: ClassNode,
        properties: List<PropertyModelGenerator>,
    ): GizmoEntityModelGenerator {
        return GizmoEntityModelGenerator(type, critterClassLoader, classNode, properties).emit()
    }
}
