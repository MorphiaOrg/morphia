package dev.morphia.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dev.morphia.test.GeneratedCases.Case;
import dev.morphia.test.GeneratedCases.Manifest;
import dev.morphia.test.GeneratedCases.OwnedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline: runs without MongoDB. */
public class GeneratedCasesTest {
    private static final File CORE = TemplatedTestBase.rootToCore("");

    @Test
    public void everyGeneratedCaseAndFileIsListed() {
        GeneratedCases.verifyAll();
    }

    @Test
    public void rejectsAnIncompleteOrStaleManifest() {
        Manifest manifest = GeneratedCases.manifest();
        Case first = manifest.cases().get(0);

        rejects(new Manifest(1, List.of(), manifest.files()), "Test is not a listed case");
        rejects(new Manifest(1, twice(manifest.cases()), manifest.files()), "listed more than once");
        rejects(new Manifest(1, List.of(new Case(first.name(), first.kind(), first.scenario(), first.test() + "X")),
                manifest.files()), "no @Test");
        rejects(new Manifest(1, manifest.cases(), List.of()), "Unlisted generated source");
        rejects(new Manifest(1, manifest.cases(), manifest.files().stream()
                .filter(file -> !file.path().contains("/vectors/")).toList()), "Unlisted generated resource");
        rejects(new Manifest(1, manifest.cases(), manifest.files().stream()
                .filter(file -> !file.path().endsWith(first.scenario())).toList()), "Scenario is not an owned file");
        rejects(new Manifest(1, manifest.cases(), manifest.files().stream()
                .map(file -> new OwnedFile(file.path(), "0".repeat(64), file.owner())).toList()), "Owned file changed");
    }

    private static List<Case> twice(List<Case> cases) {
        List<Case> doubled = new ArrayList<>(cases);
        doubled.addAll(cases);
        return doubled;
    }

    private static void rejects(Manifest manifest, String message) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> GeneratedCases.verify(manifest, CORE));
        assertTrue(failure.getMessage().contains(message), failure.getMessage());
    }
}
