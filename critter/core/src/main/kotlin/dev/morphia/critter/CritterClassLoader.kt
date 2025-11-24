package dev.morphia.critter

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader

class CritterClassLoader(parent: ClassLoader?) : ByteArrayClassLoader.ChildFirst(parent, mapOf()) {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
    }

    internal fun bytes(name: String) = typeDefinitions[name] ?: throw ClassNotFoundException(name)

    override fun findClass(name: String): Class<*> {
        val findClass = super.findClass(name)
        getResource(name.replace('.', '/') + ".class")?.let { resource ->
            register(name, resource.readBytes())
        }

        return findClass
    }
}
