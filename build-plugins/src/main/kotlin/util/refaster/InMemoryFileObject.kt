package util.refaster

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class InMemoryFileObject(name: String, val source: String) :
    SimpleJavaFileObject(
        URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.CLASS.extension),
        JavaFileObject.Kind.CLASS
    ) {
    private val content = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return content
    }

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return source
    }

    fun getBytes(): ByteArray {
        return content.toByteArray()
    }
}
