package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.ProjectedBook;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.stages.Projection.project;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestProjection extends AggregationTest {
    @Test
    public void testProjection() {

        insert("books", List.of(
            parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5,\n"
                  + "  lastModified: '2016-07-28'}")));
        Aggregation<Book> pipeline = getDs().aggregate(Book.class)
                                            .project(project()
                                                         .include("title")
                                                         .include("author"));
        MorphiaCursor<ProjectedBook> aggregate = pipeline.execute(ProjectedBook.class);
        assertEquals(aggregate.next(), new ProjectedBook(1, "abc123", "zzz", "aaa"));

        pipeline = getDs().aggregate(Book.class)
                          .project(project()
                                       .suppressId()
                                       .include("title")
                                       .include("author"));
        aggregate = pipeline.execute(ProjectedBook.class);

        assertEquals(aggregate.next(), new ProjectedBook(null, "abc123", "zzz", "aaa"));

        pipeline = getDs().aggregate(Book.class)
                          .project(project()
                                       .exclude("lastModified"));
        final MorphiaCursor<Document> docAgg = pipeline.execute(Document.class);

        assertEquals(docAgg.next(),
            parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5}"));
    }

}
