///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS com.github.zafarkhaja:java-semver:0.9.0
//DEPS org.jsoup:jsoup:1.15.3

import java.net.URL;
import com.github.zafarkhaja.semver.Version;
import org.jsoup.Jsoup;
import java.util.stream.Collectors;


public class BuildMatrix {

    public static void main(String... args) throws Exception {
        var doc = Jsoup.parse(new URL("https://www.mongodb.com/try/download/community"), 10000);
        var found = doc.getElementById("download-version")
                .siblingElements()
                .stream()

                .flatMap(it -> it.getElementsByTag("div").stream())
                .flatMap(it -> it.getElementsByTag("ul").stream())
                .flatMap(it -> it.getElementsByTag("li").stream())
                .map(it -> substringBeforeSpace(it.text()))
                .map(Version::valueOf)
                .filter(it -> it.getPreReleaseVersion() == null || it.getPreReleaseVersion().equals(""))
                .filter(it -> it.greaterThan(Version.valueOf("4.0.0")))
                .map(it -> "'%s'".formatted(it))
                .collect(Collectors.toList());

        System.out.println(found);
    }

    private static String substringBeforeSpace(String text) {
        int endIndex = text.indexOf(' ');
        if (endIndex == -1) {
            endIndex = text.length();
        }
        return text.substring(0, endIndex);
    }
}
