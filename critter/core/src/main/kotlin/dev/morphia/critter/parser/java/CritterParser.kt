package dev.morphia.critter.parser.java

import dev.morphia.critter.Critter.Companion.propertyAnnotations
import dev.morphia.critter.Critter.Companion.transientAnnotations
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor

object CritterParser {
    var outputGenerated: File? = null

    fun asmify(bytes: ByteArray): String {
        val classReader = ClassReader(bytes)
        val traceWriter = StringWriter()
        val printWriter = PrintWriter(traceWriter)
        val traceClassVisitor = TraceClassVisitor(null, ASMifier(), printWriter)
        classReader.accept(traceClassVisitor, 0)
        return traceWriter.toString()
    }

    fun propertyAnnotations(): List<String> {
        return propertyAnnotations.map { it.descriptor }
    }

    fun transientAnnotations(): List<String> {
        return transientAnnotations.map { it.descriptor }
    }
}
