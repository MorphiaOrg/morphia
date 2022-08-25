package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.stages.Densify.Range;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestDensify extends AggregationTest {
    @Test
    public void testDensify() {
        List<Document> list = List.of(
            parse("{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T00:00:00.000Z'), 'temp': " +
                  "12 }"),
            parse("{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T04:00:00.000Z'), 'temp': " +
                  "11 }"),
            parse("{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T08:00:00.000Z'), 'temp': " +
                  "11 }"),
            parse("{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T12:00:00.000Z'), 'temp': " +
                  "12 }"));
        insert("weather", list);

        List<Document> result = getDs().aggregate("weather")
                                       .densify(Densify.densify("timestamp",
                                           Range.bounded(ZonedDateTime.parse("2021-05-18T00:00:00.000Z"),
                                                    ZonedDateTime.parse("2021-05-18T08:00:00.000Z"), 1)
                                                .unit(HOUR)))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ _id: ObjectId('618c207c63056cfad0ca4309'), metadata: { sensorId: 5578, type: 'temperature' }, " +
                  "timestamp: ISODate ('2021-05-18T00:00:00.000Z'), temp: 12 }"),
            parse("{ timestamp: ISODate('2021-05-18T01:00:00.000Z') }"),
            parse("{ timestamp: ISODate('2021-05-18T02:00:00.000Z') }"),
            parse("{ timestamp: ISODate('2021-05-18T03:00:00.000Z') }"),
            parse("{ _id: ObjectId('618c207c63056cfad0ca430a'), metadata: { sensorId: 5578, type: 'temperature' }, " +
                  "timestamp: ISODate('2021-05-18T04:00:00.000Z'), temp: 11 }"),
            parse("{ timestamp: ISODate('2021-05-18T05:00:00.000Z') }"),
            parse("{ timestamp: ISODate('2021-05-18T06:00:00.000Z') }"),
            parse("{ timestamp: ISODate('2021-05-18T07:00:00.000Z') }"),
            parse("{ _id: ObjectId('618c207c63056cfad0ca430b'), metadata: { sensorId: 5578, type: 'temperature' }, " +
                  "timestamp: ISODate ('2021-05-18T08:00:00.000Z'), temp: 11 }"),
            parse("{ _id: ObjectId('618c207c63056cfad0ca430c'), metadata: { sensorId: 5578, type: 'temperature' }," +
                  " timestamp: ISODate ('2021-05-18T12:00:00.000Z'), temp: 12 }"));
        assertEquals(removeIds(result), removeIds(expected));
    }

    private List<Document> removeIds(List<Document> documents) {
        return documents.stream()
                        .peek(d -> d.remove("_id"))
                        .collect(Collectors.toList());
    }

}