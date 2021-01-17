package dev.morphia.test.models.methods;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.experimental.MorphiaReference;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Entity("users")
@Validation("{ age : { $gte : 13 } }")
public class MethodMappedUser {
    private String name;
    private List<String> likes;
    private int age;
    private Long version;
    private ObjectId id;
    private LocalDate joined;
    private MethodMappedFriend friend;
    private MorphiaReference<List<MethodMappedFriend>> friends;

    public MethodMappedUser() {
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Reference(value = "friend_reference", idOnly = true)
    public MethodMappedFriend getFriend() {
        return friend;
    }

    public void setFriend(MethodMappedFriend friend) {
        this.friend = friend;
    }

    public MorphiaReference<List<MethodMappedFriend>> getFriends() {
        return friends;
    }

    @Property("morphia_reference")
    public void setFriends(MorphiaReference<List<MethodMappedFriend>> friends) {
        this.friends = friends;
    }

    @Id
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Property("dateJoined")
    public LocalDate getJoined() {
        return joined;
    }

    public void setJoined(LocalDate joined) {
        this.joined = joined;
    }

    @Indexed
    public List<String> getLikes() {
        return likes;
    }

    public void setLikes(List<String> likes) {
        this.likes = likes;
    }

    public String getName() {
        return name;
    }

    @Text
    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }

    @Version("version_property")
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, likes, age, version, id, joined, friend, friends);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MethodMappedUser)) {
            return false;
        }
        MethodMappedUser that = (MethodMappedUser) o;
        return age == that.age && Objects.equals(name, that.name) && Objects.equals(likes, that.likes) &&
               Objects.equals(version, that.version) && Objects.equals(id, that.id) &&
               Objects.equals(joined, that.joined) && Objects.equals(friend, that.friend) &&
               Objects.equals(friends, that.friends);
    }

    @Override
    public String toString() {
        return format("User{id=%s, name='%s', joined=%s, likes=%s}", id, name, joined, likes);
    }
}
