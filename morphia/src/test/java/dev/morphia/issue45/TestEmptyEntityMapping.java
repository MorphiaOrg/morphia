package dev.morphia.issue45;


import dev.morphia.DeleteOptions;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.testutil.TestEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.size;


public class TestEmptyEntityMapping extends TestBase {

    @Test
    public void testSizeOnEmptyElements() {
        User u = new User();
        u.fullName = "User Name";
        u.userId = "USERID";
        getDs().save(u);

        Assert.assertNull("Should not find the user.", getDs().find(User.class)
                                                              .filter(size("rights", 0)).iterator()
                                                              .tryNext());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(exists("rights").not()).iterator()
                                                             .next());
        getDs().find(User.class)
               .remove(new DeleteOptions()
                           .multi(true));

        u = new User();
        u.setFullName("User Name");
        u.setUserId("USERID");
        u.rights.add(Rights.ADMIN);
        getDs().save(u);

        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(size("rights", 1)).iterator()
                                                             .next());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(exists("rights")).iterator()
                                                             .next());
    }

    public enum Rights {
        ADMIN
    }

    @Entity
    public static class User extends TestEntity {

        private String userId;
        private String fullName;
        private UserType userType;
        private Set<Rights> rights = new HashSet<>();

        public void setFullName(final String fullName) {
            this.fullName = fullName;
        }

        public void setUserId(final String userId) {
            this.userId = userId;
        }
    }

    @Entity
    public static class UserType extends TestEntity {
    }
}
