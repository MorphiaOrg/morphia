package dev.morphia.test.aggregation.stages;

import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.expMovingAvg;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;

public class TestExpMovingAverage extends AggregationTest {
    @Test
    public void testExpMovingAverage() {
        checkMinServerVersion(5.0);

        insert("stockPrices", parseDocs(
                "{_id: ObjectId('60d11fef833dfeadc8e6286b'), stock: 'ABC', date: ISODate('2020-05-18T20:00:00Z'), price: 13.0 }",
                "{_id: ObjectId('60d11fef833dfeadc8e6286c'), stock: 'ABC', date: ISODate('2020-05-19T20:00:00Z'), price: 15.4 }",
                "{_id: ObjectId('60d11fef833dfeadc8e6286d'), stock: 'ABC', date: ISODate('2020-05-20T20:00:00Z'), price: 12.0 }",
                "{_id: ObjectId('60d11fef833dfeadc8e6286e'), stock: 'ABC', date: ISODate('2020-05-21T20:00:00Z'), price: 11.7 }",
                "{_id: ObjectId('60d11fef833dfeadc8e6286f'), stock: 'DEF', date: ISODate('2020-05-18T20:00:00Z'), price: 82.0 }",
                "{_id: ObjectId('60d11fef833dfeadc8e62870'), stock: 'DEF', date: ISODate('2020-05-19T20:00:00Z'), price: 94.0 }",
                "{_id: ObjectId('60d11fef833dfeadc8e62871'), stock: 'DEF', date: ISODate('2020-05-20T20:00:00Z'), price: 112.0 }",
                "{_id: ObjectId('60d11fef833dfeadc8e62872'), stock: 'DEF', date: ISODate('2020-05-21T20:00:00Z'), price: 97.3 }"));

        List<Document> actual = getDs().aggregate("stockPrices")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("stock"))
                        .sortBy(Sort.ascending("date"))
                        .output(output("expMovingAvgForStock")
                                .operator(expMovingAvg(field("price"), 2))))
                .execute(Document.class)
                .toList();
        List<Document> expected = parseDocs(
                "{ '_id' : ObjectId('60d11fef833dfeadc8e6286b'), 'stock' : 'ABC', 'date' : ISODate('2020-05-18T20:00:00Z'), 'price'" +
                        " : 13.0, 'expMovingAvgForStock' : 13.0 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e6286c'), 'stock' : 'ABC', 'date' : ISODate('2020-05-19T20:00:00Z'), 'price'" +
                        " : 15.4, 'expMovingAvgForStock' : 14.6 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e6286d'), 'stock' : 'ABC', 'date' : ISODate('2020-05-20T20:00:00Z'), 'price'" +
                        " : 12.0, 'expMovingAvgForStock' : 12.866666666666667 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e6286e'), 'stock' : 'ABC', 'date' : ISODate('2020-05-21T20:00:00Z'), 'price'" +
                        " : 11.7, 'expMovingAvgForStock' : 12.088888888888889 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e6286f'), 'stock' : 'DEF', 'date' : ISODate('2020-05-18T20:00:00Z'), 'price'" +
                        " : 82.0, 'expMovingAvgForStock' : 82.0 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e62870'), 'stock' : 'DEF', 'date' : ISODate('2020-05-19T20:00:00Z'), 'price'" +
                        " : 94.0, 'expMovingAvgForStock' : 90.0 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e62871'), 'stock' : 'DEF', 'date' : ISODate('2020-05-20T20:00:00Z'), 'price'" +
                        " : 112.0, 'expMovingAvgForStock' : 104.66666666666667 }",
                "{ '_id' : ObjectId('60d11fef833dfeadc8e62872'), 'stock' : 'DEF', 'date' : ISODate('2020-05-21T20:00:00Z'), 'price'" +
                        " : 97.3, 'expMovingAvgForStock' : 99.75555555555556 }");

        assertListEquals(actual, expected);
    }

}
