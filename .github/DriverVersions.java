///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS org.semver4j:semver4j:5.6.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.semver4j.Semver;

import java.net.URL;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.TreeMap;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class DriverVersions {

    public static void main(String... args) throws Exception {
        // props to Chris Dellaway for the pointer to this
        var url = "https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/maven-metadata.xml";

        var mapper = new XmlMapper();
        var document = mapper.readTree(new URL(url));
        var versioning = (ObjectNode) document.get("versioning");
        var versions = versioning.get("versions").get("version");

        var comparator = comparingInt(Semver::getMajor)
                             .thenComparingInt(Semver::getMinor)
                             .thenComparingInt(Semver::getPatch)
                             .reversed();
        var grouped = stream(spliteratorUnknownSize(versions.elements(), Spliterator.ORDERED), false)
                          .map(JsonNode::asText)
                          .map(Semver::parse)
                          .filter(it -> it.isGreaterThanOrEqualTo("5.0.0"))
                          .filter(it1 -> it1.getBuild().isEmpty())
                          .filter(it1 -> it1.getPreRelease().isEmpty())
                          .collect(groupingBy(v -> Semver.of(v.getMajor(), v.getMinor(), 0), LinkedHashMap::new,
                              toList()));
        var result = grouped.values().stream()
                            .map(it -> {
                                it.sort(comparator);
                                return it;
                            })
                            .map(it -> it.get(0))
                            .map(it -> format("'%s'", it))
                            .collect(toList());
        Collections.reverse(result);

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
