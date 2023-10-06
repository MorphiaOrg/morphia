package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.TimeUnit.SECOND;
import static dev.morphia.aggregation.expressions.WindowExpressions.derivative;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.filters.Filters.gt;

public class TestDerivative extends AggregationTest {
    @Test
    public void testDerivative() {
        insert("deliveryFleet", parseDocs(
                "{ truckID: '1', timeStamp: ISODate( '2020-05-18T14:10:30Z' ), miles: 1295.1 }",
                "{ truckID: '1', timeStamp: ISODate( '2020-05-18T14:11:00Z' ), miles: 1295.63 }",
                "{ truckID: '1', timeStamp: ISODate( '2020-05-18T14:11:30Z' ), miles: 1296.25 }",
                "{ truckID: '1', timeStamp: ISODate( '2020-05-18T14:12:00Z' ), miles: 1296.76 }",
                "{ truckID: '2', timeStamp: ISODate( '2020-05-18T14:10:30Z' ), miles: 10234.1 }",
                "{ truckID: '2', timeStamp: ISODate( '2020-05-18T14:11:00Z' ), miles: 10234.33 }",
                "{ truckID: '2', timeStamp: ISODate( '2020-05-18T14:11:30Z' ), miles: 10234.73 }",
                "{ truckID: '2', timeStamp: ISODate( '2020-05-18T14:12:00Z' ), miles: 10235.13 }"));

        List<Document> actual = getDs().aggregate("deliveryFleet")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("truckID"))
                        .sortBy(ascending("timeStamp"))
                        .output(output("truckAverageSpeed")
                                .operator(derivative(field("miles"))
                                        .unit(HOUR))
                                .window()
                                .range(-30, 0, SECOND)))
                .match(gt("truckAverageSpeed", 50))
                .project(project()
                        .suppressId())
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{'truckID':'1','timeStamp':ISODate('2020-05-18T14:11:00Z'),'miles':1295.63,'truckAverageSpeed':63.60000000002401}",
                "{'truckID':'1','timeStamp':ISODate('2020-05-18T14:11:30Z'),'miles':1296.25,'truckAverageSpeed':74.3999999999869}",
                "{'truckID':'1','timeStamp':ISODate('2020-05-18T14:12:00Z'),'miles':1296.76,'truckAverageSpeed':61.199999999998916}");
        assertListEquals(actual, expected);
    }

}
