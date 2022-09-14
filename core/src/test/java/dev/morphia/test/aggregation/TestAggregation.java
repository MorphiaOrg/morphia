package dev.morphia.test.aggregation;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.test.TestBase;
import dev.morphia.test.aggregation.model.Author;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.CountResult;
import dev.morphia.test.aggregation.model.Human;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.aggregation.model.StringDates;
import dev.morphia.test.models.User;
import dev.morphia.test.models.geo.GeoCity;
import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.DateExpressions.month;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.gte;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static java.util.stream.Collectors.groupingBy;
import static org.testng.Assert.assertEquals;

@SuppressWarnings({ "unused", "RedundantSuppression" })
public class TestAggregation extends TestBase {
    @Test
    public void testBasicGrouping() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2),
                new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));

        MorphiaCursor<Author> aggregate = getDs().aggregate(Book.class)
                .group(group(id("author"))
                        .field("books", push(field("title"))))
                .sort(sort().ascending("name"))
                .execute(Author.class);

        Map<Object, List<Author>> authors = aggregate.toList()
                .stream()
                .collect(groupingBy(a -> a.getName()));
        Assert.assertEquals(authors.size(), 2, "Expecting two results");
        Assert.assertEquals(authors.get("Dante").get(0).getBooks(), of("The Banquet", "Divine Comedy", "Eclogues"), authors.toString());
        Assert.assertEquals(authors.get("Homer").get(0).getBooks(), of("The Odyssey", "Iliad"), authors.toString());
    }

    @Test
    public void testDateAggregation() {
        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .group(group(
                        id()
                                .field("month", month(field("date")))
                                .field("year", year(field("date"))))
                        .field("count", sum(value(1))));

        MorphiaCursor<User> cursor = pipeline.execute(User.class);
        while (cursor.hasNext()) {
            cursor.next();
        }
    }

    @Test
    public void testDateToString() {
        LocalDate joined = LocalDate.parse("2016-05-01 UTC", DateTimeFormatter.ofPattern("yyyy-MM-dd z"));
        getDs().save(new User("John Doe", joined));
        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .project(project()
                        .include("string", dateToString()
                                .format("%Y-%m-%d")
                                .date(field("joined"))));

        for (Iterator<StringDates> it = pipeline.execute(StringDates.class); it.hasNext();) {
            Assert.assertEquals(it.next().getString(), "2016-05-01");
        }
    }

    @Test
    public void testGenericAccumulatorUsage() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2),
                new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));

        Iterator<CountResult> aggregation = getDs().aggregate(Book.class)
                .group(group(id("author"))
                        .field("count", sum(value(1))))
                .sort(sort()
                        .ascending("_id"))
                .execute(CountResult.class);

        CountResult result1 = aggregation.next();
        CountResult result2 = aggregation.next();
        Assert.assertFalse(aggregation.hasNext(), "Expecting two results");
        Assert.assertEquals(result1.getAuthor(), "Dante");
        Assert.assertEquals(result1.getCount(), 3);
        Assert.assertEquals(result2.getAuthor(), "Homer");
        Assert.assertEquals(result2.getCount(), 2);
    }

    @Test
    public void testNullGroupId() {
        getDs().save(asList(new User("John", LocalDate.now()),
                new User("Paul", LocalDate.now()),
                new User("George", LocalDate.now()),
                new User("Ringo", LocalDate.now())));
        Aggregation<User> pipeline = getDs()
                .aggregate(User.class)
                .group(Group.group()
                        .field("count", sum(value(1))));

        assertEquals(pipeline.execute(Document.class).next().getInteger("count"), valueOf(4));
    }

    @Test
    public void testResultTypes() {
        getMapper().map(Martian.class);

        Martian martian = new Martian();
        martian.name = "Marvin";
        getDs().save(martian);

        List<Human> execute = getDs().aggregate(Martian.class)
                .limit(1)
                .execute(Human.class)
                .toList();
        Human human = execute.get(0);
        assertEquals(human.id, martian.id);
        assertEquals(human.name, martian.name);
    }

    @Test
    public void testUserPreferencesPipeline() {
        final MorphiaCursor<GeoCity> pipeline = getDs().aggregate(GeoCity.class) /* the class is irrelevant for this test */
                .group(group(
                        id("state"))
                        .field("total_pop", sum(field("pop"))))
                .match(gte("total_pop", 10000000))
                .execute(GeoCity.class);
        while (pipeline.hasNext()) {
            pipeline.next();
        }
    }
}
