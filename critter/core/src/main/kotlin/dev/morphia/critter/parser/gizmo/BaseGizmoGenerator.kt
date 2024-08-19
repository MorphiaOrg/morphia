package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.Critter.Companion.critterPackage

open class BaseGizmoGenerator(val entity: Class<*>) {
    val baseName: String
    lateinit var generatedType: String

    init {
        val `package` = critterPackage(entity)
        baseName = `package`
    }
}
