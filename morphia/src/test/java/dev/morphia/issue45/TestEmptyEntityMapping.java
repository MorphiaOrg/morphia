package dev.morphia.issue45;


import dev.morphia.DeleteOptions;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Transient;
import dev.morphia.testutil.TestEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.size;


@SuppressWarnings("unused")
public class TestEmptyEntityMapping extends TestBase {

    @Test
    public void testSizeOnEmptyElements() {
        User u = new User();
        u.setFullName("User Name");
        u.setUserId("USERID");
        getDs().save(u);

        Assert.assertNull("Should not find the user.", getDs().find(User.class)
                                                              .filter(size("rights", 0))
                                                              .execute()
                                                              .tryNext());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(exists("rights").not())
                                                             .execute()
                                                             .next());
        getDs().find(User.class)
               .remove(new DeleteOptions()
                           .multi(true));

        u = new User();
        u.setFullName("User Name");
        u.setUserId("USERID");
        u.getRights().add(Rights.ADMIN);
        getDs().save(u);

        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(size("rights", 1))
                                                             .execute()
                                                             .next());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class)
                                                             .filter(exists("rights"))
                                                             .execute()
                                                             .next());
    }

    public enum Rights {
        ADMIN
    }

    @Entity
    static class A extends TestEntity {
        private B b;
    }

    @Embedded
    static class B {
        @Transient
        private String foo;
    }

    @Entity
    public static class NotificationAddress extends TestEntity {
    }

    @Entity
    public static class User extends TestEntity {

        private String userId;
        private String fullName;
        private UserType userType;
        private Set<Rights> rights = new HashSet<>();
        private Set<NotificationAddress> notificationAddresses = new HashSet<>();

        public String getFullName() {
            return fullName;
        }

        public void setFullName(final String fullName) {
            this.fullName = fullName;
        }

        public Set<NotificationAddress> getNotificationAddresses() {
            return notificationAddresses;
        }

        public void setNotificationAddresses(final Set<NotificationAddress> notificationAddresses) {
            this.notificationAddresses = notificationAddresses;
        }

        public Set<Rights> getRights() {
            return rights;
        }

        public void setRights(final Set<Rights> rights) {
            this.rights = rights;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(final String userId) {
            this.userId = userId;
        }

        public UserType getUserType() {
            return userType;
        }

        public void setUserType(final UserType userType) {
            this.userType = userType;
        }
    }

    @Entity
    public static class UserType extends TestEntity {
    }
}
