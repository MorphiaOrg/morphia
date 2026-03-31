package dev.morphia.critter.parser.java;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import dev.morphia.critter.Critter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Singleton parser providing ASMifier-based bytecode inspection and annotation descriptor resolution utilities.
 */
public class CritterParser {
    /** The singleton instance of this parser. */
    public static final CritterParser INSTANCE = new CritterParser();

    /** Optional output directory for writing generated source files; {@code null} disables file output. */
    public File outputGenerated = null;

    private CritterParser() {
    }

    /**
     * Converts the given class bytecode to its ASMifier source representation.
     *
     * @param bytes the class bytecode
     * @return the ASMifier source code as a string
     */
    public String asmify(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        StringWriter traceWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(traceWriter);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, new ASMifier(), printWriter);
        classReader.accept(traceClassVisitor, 0);
        return traceWriter.toString();
    }

    /**
     * Returns the ASM descriptors of all annotation types that mark a field or method as a mapped property.
     *
     * @return list of property annotation descriptors
     */
    public List<String> propertyAnnotations() {
        return Critter.propertyAnnotations.stream()
                .map(Type::getDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * Returns the ASM descriptors of all annotation types that mark a field or method as transient (not persisted).
     *
     * @return list of transient annotation descriptors
     */
    public List<String> transientAnnotations() {
        return Critter.transientAnnotations.stream()
                .map(Type::getDescriptor)
                .collect(Collectors.toList());
    }
}
