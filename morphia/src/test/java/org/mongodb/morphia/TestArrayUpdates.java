package org.mongodb.morphia;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class TestArrayUpdates extends TestBase {
    @Entity
    public static class BatchData {

        @Id
        private ObjectId id;
        @Embedded
        private List<Files> files = new ArrayList<Files>();

        @Override
        public String toString() {
            return format("BatchData{id=%s, files=%s}", id, files);
        }
    }

    @Embedded
    public static class Files {
        private int position;
        private String fileName = "";
        private String fileHash = "";

        public Files() {}

        public Files(final int pos, final String fileName, final String fileHash) {
            this.position = pos;
            this.fileName = fileName;
            this.fileHash = fileHash;
        }

        @Override
        public String toString() {
            return format("Files{fileHash='%s', fileName='%s', position=%d}", fileHash, fileName, position);
        }
    }

    @Test
    public void testUpdates() {
        BatchData theBatch = new BatchData();
        theBatch.files.add(new Files(0, "fileName1", "fileHash1"));
        theBatch.files.add(new Files(0, "fileName2", "fileHash2"));
        getDs().save(theBatch);
        ObjectId id = theBatch.id;

        theBatch = new BatchData();
        theBatch.files.add(new Files(0, "fileName3", "fileHash3"));
        theBatch.files.add(new Files(0, "fileName4", "fileHash4"));
        getDs().save(theBatch);
        ObjectId id2 = theBatch.id;

        UpdateOperations<BatchData> updateOperations = getDs().createUpdateOperations(BatchData.class)
                                                              .set("files.$.fileHash", "new hash");

        getDs().update(getDs().createQuery(BatchData.class)
                              .filter("_id", id)
                              .filter("files.fileName", "fileName1"),
                       updateOperations);

        BatchData data = getDs().createQuery(BatchData.class)
                                .filter("_id", id)
                                .get();
        
        Assert.assertEquals("new hash", data.files.get(0).fileHash);
        Assert.assertEquals("fileHash2", data.files.get(1).fileHash);
        
        data = getDs().createQuery(BatchData.class)
                                .filter("_id", id2)
                                .get();
        
        Assert.assertEquals("fileHash3", data.files.get(0).fileHash);
        Assert.assertEquals("fileHash4", data.files.get(1).fileHash);
    }
}
