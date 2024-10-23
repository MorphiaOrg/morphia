package dev.morphia.critter.parser.java

import dev.morphia.critter.parser.java.CritterParser.asmify
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.ChildFirst
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.objectweb.asm.Type

class CritterClassLoader(parent: ClassLoader?) :
    //    URLClassLoader(arrayOf(File("target/critter").toURI().toURL()), parent)
    ChildFirst(parent, mapOf()) {
    companion object {
        var output = "target/critter"
        var asm = false
    }

    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
        dump(name)
    }

    override fun loadClass(name: String?): Class<*> {
        return super.loadClass(name)
    }

    fun dump(name: String, mappings: Map<Type, Type> = mapOf()) {
        File(output).mkdirs()
        val outputFolder = File(output, File(name.replace('.', '/')).parent)
        val fileName = name.substringAfterLast('.')
        outputFolder.mkdirs()
        val bytes =
            typeDefinitions[name]
                ?: getResourceAsStream("${name.replace('.', '/')}.class")?.readBytes()

        if (bytes != null) {
            FileOutputStream(File(outputFolder, "$fileName.class")).use { it.write(bytes) }
            asmify(bytes, mappings, name, outputFolder, fileName)
        } else {
            throw ClassNotFoundException("Could not find $name")
        }
    }

    private fun asmify(
        bytes: ByteArray,
        mappings: Map<Type, Type>,
        name: String,
        outputFolder: File,
        fileName: String
    ) {
        if (!asm) return

        var asm = asmify(bytes)
        mappings.forEach { (type, mapping) ->
            asm =
                asm.replace(type.internalName, mapping.internalName)
                    .replace(type.descriptor, mapping.descriptor)
                    .replace(type.className, mapping.className)
        }
        if (!name.contains("__morphia")) {
            asm =
                asm.replace("Template", "")
                    .replace(".sources", ".sources.__morphia.example")
                    .replace("/sources", "/sources/__morphia/example")
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
}
