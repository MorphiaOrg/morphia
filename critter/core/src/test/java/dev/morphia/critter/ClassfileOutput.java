package dev.morphia.critter;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.jboss.windup.decompiler.fernflower.FernflowerDecompiler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class ClassfileOutput {

    public static void dump(CritterClassLoader classLoader, String className) throws Exception {
        dump(classLoader, className, Path.of("target/dumps/"));
    }

    public static void dump(CritterClassLoader classLoader, String className, Path outputDir) throws Exception {
        byte[] bytes = classLoader.getTypeDefinitions().get(className);
        if (bytes == null) {
            String resourceName = className.replace('.', '/') + ".class";
            InputStream stream = classLoader.getResourceAsStream(resourceName);
            if (stream == null)
                return;
            bytes = stream.readAllBytes();
        }
        String[][] outputs = {
                { "javap", dumpBytecode(bytes) },
                { "asm", dumpAsmSource(bytes) },
                { "java", decompile(bytes) }
        };
        for (String[] entry : outputs) {
            Path output = outputDir.resolve(className.replace('.', '/') + "." + entry[0]);
            output.toFile().getParentFile().mkdirs();
            Files.writeString(output, entry[1]);
        }
    }

    public static void dump(String className, byte[] bytes) throws Exception {
        dump(className, bytes, Path.of("target/dumps/"));
    }

    public static void dump(String className, byte[] bytes, Path outputDir) throws Exception {
        String[][] outputs = {
                { "javap", dumpBytecode(bytes) },
                { "asm", dumpAsmSource(bytes) },
                { "java", decompile(bytes) }
        };
        for (String[] entry : outputs) {
            Path output = outputDir.resolve(className + "." + entry[0]);
            output.toFile().getParentFile().mkdirs();
            Files.writeString(output, entry[1]);
        }
    }

    public static String dumpBytecode(Class<?> clazz) throws Exception {
        ClassReader classReader = new ClassReader(clazz.getName());
        StringWriter sw = new StringWriter();
        classReader.accept(new TraceClassVisitor(new PrintWriter(sw)), 0);
        return sw.toString();
    }

    public static String dumpBytecode(byte[] bytecode) {
        ClassReader classReader = new ClassReader(bytecode);
        StringWriter sw = new StringWriter();
        classReader.accept(new TraceClassVisitor(new PrintWriter(sw)), 0);
        return sw.toString();
    }

    public static String dumpAsmSource(Class<?> clazz) throws Exception {
        ClassReader classReader = new ClassReader(clazz.getName());
        StringWriter sw = new StringWriter();
        classReader.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(sw)), 0);
        return sw.toString();
    }

    public static String dumpAsmSource(byte[] bytecode) {
        ClassReader classReader = new ClassReader(bytecode);
        StringWriter sw = new StringWriter();
        classReader.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(sw)), 0);
        return sw.toString();
    }

    public static String decompile(Class<?> clazz) throws Exception {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        InputStream stream = clazz.getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IllegalArgumentException("Cannot find class file for " + clazz.getName());
        }
        return decompile(stream.readAllBytes());
    }

    public static String decompile(byte[] bytecode) throws Exception {
        Path tempDir = Files.createTempDirectory("fernflower");
        Path outputDir = Files.createTempDirectory("fernflower-output");
        try {
            Path classFile = tempDir.resolve("TempClass.class");
            Files.write(classFile, bytecode);

            FernflowerDecompiler decompiler = new FernflowerDecompiler();
            decompiler.decompileClassFile(tempDir, classFile, outputDir);

            Path decompiledFile = outputDir.resolve("TempClass.java");
            if (Files.exists(decompiledFile)) {
                return Files.readString(decompiledFile);
            }
            return Files.walk(outputDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .findFirst()
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (Exception e) {
                            return "// Decompilation failed";
                        }
                    })
                    .orElse("// Decompilation failed");
        } finally {
            deleteRecursively(tempDir);
            deleteRecursively(outputDir);
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir))
            return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
