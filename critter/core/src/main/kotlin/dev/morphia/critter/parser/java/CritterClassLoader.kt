package dev.morphia.critter.parser.java

import io.quarkus.gizmo.ClassOutput
import java.io.File
import java.io.FileOutputStream
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.ChildFirst

class CritterClassLoader(parent: ClassLoader?) : ChildFirst(parent, mapOf()), ClassOutput {
    var debug = false

    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
        if (debug) dump("target/critter/")
    }

    override fun findClass(name: String): Class<*> {
        val findClass = super.findClass(name)
        getResource(name.replace('.', '/') + ".class")?.let { resource ->
            register(name, resource.readBytes())
        }

        return findClass
    }

    override fun write(name: String, data: ByteArray) {
        register(name, data)
    }

    private fun dump(output: String) {
        File(output).mkdirs()
        typeDefinitions.forEach { (name, bytes) ->
            val name1 = File(name.replace('.', '/')).name
            FileOutputStream(File(output, "$name1.class")).write(bytes)
        }
    }
}
