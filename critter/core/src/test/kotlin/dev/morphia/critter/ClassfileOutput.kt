package dev.morphia.critter

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.jboss.windup.decompiler.fernflower.FernflowerDecompiler
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor

object ClassfileOutput {
    fun CritterClassLoader.dump(className: String, outputDir: Path = Path.of("target/dumps/")) {
        val bytes = bytes(className)
        listOf(
                "javap" to dumpBytecode(bytes),
                "asm" to dumpAsmSource(bytes),
                "java" to decompile(bytes),
            )
            .forEach { (ext, text) ->
                val output = outputDir.resolve("${className.replace('.', '/')}.$ext")
                output.toFile().parentFile.mkdirs()
                output.writeText(text)
            }
    }

    fun dump(className: String, bytes: ByteArray, outputDir: Path = Path.of("target/dumps/")) {
        listOf(
                "javap" to dumpBytecode(bytes),
                "asm" to dumpAsmSource(bytes),
                "java" to decompile(bytes),
            )
            .forEach { (ext, text) ->
                val output = outputDir.resolve("$className.$ext")
                output.toFile().parentFile.mkdirs()
                output.writeText(text)
            }
    }

    /**
     * Dumps the bytecode of a given class as human-readable text using ASM's TraceClassVisitor.
     *
     * @param clazz the class to dump
     * @return a string containing the textual representation of the class bytecode
     */
    fun dumpBytecode(clazz: Class<*>): String {
        val classReader = ClassReader(clazz.name)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        val traceClassVisitor = TraceClassVisitor(printWriter)
        classReader.accept(traceClassVisitor, 0)
        return stringWriter.toString()
    }

    /**
     * Dumps the bytecode from a byte array as human-readable text using ASM's TraceClassVisitor.
     *
     * @param bytecode the raw bytecode to dump
     * @return a string containing the textual representation of the class bytecode
     */
    fun dumpBytecode(bytecode: ByteArray): String {
        val classReader = ClassReader(bytecode)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        val traceClassVisitor = TraceClassVisitor(printWriter)
        classReader.accept(traceClassVisitor, 0)
        return stringWriter.toString()
    }

    /**
     * Dumps the ASM source code that would generate the given class using ASM's ASMifier. This
     * produces Java source code that uses the ASM API to recreate the class.
     *
     * @param clazz the class to dump
     * @return a string containing the ASM API source code
     */
    fun dumpAsmSource(clazz: Class<*>): String {
        val classReader = ClassReader(clazz.name)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        val traceClassVisitor = TraceClassVisitor(null, ASMifier(), printWriter)
        classReader.accept(traceClassVisitor, 0)
        return stringWriter.toString()
    }

    /**
     * Dumps the ASM source code that would generate the class from a byte array using ASM's
     * ASMifier. This produces Java source code that uses the ASM API to recreate the class.
     *
     * @param bytecode the raw bytecode to dump
     * @return a string containing the ASM API source code
     */
    fun dumpAsmSource(bytecode: ByteArray): String {
        val classReader = ClassReader(bytecode)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        val traceClassVisitor = TraceClassVisitor(null, ASMifier(), printWriter)
        classReader.accept(traceClassVisitor, 0)
        return stringWriter.toString()
    }

    /**
     * Decompiles the given class to Java source code using Fernflower.
     *
     * @param clazz the class to decompile
     * @return a string containing the decompiled Java source code
     */
    fun decompile(clazz: Class<*>): String {
        val className = clazz.name.replace('.', '/') + ".class"
        val bytecode =
            clazz.classLoader.getResourceAsStream(className)?.readBytes()
                ?: throw IllegalArgumentException("Cannot find class file for ${clazz.name}")
        return decompile(bytecode)
    }

    /**
     * Decompiles bytecode to Java source code using Fernflower.
     *
     * @param bytecode the raw bytecode to decompile
     * @return a string containing the decompiled Java source code
     */
    fun decompile(bytecode: ByteArray): String {
        val tempDir = Files.createTempDirectory("fernflower")
        val outputDir = Files.createTempDirectory("fernflower-output")

        try {
            // Write bytecode to a temporary class file
            val classFile = tempDir.resolve("TempClass.class")
            Files.write(classFile, bytecode)

            // Decompile using FernflowerDecompiler
            val decompiler = FernflowerDecompiler()
            decompiler.decompileClassFile(tempDir, classFile, outputDir)

            // Read the decompiled source
            val decompiledFile = outputDir.resolve("TempClass.java")
            return if (Files.exists(decompiledFile)) {
                decompiledFile.readText()
            } else {
                // If exact name doesn't exist, find any .java file in output
                Files.walk(outputDir)
                    .filter { it.toString().endsWith(".java") }
                    .findFirst()
                    .map { it.readText() }
                    .orElse("// Decompilation failed")
            }
        } finally {
            tempDir.toFile().deleteRecursively()
            outputDir.toFile().deleteRecursively()
        }
    }
}
