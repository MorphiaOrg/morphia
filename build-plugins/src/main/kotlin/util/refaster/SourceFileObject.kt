package util.refaster

import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class SourceFileObject(name: String, val source: String) :
    SimpleJavaFileObject(
        URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
        JavaFileObject.Kind.SOURCE
    ) {
    override fun openOutputStream() = TODO()

    override fun getCharContent(ignoreEncodingErrors: Boolean) = source
}
