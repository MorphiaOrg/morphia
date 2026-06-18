package dev.morphia.critter.parser.generator;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.morphia.critter.CritterClassLoader;

import org.junit.jupiter.api.Test;

import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;

public class DumpBytecodeTest {
    @Test
    public void dumpGeneratedClasses() throws Exception {
        CritterClassLoader loader = new CritterClassLoader();
        new CritterGenerator(defaultMapper()).generate(
                dev.morphia.critter.sources.Example.class, loader, false);

        Path outDir = Path.of("/tmp/critter-dump");
        Files.createDirectories(outDir);
        for (var entry : loader.getTypeDefinitions().entrySet()) {
            String name = entry.getKey();
            if (name.contains("NameModel") || name.contains("ExampleEntityModel")) {
                Path file = outDir.resolve(name.replace('.', '_') + ".class");
                Files.write(file, entry.getValue());
                System.out.println("Wrote: " + file);
            }
        }
    }
}
