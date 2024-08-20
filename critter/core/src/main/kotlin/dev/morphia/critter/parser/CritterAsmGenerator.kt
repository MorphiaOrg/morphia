package dev.morphia.critter.parser

import dev.morphia.critter.parser.asm.CritterEntityModelGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class CritterAsmGenerator(val classLoader: CritterClassLoader, val mapper: Mapper) {
    val propertyFinder = PropertyFinder(mapper, classLoader)

    fun generate(type: Class<*>): CritterEntityModel {
        val classNode = ClassNode()
        ClassReader(type.name).accept(classNode, 0)
        val models = propertyFinder.find(type, classNode)

        return entityModel(type, models)
    }

    private fun entityModel(type: Class<*>, models: List<String>): CritterEntityModel {
        val entityModelGenerator = CritterEntityModelGenerator(type, models)
        val className = entityModelGenerator.generatedType.className
        critterClassLoader.register(className, entityModelGenerator.emit())
        return critterClassLoader
            .loadClass(className)
            .getConstructor(Mapper::class.java)
            .newInstance(mapper) as CritterEntityModel
    }
}
