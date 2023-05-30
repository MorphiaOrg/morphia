package dev.morphia.test.aggregation.expressions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.morphia.test.TestBase;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static org.testng.Assert.fail;

public class TemplatedTestBase extends TestBase {
    public final String prefix() {
        String root = getClass().getSimpleName().substring(4);
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    protected void loadData(String collection, String resourceName) {
        insert(collection, loadJson(format("%s/%s/data.json", prefix(), resourceName)));
    }

    protected @NotNull List<Document> loadExpected(String resourceName) {
        return loadJson(format("%s/%s/expected.json", prefix(), resourceName));
    }

    protected @NotNull <T> List<T> loadExpected(Class<T> type, String resourceName) {
        return loadJson(type, format("%s/%s/expected.json", prefix(), resourceName));
    }

    @NotNull
    protected List<Document> loadJson(String name) {
        List<Document> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail("missing data file: " + name);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                data.add(Document.parse(reader.readLine()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return data;
    }

    @NotNull
    protected <T> List<T> loadJson(Class<T> type, String name) {
        List<T> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail("missing data file: " + name);
        }
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                data.add(mapper.readValue(reader.readLine(), type));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return data;
    }
}
