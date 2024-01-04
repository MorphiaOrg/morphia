package util;

import java.io.File;
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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static java.util.List.of;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Mojo(name = "docs-config", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class DocsConfig extends AbstractMojo {
    private static final String DOCS_ANTORA_YML = "docs/antora.yml";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private boolean master;

    protected static File PROJECT_ROOT = new File(".").getAbsoluteFile();

    static {
        while (!new File(PROJECT_ROOT, ".git").exists()) {
            PROJECT_ROOT = PROJECT_ROOT.getParentFile();
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        Model model;
        Map antora;
        try {
            antora = antora();
            model = pom();
            master = "master".equals(gitProperties().get("git.branch"));

            Version pomVersion = Version.valueOf(model.getVersion());
            String url = model.getUrl();

            var updated = new LinkedHashMap<String, Object>();
            copy(updated, antora, "name");
            copy(updated, antora, "title");
            updated.put("version", String.format("%s.%s", pomVersion.getMajorVersion(), pomVersion.getMinorVersion()));
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
                path = String.format("/tree/%s.%s.x", pomVersion.getMajorVersion(), pomVersion.getMinorVersion());
            }
            attributes.put("srcRef", String.format("%s%s", url, path));

            SequenceWriter sw = objectMapper.writer().writeValues(new FileWriter(DOCS_ANTORA_YML));
            sw.write(updated);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
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

    private Map gitProperties() throws IOException {
        Map gitInfo;
        try (InputStream inputStream = new FileInputStream(new File(PROJECT_ROOT, "core/target/git.properties"))) {
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
        return new MavenXpp3Reader().read(new FileReader(new File(PROJECT_ROOT, "pom.xml")));
    }
}