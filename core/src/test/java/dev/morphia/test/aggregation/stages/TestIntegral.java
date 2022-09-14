package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.WindowExpressions.integral;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.query.Sort.ascending;

public class TestIntegral extends AggregationTest {
    @Test
    public void testIntegral() {
        checkMinServerVersion(5.0);

        insert("powerConsumption", parseDocs(
                "{ powerMeterID: '1', timeStamp: ISODate( '2020-05-18T14:10:30Z' ), kilowatts: 2.95 }",
                "{ powerMeterID: '1', timeStamp: ISODate( '2020-05-18T14:11:00Z' ), kilowatts: 2.7 }",
                "{ powerMeterID: '1', timeStamp: ISODate( '2020-05-18T14:11:30Z' ), kilowatts: 2.6 }",
                "{ powerMeterID: '1', timeStamp: ISODate( '2020-05-18T14:12:00Z' ), kilowatts: 2.98 }",
                "{ powerMeterID: '2', timeStamp: ISODate( '2020-05-18T14:10:30Z' ), kilowatts: 2.5 }",
                "{ powerMeterID: '2', timeStamp: ISODate( '2020-05-18T14:11:00Z' ), kilowatts: 2.25 }",
                "{ powerMeterID: '2', timeStamp: ISODate( '2020-05-18T14:11:30Z' ), kilowatts: 2.75 }",
                "{ powerMeterID: '2', timeStamp: ISODate( '2020-05-18T14:12:00Z' ), kilowatts: 2.82 }"));

        List<Document> actual = getDs().aggregate("powerConsumption")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("powerMeterID"))
                        .sortBy(ascending("timeStamp"))
                        .output(output("powerMeterKilowattHours")
                                .operator(integral(field("kilowatts"))
                                        .unit(HOUR))
                                .window()
                                .range("unbounded", "current", HOUR)))
                .project(project()
                        .suppressId())
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ 'powerMeterID' : '1', 'timeStamp' : ISODate('2020-05-18T14:10:30Z'), 'kilowatts' : 2.95, 'powerMeterKilowattHours' : 0.0 }",
                "{ 'powerMeterID' : '1', 'timeStamp' : ISODate('2020-05-18T14:11:00Z'), 'kilowatts' : 2.7, 'powerMeterKilowattHours' : 0" +
                        ".023541666666666666 }",
                "{ 'powerMeterID' : '1', 'timeStamp' : ISODate('2020-05-18T14:11:30Z'), 'kilowatts' : 2.6, 'powerMeterKilowattHours' : 0" +
                        ".045625 }",
                "{ 'powerMeterID' : '1', 'timeStamp' : ISODate('2020-05-18T14:12:00Z'), 'kilowatts' : 2.98, 'powerMeterKilowattHours' : 0" +
                        ".068875 }",
                "{ 'powerMeterID' : '2', 'timeStamp' : ISODate('2020-05-18T14:10:30Z'), 'kilowatts' : 2.5, 'powerMeterKilowattHours' : 0.0 }",
                "{ 'powerMeterID' : '2', 'timeStamp' : ISODate('2020-05-18T14:11:00Z'), 'kilowatts' : 2.25, 'powerMeterKilowattHours' : 0" +
                        ".019791666666666666 }",
                "{ 'powerMeterID' : '2', 'timeStamp' : ISODate('2020-05-18T14:11:30Z'), 'kilowatts' : 2.75, 'powerMeterKilowattHours' : 0" +
                        ".040625 }",
                "{ 'powerMeterID' : '2', 'timeStamp' : ISODate('2020-05-18T14:12:00Z'), 'kilowatts' : 2.82, 'powerMeterKilowattHours' : 0" +
                        ".06383333333333334 }");
        assertListEquals(actual, expected);
    }

}
