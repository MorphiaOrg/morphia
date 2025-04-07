package dev.morphia.test;

import java.io.FileReader;

import dev.morphia.MorphiaVersion25;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.semver4j.Semver;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class MorphiaVersionTest {
    @Test
    public void testVersion() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("../pom.xml"));

        var version = Semver.parse(model.getVersion());
        String minorVersion = format("%s%s", version.getMajor(), version.getMinor());
        //noinspection MisorderedAssertEqualsArguments
        assertEquals(MorphiaVersion25.class.getSimpleName().replaceAll("\\D", ""), minorVersion);
    }
}