package dev.morphia.test;

import java.io.File;
import java.io.FileReader;

import dev.morphia.MorphiaVersion30;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.semver4j.Semver;
import org.testng.annotations.Test;

import static dev.morphia.test.TestBase.GIT_ROOT;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class MorphiaVersionTest {
    @Test
    public void testVersion() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(new File(GIT_ROOT, "pom.xml").getAbsoluteFile()));

        var version = Semver.parse(model.getVersion());
        String minorVersion = format("%s%s", version.getMajor(), version.getMinor());
        //noinspection MisorderedAssertEqualsArguments
        assertEquals(MorphiaVersion30.class.getSimpleName().replaceAll("\\D", ""), minorVersion);
    }
}