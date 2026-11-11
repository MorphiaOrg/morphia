///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.semver4j.Semver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class BuildMatrix {

    // releases of these lines up to and including the given patch refuse to start on Linux 6.19 and newer, which the
    // runners use (https://jira.mongodb.org/browse/SERVER-121912). A line is skipped until a later patch has an image.
    private static final Map<String, Semver> LAST_BROKEN = Map.of(
            "8.0", Semver.parse("8.0.34"),
            "8.3", Semver.parse("8.3.13"));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var json = "https://downloads.mongodb.org/current.json";
        var document = MAPPER.readTree(new URL(json));

        var list = (ArrayNode)document.get("versions");

        var result = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(list.elements(), Spliterator.ORDERED), false)
                .map(d -> ((JsonNode)d.get("version")).asText())
                .map(Semver::parse)
                .filter(it -> it.getPreRelease().isEmpty() || it.getPreRelease().get(0).equals(""))
                .filter(it -> it.isGreaterThanOrEqualTo("7.0.0"))
                .map(BuildMatrix::usableVersion)
                .flatMap(Optional::stream)
                .map(it -> format("'%s'", it))
                .collect(Collectors.toList());
        System.out.println(result);
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
}
