package dev.morphia.test.models;

import dev.morphia.Key;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.List;

@Entity
@SuppressWarnings({"UnusedDeclaration", "removal"})
public class Keys {
    @Id
    private ObjectId id;
    private List<Key<FacebookUser>> users;
    private Key<Rectangle> rect;

    private Keys() {
    }

    public Keys(Key<Rectangle> rectKey, List<Key<FacebookUser>> users) {
        rect = rectKey;
        this.users = users;
    }

    public ObjectId getId() {
        return id;
    }

    public Key<Rectangle> getRect() {
        return rect;
    }

    public List<Key<FacebookUser>> getUsers() {
        return users;
    }
}
