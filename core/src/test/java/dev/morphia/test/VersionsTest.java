package dev.morphia.test;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

import com.github.zafarkhaja.semver.Version;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class VersionsTest {
    public void latest() throws IOException {
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
                .collect(Collectors.toList());

        assertFalse(found.isEmpty(), "Should find versions");

        found.removeIf(version -> Versions.find(version) != null);
        assertTrue(found.isEmpty(), "Some versions missing from the build config: " + found);
    }

    @NotNull
    private static String substringBeforeSpace(String text) {
        int endIndex = text.indexOf(' ');
        if (endIndex == -1) {
            endIndex = text.length();
        }
        return text.substring(0, endIndex);
    }
}