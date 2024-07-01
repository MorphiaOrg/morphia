package dev.morphia.critter.parser.java

import io.quarkus.gizmo.ClassOutput
import java.io.File
import java.io.FileOutputStream
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.ChildFirst

class CritterClassLoader(parent: ClassLoader?) : ChildFirst(parent, mapOf()), ClassOutput {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
    }

    override fun write(name: String, data: ByteArray) {
        register(name, data)
    }

    fun dump(output: String) {
        typeDefinitions.forEach { (name, bytes) ->
            val name1 = File(name.replace('.', '/')).name
            val file = File(output, "$name1.class")
            println("**************** dumping '$name' to file $file")
            FileOutputStream(file).write(bytes)
        }

    }
}
