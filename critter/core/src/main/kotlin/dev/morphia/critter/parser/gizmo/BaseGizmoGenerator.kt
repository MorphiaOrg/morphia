package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.Critter.Companion.critterClassLoader
import dev.morphia.critter.Critter.Companion.critterPackage
import io.quarkus.gizmo.ClassCreator

open class BaseGizmoGenerator(val entity: Class<*>) {
    lateinit var generatedType: String

    val baseName = critterPackage(entity)
    val builder: ClassCreator.Builder by lazy {
        ClassCreator.builder()
            .classOutput { name, data -> critterClassLoader.register(name.replace('/', '.'), data) }
            .className(generatedType)
    }
    val creator: ClassCreator by lazy { builder.build() }
}
