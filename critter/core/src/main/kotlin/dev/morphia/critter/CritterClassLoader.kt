package dev.morphia.critter

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader

class CritterClassLoader(parent: ClassLoader?) : ByteArrayClassLoader.ChildFirst(parent, mapOf()) {
    fun register(name: String, bytes: ByteArray) {
        typeDefinitions[name] = bytes
    }

    internal fun bytes(name: String): ByteArray {
        // If already registered, return it
        typeDefinitions[name]?.let {
            return it
        }

        // Try to load from resources if it's a project class
        if (shouldRegister(name)) {
            val resourceName = name.replace('.', '/') + ".class"
            // Try both this classloader and parent classloader
            val resourceStream =
                getResourceAsStream(resourceName) ?: parent?.getResourceAsStream(resourceName)
            resourceStream?.use { stream ->
                val bytes = stream.readBytes()
                register(name, bytes)
                return bytes
            }
        }

        throw ClassNotFoundException(name)
    }

    override fun findClass(name: String): Class<*> {
        // Try to register from resources first if not already registered
        // Only register project classes to avoid LinkageError with third-party libraries
        if (!typeDefinitions.containsKey(name) && shouldRegister(name)) {
            getResource(name.replace('.', '/') + ".class")?.let { resource ->
                register(name, resource.readBytes())
            }
        }

        return super.findClass(name)
    }

    private fun shouldRegister(className: String): Boolean {
        // Only register classes from the dev.morphia.critter package
        // This avoids SecurityException (java.*, javax.*) and LinkageError (third-party libs)
        return className.startsWith("dev.morphia.critter.")
    }
}
