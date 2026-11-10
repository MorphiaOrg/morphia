///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.semver4j.Semver;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class BuildMatrix {

    // these releases refuse to start on Linux 6.19 and newer, which the runners use
    // (https://jira.mongodb.org/browse/SERVER-121912). An earlier patch is used instead until a later one ships.
    private static final Set<String> BROKEN = Set.of("8.0.34", "8.3.13");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var json = "https://downloads.mongodb.org/current.json";
        var mapper = new ObjectMapper();

        var document = mapper.readTree(new URL(json));

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
     * MongoDB lists a release before its Docker image is published, so walk back through the patch releases of the
     * same minor line until one has an image and isn't known to be broken.
     */
    private static Optional<Semver> usableVersion(Semver latest) {
        for (int patch = latest.getPatch(); patch >= 0; patch--) {
            var candidate = Semver.of(latest.getMajor(), latest.getMinor(), patch);
            if (!BROKEN.contains(candidate.toString()) && hasDockerImage(candidate)) {
                return Optional.of(candidate);
            }
        }
        System.err.println("No usable docker image found for the " + latest.getMajor() + "." + latest.getMinor() + " line");
        return Optional.empty();
    }

    private static boolean hasDockerImage(Semver version) {
        var request = HttpRequest.newBuilder(URI.create("https://hub.docker.com/v2/repositories/library/mongo/tags/" + version))
                .GET()
                .build();
        int status;
        try {
            status = CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (status != 200 && status != 404) {
            throw new IllegalStateException(format("Unexpected status %d checking docker image for %s", status, version));
        }
        return status == 200;
    }
}
