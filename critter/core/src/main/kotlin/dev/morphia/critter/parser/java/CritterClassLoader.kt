package dev.morphia.critter.parser.java

import io.quarkus.gizmo.ClassOutput
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader

class CritterClassLoader(parent: ClassLoader) : ByteArrayClassLoader(parent, mapOf()), ClassOutput {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
    }

    override fun write(name: String, data: ByteArray) {
        register(name, data)
    }
}
