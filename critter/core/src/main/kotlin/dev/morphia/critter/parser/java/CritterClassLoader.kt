package dev.morphia.critter.parser.java

import dev.morphia.critter.parser.java.CritterParser.asmify
import io.quarkus.gizmo.ClassOutput
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.ChildFirst
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.objectweb.asm.Type

class CritterClassLoader(parent: ClassLoader?) : ChildFirst(parent, mapOf()), ClassOutput {
    companion object {
        var debug = false
        var output = "target/critter"
    }

    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
        dump(name)
    }

    fun dump(name: String, mappings: Map<Type, Type> = mapOf()) {
        File(output).mkdirs()
        val outputFolder = File(output, File(name.replace('.', '/')).parent)
        val fileName = name.substringAfterLast('.')
        outputFolder.mkdirs()

        val bytes =
            typeDefinitions[name]
                ?: getResourceAsStream("${name.replace('.', '/')}.class").readBytes()

        FileOutputStream(File(outputFolder, "$fileName.class")).use { it.write(bytes) }
        var asm = asmify(bytes)
        mappings.forEach { (type, mapping) ->
            asm =
                asm.replace(type.internalName, mapping.internalName)
                    .replace(type.descriptor, mapping.descriptor)
                    .replace(type.className, mapping.className)
        }
        val source = Roaster.parse(JavaClassSource::class.java, asm)
        FileWriter(File(outputFolder, "$fileName.asm")).use { it.write(source.toString()) }
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
}
