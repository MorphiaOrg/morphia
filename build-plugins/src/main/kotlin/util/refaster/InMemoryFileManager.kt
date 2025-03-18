package util.refaster

import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager

class InMemoryFileManager : ForwardingJavaFileManager<StandardJavaFileManager> {
    val classBytes = mutableMapOf<String, InMemoryFileObject>()

    constructor(fileManager: StandardJavaFileManager) : super(fileManager)

    override fun getJavaFileForOutput(
        location: JavaFileManager.Location,
        className: String,
        kind: JavaFileObject.Kind,
        sibling: FileObject
    ): JavaFileObject {
        val fileObject = classBytes[className]
        return fileObject as JavaFileObject
    }
}
