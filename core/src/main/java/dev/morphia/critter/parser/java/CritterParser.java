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

public class CritterParser {
    public static final CritterParser INSTANCE = new CritterParser();

    public File outputGenerated = null;

    private CritterParser() {
    }

    public String asmify(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        StringWriter traceWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(traceWriter);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, new ASMifier(), printWriter);
        classReader.accept(traceClassVisitor, 0);
        return traceWriter.toString();
    }

    public List<String> propertyAnnotations() {
        return Critter.propertyAnnotations.stream()
                .map(Type::getDescriptor)
                .collect(Collectors.toList());
    }

    public List<String> transientAnnotations() {
        return Critter.transientAnnotations.stream()
                .map(Type::getDescriptor)
                .collect(Collectors.toList());
    }
}
