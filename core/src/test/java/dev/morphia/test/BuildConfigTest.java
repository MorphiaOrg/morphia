package dev.morphia.test;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.zafarkhaja.semver.Version;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static dev.morphia.test.TestBase.walk;
import static java.lang.String.format;
import static java.util.List.of;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BuildConfigTest {

    private static final String DOCS_ANTORA_YML = "../docs/antora.yml";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final Map antora;
    private final Map gitInfo;
    private final Model model;

    private final boolean master;

    public BuildConfigTest() throws IOException, XmlPullParserException {
        antora = antora();
        gitInfo = gitProperties();
        model = pom();
        master = "master".equals(gitInfo.get("git.branch"));
    }

    @Test
    public void testDocsConfig() throws IOException {
        Version pomVersion = Version.valueOf(model.getVersion());
        String url = model.getUrl();

        var updated = new LinkedHashMap<String, Object>();
        copy(updated, antora, "name");
        copy(updated, antora, "title");
        updated.put("version", format("%s.%s", pomVersion.getMajorVersion(), pomVersion.getMinorVersion()));
        if (master) {
            updated.put("prerelease", "-SNAPSHOT");
        }
        copy(updated, antora, "nav");
        copy(updated, antora, "asciidoc");
        Map<String, Object> attributes = walk(antora, of("asciidoc", "attributes"));
        attributes.put("version", previous(pomVersion).toString());

        String path;
        if (master) {
            path = "/blob/master";
        } else {
            path = format("/tree/%s.%s.x", pomVersion.getMajorVersion(), pomVersion.getMinorVersion());
        }
        attributes.put("srcRef", String.format("%s%s", url, path));

        SequenceWriter sw = objectMapper.writer().writeValues(new FileWriter(DOCS_ANTORA_YML));
        sw.write(updated);
    }

    private Object previous(Version version) {
        Version previous = Version.forIntegers(version.getMajorVersion(), version.getMinorVersion(),
                Math.max(version.getPatchVersion() - 1, 0));
        if (master) {
            previous = previous.setPreReleaseVersion("SNAPSHOT");
        }
        return previous;
    }

    private void copy(LinkedHashMap<String, Object> updated, Map antora, String key) {
        updated.put(key, antora.get(key));
    }

    @NotNull
    private Map gitProperties() throws IOException {
        Map gitInfo;
        try (InputStream inputStream = new FileInputStream("target/git.properties")) {
            var props = new Properties();
            props.load(inputStream);
            gitInfo = new TreeMap(props);
        }
        return gitInfo;
    }

    private Map antora() throws IOException {
        Map map;
        try (InputStream inputStream = new FileInputStream(DOCS_ANTORA_YML)) {
            map = objectMapper.readValue(inputStream, LinkedHashMap.class);
        }
        return map;
    }

    private <T> T walk(Map map, List<String> steps) {
        Object value = map;
        for (String step : steps) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(step);
            }
        }
        return (T) value;
    }

    private Model pom() throws IOException, XmlPullParserException {
        return new MavenXpp3Reader().read(new FileReader("../pom.xml"));
    }
}