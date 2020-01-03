package dev.morphia.aggregation.experimental;

import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.model.Sales;
import dev.morphia.aggregation.experimental.model.StringDates;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.DateExpression.dateFromParts;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.dateFromString;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.dateToString;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.dayOfMonth;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.dayOfWeek;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.dayOfYear;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.hour;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.milliseconds;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.minute;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.month;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.second;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.week;
import static dev.morphia.aggregation.experimental.expressions.DateExpression.year;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.stages.Projection.of;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;

public class DateExpressionTest extends TestBase {
    @Test
    public void testDateAggregation() {
        getDatabase().getCollection("sales").insertOne(
            parse("{\"_id\" : 1,\"item\" : \"abc\",\"price\" : 10,\"quantity\" : 2,\"date\" : ISODate(\"2014-01-01T08:15:39.736Z\")"
                  + "\n}"));
        Aggregation<Sales> pipeline = getDs()
                                          .aggregate(Sales.class)
                                          .project(of()
                                                       .include("year", year(field("date")))
                                                       .include("month", month(field("date")))
                                                       .include("day", dayOfMonth(field("date")))
                                                       .include("hour", hour(field("date")))
                                                       .include("minutes", minute(field("date")))
                                                       .include("seconds", second(field("date")))
                                                       .include("milliseconds", milliseconds(field("date")))
                                                       .include("dayOfYear", dayOfYear(field("date")))
                                                       .include("dayOfWeek", dayOfWeek(field("date")))
                                                       .include("week", week(field("date"))));
        Document dates = pipeline.execute(Document.class).tryNext();
        assertEquals(1, dates.getInteger("_id").intValue());
        assertEquals(2014, dates.getInteger("year").intValue());
        assertEquals(1, dates.getInteger("month").intValue());
        assertEquals(1, dates.getInteger("day").intValue());
        assertEquals(8, dates.getInteger("hour").intValue());
        assertEquals(15, dates.getInteger("minutes").intValue());
        assertEquals(39, dates.getInteger("seconds").intValue());
        assertEquals(736, dates.getInteger("milliseconds").intValue());
        assertEquals(1, dates.getInteger("dayOfYear").intValue());
        assertEquals(4, dates.getInteger("dayOfWeek").intValue());
        assertEquals(0, dates.getInteger("week").intValue());
    }

    @Test
    public void testDateFromParts() {
        getDs().save(new Sales());
        Document result =
            getDs()
                .aggregate(Sales.class)
                .project(of()
                             .include("date", dateFromParts()
                                                  .year(2017)
                                                  .month(2)
                                                  .day(8)
                                                  .hour(12))
                             .include("date_iso", dateFromParts()
                                                      .isoWeekYear(2017)
                                                      .isoWeek(6)
                                                      .isoDayOfWeek(3)
                                                      .hour(12))
                             .include("date_timezone", dateFromParts()
                                                           .year(2016)
                                                           .month(12)
                                                           .day(31)
                                                           .hour(23)
                                                           .minute(46)
                                                           .second(12)
                                                           .timezone("America/New_York")))
                .execute(Document.class)
                .next();
        result.remove("_id");
        assertEquals(parse("{'date': ISODate('2017-02-08T12:00:00Z'), 'date_iso': ISODate('2017-02-08T12:00:00Z'),"
                           + "'date_timezone': ISODate('2017-01-01T04:46:12Z')}"), result);
    }

    @Test
    public void testDateToString() throws ParseException {
        Date joined = new SimpleDateFormat("yyyy-MM-dd z").parse("2016-05-01 UTC");
        getDs().save(new User("John Doe", joined));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .project(of()
                                                      .include("string",
                                                          dateToString("%Y-%m-%d", field("joined"))));

        MorphiaCursor<StringDates> it = pipeline.execute(StringDates.class);
        while (it.hasNext()) {
            assertEquals("2016-05-01", it.next().getString());
        }
    }

    @Test
    public void testDateFromString() {
        List<Document> list = List.of(
            parse("{ _id: 1, date: '2017-02-08T12:10:40.787', timezone: 'America/New_York', message:  'Step 1: Started' }"),
            parse("{ _id: 2, date: '2017-02-08', timezone: '-05:00', message:  'Step 1: Ended' }"),
            parse("{ _id: 3, message:  ' Step 1: Ended ' }"),
            parse("{ _id: 4, date: '2017-02-09', timezone: 'Europe/London', message: 'Step 2: Started'}"),
            parse("{ _id: 5, date: '2017-02-09T03:35:02.055', timezone: '+0530', message: 'Step 2: In Progress'}"));

        getDatabase().getCollection("logmessages", Document.class)
                     .insertMany(list);

        List<Document> result = getDs().aggregate(LogMessage.class)
                                       .project(of().include("date", dateFromString()
                                                                         .dateString(field("date"))
                                                                         .timeZone("America/New_York")))
                                       .execute(Document.class)
                                       .toList();
        assertEquals(List.of(
            parse("{ '_id' : 1, 'date' : ISODate('2017-02-08T17:10:40.787Z') }"),
            parse("{ '_id' : 2, 'date' : ISODate('2017-02-08T05:00:00Z') }"),
            parse("{ '_id' : 3, 'date' : null }"),
            parse("{ '_id' : 4, 'date' : ISODate('2017-02-09T05:00:00Z') }"),
            parse("{ '_id' : 5, 'date' : ISODate('2017-02-09T08:35:02.055Z') }")), result);


        result = getDs().aggregate(LogMessage.class)
                        .project(of().include("date", dateFromString()
                                                          .dateString(field("date"))
                                                          .timeZone(field("timezone"))))
                        .execute(Document.class)
                        .toList();

        assertEquals(List.of(
            parse("{ '_id' : 1, 'date' : ISODate('2017-02-08T17:10:40.787Z') }"),
            parse("{ '_id' : 2, 'date' : ISODate('2017-02-08T05:00:00Z') }"),
            parse(" { '_id' : 3, 'date' : null }"),
            parse("{ '_id' : 4, 'date' : ISODate('2017-02-09T00:00:00Z') }"),
            parse(" { '_id' : 5, 'date' : ISODate('2017-02-08T22:05:02.055Z') }")), result);
    }

    @Entity("logmessages")
    private static class LogMessage {
        @Id
        private ObjectId id;
    }
}
