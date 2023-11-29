///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.github.zafarkhaja:java-semver:0.9.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.zafarkhaja.semver.Version;

import java.net.URL;
import java.util.Spliterator;
import java.util.Spliterators;
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
                .map(Version::valueOf)
                .filter(it -> it.getPreReleaseVersion() == null || it.getPreReleaseVersion().equals(""))
                .filter(it -> it.greaterThanOrEqualTo(Version.valueOf("4.0.0")))
                .map(it -> format("'%s'", it))
                .collect(Collectors.toList());
        System.out.println(result);
    }
}
