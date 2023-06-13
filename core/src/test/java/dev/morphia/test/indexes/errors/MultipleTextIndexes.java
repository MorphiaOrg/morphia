package dev.morphia.test.indexes.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import org.bson.types.ObjectId;

import static dev.morphia.utils.IndexType.TEXT;

@Entity
@Indexes({ @Index(fields = @Field(value = "name", type = TEXT)),
        @Index(fields = @Field(value = "nickName", type = TEXT)) })
public class MultipleTextIndexes {
    @Id
    private ObjectId id;
    private String name;
    private String nickName;
}
