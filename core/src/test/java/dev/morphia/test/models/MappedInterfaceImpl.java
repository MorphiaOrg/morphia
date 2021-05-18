package dev.morphia.test.models;

import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;

import static com.mongodb.client.model.CollationStrength.SECONDARY;

@Indexes(
    @Index(fields = @Field(value = "name", type = IndexType.DESC),
        options = @IndexOptions(name = "behind_interface",
            collation = @Collation(locale = "en", strength = SECONDARY))))
public class MappedInterfaceImpl implements MappedInterface {
    @Indexed
    private String name;
}
