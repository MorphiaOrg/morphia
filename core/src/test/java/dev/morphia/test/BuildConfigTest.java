package dev.morphia.test;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.antwerkz.bottlerocket.Versions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.zafarkhaja.semver.Version;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static java.util.List.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BuildConfigTest {
    final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void testBuildMatrix() throws IOException {
        Map map;
        try (InputStream inputStream = new FileInputStream("../.github/workflows/build.yml")) {
            map = objectMapper.readValue(inputStream, LinkedHashMap.class);
        }
        assertEquals(walk(map, of("jobs", "Build", "with", "maven-flags")),
                String.format("-Dmongodb=%s -Dcode-audits", Versions.Version60.version()));

        checkForVersions(walk(map, of("jobs", "Test", "strategy", "matrix", "mongo")),
                Versions.Version60, Versions.Version50, Versions.Version44, Versions.Version42);

        try (InputStream inputStream = new FileInputStream("../.github/workflows/pull-request.yml")) {
            map = objectMapper.readValue(inputStream, LinkedHashMap.class);
        }

        assertEquals(walk(map, of("jobs", "Build", "with", "maven-flags")),
                String.format("-Dmongodb=%s", Versions.Version60.version()));
    }

    private static <T> T walk(Map map, List<String> steps) {
        Object value = map;
        for (String step : steps) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(step);
            }
        }
        return (T) value;
    }

    @Test
    public void testDocsConfig() throws IOException, XmlPullParserException {
        Version pomVersion = pomVersion();

        Map map;
        try (InputStream inputStream = new FileInputStream("../docs/antora.yml")) {
            map = objectMapper.readValue(inputStream, LinkedHashMap.class);
        }
        String version = map.get("version").toString();
        boolean master = pomVersion.getPatchVersion() == 0 && pomVersion.getPreReleaseVersion() != null;
        if (master) {
            assertEquals(map.get("prerelease"), "-SNAPSHOT");
        } else {
            assertNull(map.get("prerelease"));
        }

        assertEquals(version, format("%s.%s", pomVersion.getMajorVersion(), pomVersion.getMinorVersion()));
        map = walk(map, of("asciidoc", "attributes"));
        version = (String) map.get("version");
        var srcRef = (String) map.get("srcRef");
        if (master) {
            assertEquals(version, pomVersion.toString());
            assertTrue(srcRef.endsWith("/blob/master"));
        } else {
            var branch = format("%s.%s.x", pomVersion.getMajorVersion(), pomVersion.getMinorVersion());
            Version released = Version.forIntegers(pomVersion.getMajorVersion(), pomVersion.getMinorVersion(),
                    pomVersion.getPatchVersion() - 1);
            assertTrue(srcRef.endsWith(format("/tree/%s", branch)));
            assertEquals(version, released.toString());
        }
    }

    @NotNull
    private static Version pomVersion() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("../pom.xml"));

        Version pomVersion = Version.valueOf(model.getVersion());
        return pomVersion;
    }

    private static void checkForVersions(List<String> mongo, Versions... versions) {
        List<String> expected = Arrays.stream(versions)
                .map(v -> v.version().toString())
                .collect(Collectors.toList());
        assertEquals(mongo, expected);
    }

}
