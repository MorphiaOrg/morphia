///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS com.github.zafarkhaja:java-semver:0.9.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import java.net.URL;
import java.util.stream.Collectors;
import java.util.Spliterators;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.zafarkhaja.semver.Version;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class DriverSnapshot {

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var url = "https://oss.sonatype.org/content/repositories/snapshots/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        var mapper = new XmlMapper();
        var min = System.getenv().getOrDefault("DRIVER_MIN", "5.0.0");
        Version driverMinimum = Version.valueOf(min);

        var document = mapper.readTree(new URL(url));

        var versions = document
                           .get("versioning")
                           .get("versions")
                           .get("version");

        var result = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                .map(JsonNode::asText)
                .map(Version::valueOf)
                .filter(it -> it.greaterThanOrEqualTo(driverMinimum))
                         .sorted()
                .toList();
        System.out.println(result.get(result.size() - 1));
    }
}
