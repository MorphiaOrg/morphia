///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.semver4j.Semver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Computes the build matrix values, prints them, and appends them to $GITHUB_OUTPUT when that is set:
 * <ul>
 * <li>mongo_versions: the newest usable patch release of each MongoDB line from 7.0 on</li>
 * <li>driver_versions: the newest patch release of each driver minor line from DRIVER_MIN (default 5.0.0) on</li>
 * <li>driver_snapshot: the newest driver snapshot from DRIVER_MIN on</li>
 * </ul>
 */
public class BuildMatrix {

    // releases of these lines up to and including the given patch refuse to start on Linux 6.19 and newer, which the
    // runners use (https://jira.mongodb.org/browse/SERVER-121912). A line is skipped until a later patch has an image.
    private static final Map<String, Semver> LAST_BROKEN = Map.of(
            "8.0", Semver.parse("8.0.34"),
            "8.3", Semver.parse("8.3.13"));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final Semver DRIVER_MINIMUM = Semver.parse(System.getenv().getOrDefault("DRIVER_MIN", "5.0.0"));

    public static void main(String... args) throws Exception {
        var outputs = new LinkedHashMap<String, Object>();
        outputs.put("mongo_versions", mongoVersions());
        outputs.put("driver_versions", driverVersions());
        outputs.put("driver_snapshot", driverSnapshot());

        var lines = outputs.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n", "", "\n"));
        System.out.print(lines);
        var githubOutput = System.getenv("GITHUB_OUTPUT");
        if (githubOutput != null) {
            Files.writeString(Path.of(githubOutput), lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static List<String> mongoVersions() throws IOException {
        // props to Chris Dellaway for the pointer to this
        var json = "https://downloads.mongodb.org/current.json";
        var document = MAPPER.readTree(new URL(json));

        var list = (ArrayNode)document.get("versions");

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(list.elements(), Spliterator.ORDERED), false)
                .map(d -> ((JsonNode)d.get("version")).asText())
                .map(Semver::parse)
                .filter(it -> it.getPreRelease().isEmpty() || it.getPreRelease().get(0).equals(""))
                .filter(it -> it.isGreaterThanOrEqualTo("7.0.0"))
                .map(BuildMatrix::usableVersion)
                .flatMap(Optional::stream)
                .map(it -> format("'%s'", it))
                .collect(toList());
    }

    /**
     * MongoDB lists a release before its Docker image is published, so use the newest patch release of the same minor
     * line that has an image and is newer than any known broken release.
     */
    private static Optional<Semver> usableVersion(Semver latest) {
        var line = latest.getMajor() + "." + latest.getMinor();
        var url = format("https://hub.docker.com/v2/repositories/library/mongo/tags?name=%s.&page_size=100", line);
        JsonNode tags;
        try {
            tags = MAPPER.readTree(new URL(url)).get("results");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list docker images for the " + line + " line", e);
        }
        var pattern = Pattern.compile(Pattern.quote(line) + "\\.\\d+");
        var version = StreamSupport.stream(tags.spliterator(), false)
                .map(t -> t.get("name").asText())
                .filter(name -> pattern.matcher(name).matches())
                .map(Semver::parse)
                .filter(it -> !LAST_BROKEN.containsKey(line) || it.isGreaterThan(LAST_BROKEN.get(line)))
                .max(Comparator.naturalOrder());
        if (version.isEmpty()) {
            System.err.println("No usable docker image found for the " + line + " line");
        }
        return version;
    }

    private static List<String> driverVersions() throws IOException {
        var url = "https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        var comparator = comparingInt(Semver::getMajor)
                             .thenComparingInt(Semver::getMinor)
                             .thenComparingInt(Semver::getPatch)
                             .reversed();
        var grouped = driverMetadataVersions(url).stream()
                          .filter(it -> it.getBuild().isEmpty())
                          .filter(it -> it.getPreRelease().isEmpty())
                          .collect(groupingBy(v -> Semver.of(v.getMajor(), v.getMinor(), 0), LinkedHashMap::new, toList()));
        var result = grouped.values().stream()
                            .peek(it -> it.sort(comparator))
                            .map(it -> it.get(0))
                            .map(it -> format("'%s'", it))
                            .collect(toList());
        Collections.reverse(result);
        return result;
    }

    private static Semver driverSnapshot() throws IOException {
        var url = "https://central.sonatype.com/repository/maven-snapshots/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        return driverMetadataVersions(url).stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("No driver snapshot found at " + url));
    }

    private static List<Semver> driverMetadataVersions(String url) throws IOException {
        var versions = XML_MAPPER.readTree(new URL(url))
                           .get("versioning")
                           .get("versions")
                           .get("version");
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                .map(JsonNode::asText)
                .map(Semver::parse)
                .filter(it -> it.isGreaterThanOrEqualTo(DRIVER_MINIMUM))
                .collect(toList());
    }
}
