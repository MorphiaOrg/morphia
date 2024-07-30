package dev.morphia.critter.parser

import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.generators.CritterEntityModelGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.mapping.Mapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class CritterGenerator(val classLoader: CritterClassLoader, val mapper: Mapper) {
    val propertyFinder = PropertyFinder(mapper, classLoader)

    fun generate(type: Class<*>): CritterEntityModel {
        val classNode = ClassNode()
        ClassReader(type.name).accept(classNode, 0)

        propertyFinder.find(type, classNode)

        return entityModel(type)
    }

    private fun entityModel(type: Class<*>): CritterEntityModel {
        val entityModelGenerator = CritterEntityModelGenerator(type)
        val className = entityModelGenerator.generatedType.className
        critterClassLoader.register(className, entityModelGenerator.emit())
        return critterClassLoader
            .loadClass(className)
            .getConstructor(Mapper::class.java)
            .newInstance(mapper) as CritterEntityModel
    }
}
