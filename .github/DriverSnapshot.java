///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import java.net.URL;
import java.util.stream.Collectors;
import java.util.Spliterators;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import org.semver4j.Semver;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class DriverSnapshot {

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var url = "https://central.sonatype.com/repository/maven-snapshots/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        var mapper = new XmlMapper();
        var min = System.getenv().getOrDefault("DRIVER_MIN", "5.0.0");
        var driverMinimum = Semver.parse(min);

        var document = mapper.readTree(new URL(url));

        var versions = document
                           .get("versioning")
                           .get("versions")
                           .get("version");

        var result = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                .map(JsonNode::asText)
                .map(Semver::parse)
                .filter(it -> it.isGreaterThanOrEqualTo(driverMinimum))
                         .sorted()
                .toList();
        System.out.println(result.get(result.size() - 1));
    }
}
