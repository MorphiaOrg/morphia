///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.github.zafarkhaja:java-semver:0.9.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.zafarkhaja.semver.Version;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.Spliterators.spliteratorUnknownSize;

public class DriverVersions {

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var url = "https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/maven-metadata.xml";
        var mapper = new XmlMapper();

        var document = mapper.readTree(new URL(url));

        var list = (ObjectNode) document.get("versioning");
        var latest = Version.valueOf(list.get("latest").asText());
        var versions = list.get("versions").get("version");

        var result = StreamSupport.stream(
                                      spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                                  .map(JsonNode::asText)
                                  .map(Version::valueOf)
                                  .filter(it -> it.lessThan(Version.valueOf("5.0.0")))
                                  .filter(it -> it.greaterThan(Version.valueOf("4.0.0")))
                                  .filter(it -> it.getBuildMetadata().isEmpty())
                                  .filter(it -> it.getPreReleaseVersion().isEmpty())
                                  .collect(Collectors.groupingBy(
                                      version -> format("%s.%s", version.getMajorVersion(), version.getMinorVersion())))
                                  .values().stream()
                                  .map(it -> it.get(0))
                                  .sorted(Comparator.comparingInt(Version::getMajorVersion)
                                                    .thenComparingInt(Version::getMinorVersion)
                                                    .thenComparingInt(Version::getPatchVersion))
                                  .map(Version::toString)
                                  .collect(Collectors.toList());

        Collections.reverse(result);
        var map = Map.of("latest", latest, "versions", result);
        if (args.length != 0 && args[0].equals("all")) {
            System.out.println(result);
        } else {
            System.out.println(latest);
        }
    }
}
