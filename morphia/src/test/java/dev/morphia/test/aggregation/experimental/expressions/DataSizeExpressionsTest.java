package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.SystemVariables;
import dev.morphia.aggregation.experimental.stages.Projection;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.DataSizeExpressions.binarySize;
import static dev.morphia.aggregation.experimental.expressions.DataSizeExpressions.bsonSize;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static java.util.List.of;
import static org.bson.Document.parse;

public class DataSizeExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testBinarySize() {
        checkMinServerVersion(4.4);
        insert("images", of(
            parse("{ _id: 1, name: 'cat.jpg', binary: new BinData(0, 'OEJTfmD8twzaj/LPKLIVkA==')}"),
            parse("{ _id: 2, name: 'big_ben.jpg', binary: new BinData(0, 'aGVsZmRqYWZqYmxhaGJsYXJnYWZkYXJlcTU1NDE1Z2FmZCBmZGFmZGE=')}"),
            parse("{ _id: 3, name: 'tea_set.jpg', binary: new BinData(0, 'MyIRAFVEd2aImaq7zN3u/w==')}"),
            parse(
                "{ _id: 4, name: 'concert.jpg', binary: new BinData(0, "
                +
                "'TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=')}"),
            parse("{ _id: 5, name: 'empty.jpg', binary: new BinData(0, '') }")));

        List<Document> documents = getDs().aggregate("images")
                                          .project(Projection.of()
                                                             .include("name", field("name"))
                                                             .include("imageSize", binarySize(field("binary"))))
                                          .execute(Document.class)
                                          .toList();
        List<Document> expected = of(parse("{ '_id' : 1, 'name' : 'cat.jpg', 'imageSize' : 16 }"),
            parse("{ '_id' : 2, 'name' : 'big_ben.jpg', 'imageSize' : 41 }"),
            parse("{ '_id' : 3, 'name' : 'tea_set.jpg', 'imageSize' : 16 }"),
            parse("{ '_id' : 4, 'name' : 'concert.jpg', 'imageSize' : 269 }"),
            parse("{ '_id' : 5, 'name' : 'empty.jpg', 'imageSize' : 0 }"));

        assertListEquals(expected, documents);
    }

    @Test
    public void testBsonSize() {
        checkMinServerVersion(4.4);
        insert("employees", of(
            parse("{ '_id': 1, 'name': 'Alice', 'email': 'alice@company.com', 'position': 'Software Developer', 'current_task': "
                  + "{ 'project_id': 1, 'project_name': 'Aggregation Improvements', 'project_duration': 5, 'hours': 20 } }"),
            parse("{ '_id': 2, 'name': 'Bob', 'email': 'bob@company.com', 'position': 'Sales', 'current_task': { 'project_id': 2, "
                  + "'project_name': 'Write Blog Posts', 'project_duration': 2, 'hours': 10, 'notes': 'Progress is slow. "
                  + "Waiting for feedback.' } }"),
            parse("{ '_id': 3, 'name': 'Charlie', 'email': 'charlie@company.com', 'position': 'HR (On Leave)', 'current_task': null }"),
            parse("{ '_id': 4, 'name': 'Dianne', 'email': 'diane@company.com', 'position': 'Web Designer', 'current_task': { "
                  + "'project_id': 3, 'project_name': 'Update Home Page', 'notes': 'Need to scope this project.' } }")));

        List<Document> list = getDs().aggregate("employees")
                                     .project(Projection.of()
                                                        .include("name")
                                                        .include("object_size", bsonSize(SystemVariables.ROOT)))
                                     .execute(Document.class)
                                     .toList();

        List<Document> expected = of(
            parse("{ '_id' : 1, 'name' : 'Alice', 'object_size' : 203 }"),
            parse("{ '_id' : 2, 'name' : 'Bob', 'object_size' : 229 }"),
            parse("{ '_id' : 3, 'name' : 'Charlie', 'object_size' : 105 }"),
            parse("{ '_id' : 4, 'name' : 'Dianne', 'object_size' : 196   }"));

        assertListEquals(expected, list);
    }
}
