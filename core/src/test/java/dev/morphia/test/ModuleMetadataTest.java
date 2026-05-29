package dev.morphia.test;

import java.io.File;
import java.io.FileReader;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static dev.morphia.test.TestBase.GIT_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleMetadataTest {
    @Test
    public void coreJarDefinesAutomaticModuleName() throws Exception {
        assertAutomaticModuleName("core/pom.xml", "dev.morphia.core");
    }

    @Test
    public void kotlinJarDefinesAutomaticModuleName() throws Exception {
        assertAutomaticModuleName("kotlin/pom.xml", "dev.morphia.kotlin");
    }

    private static void assertAutomaticModuleName(String path, String expectedValue) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (FileReader fileReader = new FileReader(new File(GIT_ROOT, path).getAbsoluteFile())) {
            model = reader.read(fileReader);
        }

        Optional<Plugin> plugin = model.getBuild().getPlugins().stream()
                .filter(p -> "maven-jar-plugin".equals(p.getArtifactId()))
                .findFirst();

        assertTrue(plugin.isPresent(), "Expected maven-jar-plugin in " + path);

        Xpp3Dom configuration = (Xpp3Dom) plugin.get().getConfiguration();
        assertNotNull(configuration, "Expected maven-jar-plugin configuration in " + path);
        Xpp3Dom archive = configuration.getChild("archive");
        assertNotNull(archive, "Expected archive configuration in " + path);
        Xpp3Dom manifestEntries = archive.getChild("manifestEntries");
        assertNotNull(manifestEntries, "Expected manifestEntries configuration in " + path);
        Xpp3Dom automaticModuleName = manifestEntries.getChild("Automatic-Module-Name");
        assertNotNull(automaticModuleName, "Expected Automatic-Module-Name entry in " + path);
        assertEquals(automaticModuleName.getValue(), expectedValue);
    }
}
