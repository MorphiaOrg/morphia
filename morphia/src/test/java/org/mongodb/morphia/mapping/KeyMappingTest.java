package org.mongodb.morphia.mapping;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;

public class KeyMappingTest extends TestBase {
    @Test
    public void keyMapping() {
        getMorphia().map(User.class, Channel.class);
        insertData();

        final Datastore datastore = getDs();
        User user = datastore.find(User.class).get();
        List<Key<Channel>> followedChannels = user.followedChannels;

        Channel channel = datastore.find(Channel.class, "name", "Sport channel").get();

        Assert.assertTrue(followedChannels.contains(datastore.getKey(channel)));
    }

    @Test
    public void testKeyComparisons() throws Exception {
        final User user = new User("Luke Skywalker");
        getDs().save(user);
        final Key<User> k1 = new Key<User>(User.class, "User", user.id);
        final Key<User> k2 = getDs().getKey(user);

        Assert.assertTrue(k1.equals(k2));
        Assert.assertTrue(k2.equals(k1));

    }

    private void insertData() {
        final Datastore datastore = getDs();

        Channel sportChannel = new Channel("Sport channel");
        datastore.save(sportChannel);

        datastore.save(new Channel("Art channel"));

        Channel fitnessChannel = new Channel("Fitness channel");
        datastore.save(fitnessChannel);

        final List<Key<Channel>> followedChannels = new ArrayList<Key<Channel>>();
        followedChannels.add(datastore.getKey(sportChannel));
        followedChannels.add(datastore.getKey(fitnessChannel));

        datastore.save(new User("Roberto", datastore.getKey(sportChannel), followedChannels));
    }

    @Entity(noClassnameStored = true)
    static class User {
        @Id
        private ObjectId id;

        private Key<Channel> favoriteChannels;

        private List<Key<Channel>> followedChannels = new ArrayList<Key<Channel>>();

        private String name;

        public User() {
        }

        public User(final String name, final Key<Channel> favoriteChannels,
                    final List<Key<Channel>> followedChannels) {
            this.name = name;
            this.favoriteChannels = favoriteChannels;
            this.followedChannels = followedChannels;
        }

        public User(final String name) {
            this.name = name;
        }
    }

    @Entity(noClassnameStored = true)
    static class Channel {

        @Id
        private ObjectId id;
        private String name;

        public Channel() {

        }

        public Channel(final String name) {
            this.name = name;
        }
    }
}
