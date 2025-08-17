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
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.semver4j.Semver;

import static java.util.List.of;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Mojo(name = "docs-config", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class DocsConfig extends AbstractMojo {
    private static final File DOCS_ANTORA_YML;

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private boolean master;

    protected static File PROJECT_ROOT = new File(".").getAbsoluteFile();

    static {
        while (!new File(PROJECT_ROOT, ".git").exists()) {
            PROJECT_ROOT = PROJECT_ROOT.getParentFile();
        }
        DOCS_ANTORA_YML = new File(PROJECT_ROOT, "docs/antora.yml");
    }

    @Override
    public void execute() throws MojoExecutionException {
        Model model;
        Map antora;
        try {
            antora = antora();
            model = pom();
            String branch = gitProperties().get("git.branch").toString();

            if (validateBranch(branch)) {
                master = "master".equals(branch);
                var pomVersion = Semver.parse(model.getVersion());
                String url = model.getUrl();

                var updated = new LinkedHashMap<String, Object>();
                copy(updated, antora, "name");
                copy(updated, antora, "title");
                updated.put("version", String.format("%s.%s", pomVersion.getMajor(), pomVersion.getMinor()));
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
                    path = String.format("/tree/%s.%s.x", pomVersion.getMajor(), pomVersion.getMinor());
                }
                attributes.put("srcRef", String.format("%s%s", url, path));

                SequenceWriter sw = objectMapper.writer().writeValues(new FileWriter(DOCS_ANTORA_YML));
                sw.write(updated);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean validateBranch(String branch) {
        Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.x");

        return "master".equals(branch) || pattern.matcher(branch).matches();
    }

    private Semver previous(Semver version) {
        var previous = Semver.of(version.getMajor(), version.getMinor(),
                Math.max(version.getPatch() - 1, 0));
        if (master) {
            previous = previous.withPreRelease("SNAPSHOT");
        }
        return previous.build();
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