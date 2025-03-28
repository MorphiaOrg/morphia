package dev.morphia.test;

import java.io.FileReader;

import com.github.zafarkhaja.semver.Version;

import dev.morphia.MorphiaVersion25;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class MorphiaVersionTest {
    @Test
    public void testVersion() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("../pom.xml"));

        Version version = Version.valueOf(model.getVersion());
        String minorVersion = format("%s%s", version.getMajorVersion(), version.getMinorVersion());
        //noinspection MisorderedAssertEqualsArguments
        assertEquals(MorphiaVersion25.class.getSimpleName().replaceAll("\\D", ""), minorVersion);
    }
}