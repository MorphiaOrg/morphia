package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.stages.Projection;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.DataSizeExpressions.binarySize;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static java.util.List.of;
import static org.bson.Document.parse;

public class DataSizeExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testBinarySize() {
        getDatabase().getCollection("images").insertMany(of(
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
}
