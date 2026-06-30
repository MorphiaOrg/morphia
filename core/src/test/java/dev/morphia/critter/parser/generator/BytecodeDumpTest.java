package dev.morphia.critter.parser.generator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.sources.Example;
import dev.morphia.critter.sources.MethodExample;
import dev.morphia.mapping.CritterMapperTestEntity;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.Book;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;

/**
 * Dumps readable ASM Textifier output for all classes generated from 5 representative entities
 * into target/critter-bytecode/, preserving the package directory hierarchy.
 */
public class BytecodeDumpTest {

    private static final List<Class<?>> ENTITIES = List.of(
            Example.class,
            MethodExample.class,
            Author.class,
            Book.class,
            CritterMapperTestEntity.class);

    @Test
    public void dumpBytecodeAsText() throws Exception {
        Path outRoot = Path.of("target/critter-bytecode");

        for (Class<?> entity : ENTITIES) {
            CritterClassLoader loader = new CritterClassLoader();
            new CritterGenerator(defaultMapper()).generate(entity, loader, false);

            for (Map.Entry<String, byte[]> entry : loader.getTypeDefinitions().entrySet()) {
                String className = entry.getKey();
                byte[] bytes = entry.getValue();

                Path outFile = outRoot.resolve(className.replace('.', '/') + ".txt");
                Files.createDirectories(outFile.getParent());

                StringWriter sw = new StringWriter();
                new ClassReader(bytes).accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(sw)), 0);
                Files.writeString(outFile, sw.toString());
            }
        }
    }
}
