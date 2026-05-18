package dev.morphia.test;

import java.io.File;
import java.nio.file.Files;

import org.testng.annotations.Test;

import static dev.morphia.test.TestBase.GIT_ROOT;
import static org.testng.Assert.assertTrue;

public class ModuleMetadataTest {
    @Test
    public void coreJarDefinesAutomaticModuleName() throws Exception {
        assertPomContains("core/pom.xml", "<Automatic-Module-Name>dev.morphia.core</Automatic-Module-Name>");
    }

    @Test
    public void kotlinJarDefinesAutomaticModuleName() throws Exception {
        assertPomContains("kotlin/pom.xml", "<Automatic-Module-Name>dev.morphia.kotlin</Automatic-Module-Name>");
    }

    private static void assertPomContains(String path, String value) throws Exception {
        String pom = Files.readString(new File(GIT_ROOT, path).toPath());
        assertTrue(pom.contains(value), "Expected " + path + " to contain " + value);
    }
}
