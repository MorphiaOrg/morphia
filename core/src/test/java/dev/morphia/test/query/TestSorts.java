package dev.morphia.test.query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.mongodb.lang.NonNull;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Text;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.aggregation.expressions.TemplatedTestBase;

import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.testng.annotations.Test;

import static dev.morphia.query.Meta.textScore;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.filters.Filters.text;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestSorts extends TemplatedTestBase {
    public <D> void testQuery(double serverVersion, String resourceName, boolean orderMatters, Query<D> query, FindOptions options) {
        checkMinServerVersion(serverVersion);

        loadData(getDs().getCollection(query.getEntityClass()).getNamespace().getCollectionName(), resourceName);

        List<D> actual = runQuery(resourceName, query, options);

        List<D> expected = map(query.getEntityClass(), loadExpected(resourceName));

        if (orderMatters) {
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    private <D> List<D> map(Class<D> entityClass, List<Document> documents) {
        var codec = getDs().getCodecRegistry().get(entityClass);

        DecoderContext context = DecoderContext.builder().build();
        return documents.stream()
                .map(document -> {
                    return codec.decode(new DocumentReader(document), context);
                })
                .collect(toList());
    }

    @Test
    public void metaAndSorts() {
        getMapper().map(Article.class);
        getDs().ensureIndexes();

        Query<Article> query = getDs().find(Article.class)
                .filter(text("coffee"));
        FindOptions options = new FindOptions()
                .logQuery()
                .sort(textScore("textScore"),
                        ascending("subject"));

        testQuery(0, "metaAndSorts", true, query, options);
    }

    @NonNull
    protected <D> List<D> runQuery(@NonNull String queryTemplate, @NonNull Query<D> query,
                                   @NonNull FindOptions options) {
        String queryName = format("%s/%s/query.json", prefix(), queryTemplate);
        try {

            InputStream stream = getClass().getResourceAsStream(queryName);
            assertNotNull(stream, "Could not find query template: " + queryName);
            Document expectedQuery;
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                expectedQuery = Document.parse(new BufferedReader(reader).readLine());
            }

            assertDocumentEquals(query.toDocument(), expectedQuery);

            try (var cursor = query.iterator(options)) {
                return cursor.toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Entity(useDiscriminator = false)
    private static class Article {
        @Id
        int id;
        @Text
        String subject;
        String author;
        int views;
        double textScore;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Article)) {
                return false;
            }
            Article article = (Article) o;
            return id == article.id && views == article.views && Double.compare(article.textScore, textScore) == 0 &&
                    Objects.equals(subject, article.subject) && Objects.equals(author, article.author);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, subject, author, views, textScore);
        }

        @Override
        public String toString() {
            return format("Article{id=%d, subject='%s', author='%s', views=%d, textScore=%s}", id, subject, author, views, textScore);
        }
    }
}
