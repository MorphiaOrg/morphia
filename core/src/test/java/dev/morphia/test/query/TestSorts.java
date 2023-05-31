package dev.morphia.test.query;

import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Text;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.aggregation.expressions.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.Meta.textScore;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.filters.Filters.text;
import static java.lang.String.format;

public class TestSorts extends TemplatedTestBase {

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

        testQuery(query, options, true);
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
