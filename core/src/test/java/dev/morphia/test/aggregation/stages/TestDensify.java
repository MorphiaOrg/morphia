package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Densify;
import dev.morphia.aggregation.stages.Densify.Range;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static org.testng.Assert.assertEquals;

public class TestDensify extends AggregationTest {
    @Test
    public void testDensify() {
        checkMinServerVersion(5.1);
        insert("weather", parseDocs(
                "{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T00:00:00.000Z'), 'temp': 12 }",
                "{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T04:00:00.000Z'), 'temp': 11 }",
                "{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T08:00:00.000Z'), 'temp': 11 }",
                "{ 'metadata': { 'sensorId': 5578, 'type': 'temperature' }, 'timestamp': ISODate('2021-05-18T12:00:00.000Z'), 'temp': 12 }"));

        List<Document> result = getDs().aggregate("weather")
                .densify(Densify.densify("timestamp",
                        Range.bounded(ZonedDateTime.parse("2021-05-18T00:00:00.000Z"),
                                ZonedDateTime.parse("2021-05-18T08:00:00.000Z"), 1)
                                .unit(HOUR)))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ metadata: { sensorId: 5578, type: 'temperature' }, timestamp: ISODate ('2021-05-18T00:00:00.000Z'), temp: 12 }",
                "{ timestamp: ISODate('2021-05-18T01:00:00.000Z') }",
                "{ timestamp: ISODate('2021-05-18T02:00:00.000Z') }",
                "{ timestamp: ISODate('2021-05-18T03:00:00.000Z') }",
                "{ metadata: { sensorId: 5578, type: 'temperature' }, timestamp: ISODate('2021-05-18T04:00:00.000Z'), temp: 11 }",
                "{ timestamp: ISODate('2021-05-18T05:00:00.000Z') }",
                "{ timestamp: ISODate('2021-05-18T06:00:00.000Z') }",
                "{ timestamp: ISODate('2021-05-18T07:00:00.000Z') }",
                "{ metadata: { sensorId: 5578, type: 'temperature' }, timestamp: ISODate ('2021-05-18T08:00:00.000Z'), temp: 11 }",
                "{ metadata: { sensorId: 5578, type: 'temperature' }, timestamp: ISODate ('2021-05-18T12:00:00.000Z'), temp: 12 }");
        assertEquals(removeIds(result), removeIds(expected));
    }

}