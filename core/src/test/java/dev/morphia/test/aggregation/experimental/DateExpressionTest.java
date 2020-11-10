package dev.morphia.test.aggregation.experimental;

import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.test.aggregation.experimental.expressions.ExpressionsTestBase;
import dev.morphia.test.aggregation.experimental.model.Sales;
import dev.morphia.test.aggregation.experimental.model.StringDates;
import dev.morphia.test.models.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateFromParts;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateFromString;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateToParts;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dayOfMonth;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dayOfWeek;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.hour;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.isoDayOfWeek;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.isoWeek;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.isoWeekYear;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.milliseconds;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.minute;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.month;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.second;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.toDate;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.week;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.year;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.stages.Projection.of;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class DateExpressionTest extends ExpressionsTestBase {
    @Test
    public void testDateAggregation() {
        insert("sales", List.of(
            parse("{\"_id\" : 1,\"item\" : \"abc\",\"price\" : 10,\"quantity\" : 2,\"date\" : ISODate(\"2014-01-01T08:15:39.736Z\")\n}")));
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
        assertEquals(dates.getInteger("_id").intValue(), 1);
        assertEquals(dates.getInteger("year").intValue(), 2014);
        assertEquals(dates.getInteger("month").intValue(), 1);
        assertEquals(dates.getInteger("day").intValue(), 1);
        assertEquals(dates.getInteger("hour").intValue(), 8);
        assertEquals(dates.getInteger("minutes").intValue(), 15);
        assertEquals(dates.getInteger("seconds").intValue(), 39);
        assertEquals(dates.getInteger("milliseconds").intValue(), 736);
        assertEquals(dates.getInteger("dayOfYear").intValue(), 1);
        assertEquals(dates.getInteger("dayOfWeek").intValue(), 4);
        assertEquals(dates.getInteger("week").intValue(), 0);
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
        assertEquals(result, parse("{'date': ISODate('2017-02-08T12:00:00Z'), 'date_iso': ISODate('2017-02-08T12:00:00Z'),"
                                   + "'date_timezone': ISODate('2017-01-01T04:46:12Z')}"));
    }

    @Test
    public void testDateFromString() {
        List<Document> list = List.of(
            parse("{ _id: 1, date: '2017-02-08T12:10:40.787', timezone: 'America/New_York', message:  'Step 1: Started' }"),
            parse("{ _id: 2, date: '2017-02-08', timezone: '-05:00', message:  'Step 1: Ended' }"),
            parse("{ _id: 3, message:  ' Step 1: Ended ' }"),
            parse("{ _id: 4, date: '2017-02-09', timezone: 'Europe/London', message: 'Step 2: Started'}"),
            parse("{ _id: 5, date: '2017-02-09T03:35:02.055', timezone: '+0530', message: 'Step 2: In Progress'}"));

        insert("logmessages", list);

        List<Document> result = getDs().aggregate(LogMessage.class)
                                       .project(of().include("date", dateFromString()
                                                                         .dateString(field("date"))
                                                                         .timeZone("America/New_York")))
                                       .execute(Document.class)
                                       .toList();
        assertEquals(result, List.of(
            parse("{ '_id' : 1, 'date' : ISODate('2017-02-08T17:10:40.787Z') }"),
            parse("{ '_id' : 2, 'date' : ISODate('2017-02-08T05:00:00Z') }"),
            parse("{ '_id' : 3, 'date' : null }"),
            parse("{ '_id' : 4, 'date' : ISODate('2017-02-09T05:00:00Z') }"),
            parse("{ '_id' : 5, 'date' : ISODate('2017-02-09T08:35:02.055Z') }")));


        result = getDs().aggregate(LogMessage.class)
                        .project(of().include("date", dateFromString()
                                                          .dateString(field("date"))
                                                          .timeZone(field("timezone"))))
                        .execute(Document.class)
                        .toList();

        assertEquals(result, List.of(
            parse("{ '_id' : 1, 'date' : ISODate('2017-02-08T17:10:40.787Z') }"),
            parse("{ '_id' : 2, 'date' : ISODate('2017-02-08T05:00:00Z') }"),
            parse("{ '_id' : 3, 'date' : null }"),
            parse("{ '_id' : 4, 'date' : ISODate('2017-02-09T00:00:00Z') }"),
            parse("{ '_id' : 5, 'date' : ISODate('2017-02-08T22:05:02.055Z') }")));
    }

    @Test
    public void testDateToParts() {
        getMapper().getCollection(User.class).drop();
        getDocumentCollection(User.class)
            .insertOne(parse("{'_id': 2, 'item': 'abc', 'price': 10, 'quantity': 2, 'date': ISODate('2017-01-01T01:29:09.123Z')}"));

        Document parts = getDs().aggregate(User.class)
                                .project(of()
                                             .include("date", dateToParts(field("date")))
                                             .include("date_iso", dateToParts(field("date"))
                                                                      .iso8601(true))
                                             .include("date_timezone", dateToParts(field("date"))
                                                                           .timezone(value("America/New_York"))))
                                .execute(Document.class)
                                .next();
        assertDocumentEquals(parts, parse("{\"_id\":2,\"date\":{\"year\":2017,\"month\":1,\"day\":1,\"hour\":1,\"minute\":29,\"second\":9,"
                                          + "\"millisecond\":123},\"date_iso\":{\"isoWeekYear\":2016,\"isoWeek\":52,\"isoDayOfWeek\":7,"
                                          + "\"hour\":1,\"minute\":29,\"second\":9,\"millisecond\":123},\"date_timezone\":{\"year\":2016,"
                                          + "\"month\":12,\"day\":31,\"hour\":20,\"minute\":29,\"second\":9,\"millisecond\":123}}"));
    }

    @Test
    public void testDateToString() {
        LocalDate joined = LocalDate.parse("2016-05-01 UTC", DateTimeFormatter.ofPattern("yyyy-MM-dd z"));
        getMapper().getCollection(User.class).drop();
        getDs().save(new User("John Doe", joined));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .project(of().include("string",
                                             dateToString()
                                                 .format("%Y-%m-%d")
                                                 .date(field("joined"))));

        MorphiaCursor<StringDates> it = pipeline.execute(StringDates.class);
        while (it.hasNext()) {
            assertEquals(it.next().getString(), "2016-05-01");
        }
    }

    @Test
    public void testIsoDayOfWeek() throws ParseException {
        assertAndCheckDocShape("{}", isoDayOfWeek(value(new SimpleDateFormat("MMM dd, yyyy").parse("August 14, 2011")))
                                         .timezone(value("America/Chicago")), 6);
        assertAndCheckDocShape("{}", isoDayOfWeek(value(new SimpleDateFormat("yyyy-MM-dd").parse("2016-01-01"))), 5);
    }

    @Test
    public void testIsoWeek() throws ParseException {
        assertAndCheckDocShape("{}", isoWeek(value(new SimpleDateFormat("MMM dd, yyyy").parse("August 14, 2011")))
                           .timezone(value("America/Chicago")), 32);
        assertAndCheckDocShape("{}", isoWeek(value(new SimpleDateFormat("MMM dd, yyyy").parse("Jan 4, 2016"))), 1);
    }

    @Test
    public void testIsoWeekYear() throws ParseException {
        assertAndCheckDocShape("{}", isoWeekYear(value(new SimpleDateFormat("MMM dd, yyyy").parse("April 08, 2024")))
                           .timezone(value("America/Chicago")), 2024L);
        assertAndCheckDocShape("{}", isoWeekYear(value(new SimpleDateFormat("yyyy-MM-dd").parse("2015-05-26"))),
            2015L);
    }

    @Test
    public void testToDate() {
        checkMinServerVersion(4.0);
        insert("orders", List.of(
            parse(" { _id: 1, item: 'apple', qty: 5, order_date: '2018-03-10'}"),
            parse("{ _id: 2, item: 'pie', qty: 10,  order_date: '2018-03-12'}"),
            parse("{ _id: 3, item: 'ice cream', qty: 2, price: '4.99', order_date: '2018-03-05' }"),
            parse("{ _id: 4, item: 'almonds' ,  qty: 5, price: 5,  order_date: '2018-03-05 +10:00'}")));

        List<Document> result = getDs().aggregate(Order.class)
                                       .addFields(AddFields.of()
                                                           .field("convertedDate", toDate(field("order_date"))))
                                       .sort(Sort.on()
                                                 .ascending("convertedDate"))
                                       .execute(Document.class)
                                       .toList();

        List<Document> documents = List.of(
            parse(
                "{'_id': 4, 'item': 'almonds', 'qty': 5, 'price': 5, 'order_date': '2018-03-05 +10:00', 'convertedDate': ISODate" +
                "('2018-03-04T14:00:00Z')}"),
            parse(
                "{'_id': 3, 'item': 'ice cream', 'qty': 2, 'price': '4.99', 'order_date': '2018-03-05', 'convertedDate': ISODate" +
                "('2018-03-05T00:00:00Z')}"),
            parse("{'_id': 1, 'item': 'apple', 'qty': 5, 'order_date': '2018-03-10', 'convertedDate': ISODate('2018-03-10T00:00:00Z')}"),
            parse("{'_id': 2, 'item': 'pie', 'qty': 10, 'order_date': '2018-03-12', 'convertedDate': ISODate('2018-03-12T00:00:00Z')}"));

        assertEquals(result, documents);
    }

    @Entity("logmessages")
    private static class LogMessage {
        @Id
        private ObjectId id;
    }

    @Entity("orders")
    private static class Order {
        @Id
        private ObjectId id;
    }
}
