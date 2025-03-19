package util.refaster

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class InMemoryFileObject(name: String) :
    SimpleJavaFileObject(
        URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.CLASS.extension),
        JavaFileObject.Kind.CLASS
    ) {
    private val content = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return content
    }

    override fun openInputStream(): InputStream {
        return ByteArrayInputStream(content.toByteArray())
    }

    fun getBytes(): ByteArray {
        return content.toByteArray()
    }
}
