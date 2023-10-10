///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.github.zafarkhaja:java-semver:0.9.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import java.net.URL;
import java.util.stream.Collectors;
import java.util.Spliterators;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;
import com.github.zafarkhaja.semver.Version;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.stream.Collectors;

public class DriverSnapshot {

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var url = "https://oss.sonatype.org/content/repositories/snapshots/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        var mapper = new XmlMapper();

        var document = mapper.readTree(new URL(url));

        var list = (ObjectNode)document.get("versioning");
        var latest = Version.valueOf(list.get("latest").asText());
        var versions = list.get("versions").get("version");

        var result = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                .map(v -> v.asText())
                .map(v -> Version.valueOf(v))
                .filter(it -> it.greaterThanOrEqualTo(Version.valueOf("5.0.0-SNAPSHOT")))
                .collect(Collectors.toList());
        System.out.println(result.get(0));
    }
}
