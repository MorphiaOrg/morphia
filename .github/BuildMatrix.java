///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.semver4j.Semver;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class BuildMatrix {

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
                .filter(it -> it.isGreaterThanOrEqualTo("4.0.0"))
                .map(it -> format("'%s'", it))
                .collect(Collectors.toList());

        var latest = result.get(0);
        var map = new TreeMap<>(Map.of("latest", List.of(latest), "versions", result));
        if (args.length != 0) {
            if (args[0].equals("all")) {
                System.out.println(map.get("versions"));
            } else {
                System.out.println(map.get("latest"));
            }
        } else {
            System.out.println(map.get("latest"));
            System.out.println(map.get("versions"));
            System.out.println(map);
        }

    }
}
