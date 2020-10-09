package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.Objects;

@Entity(value = "blogImages", discriminatorKey = "type", discriminator = "JPG")
public abstract class BlogImage {
    @Id
    public ObjectId id;
    public ImageType type;
    public String content;

    public BlogImage() {
    }

    public BlogImage(ImageType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogImage)) {
            return false;
        }
        BlogImage blogImage = (BlogImage) o;
        return Objects.equals(id, blogImage.id) &&
               type == blogImage.type &&
               Objects.equals(content, blogImage.content);
    }
}

