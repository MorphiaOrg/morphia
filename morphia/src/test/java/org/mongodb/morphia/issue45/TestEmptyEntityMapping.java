package org.mongodb.morphia.issue45;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.HashSet;
import java.util.Set;


public class TestEmptyEntityMapping extends TestBase {
    @Test
    public void testEmptyEmbeddedNotNullAfterReload() throws Exception {
        A a = new A();
        a.b = new B();

        getDs().save(a);
        Assert.assertNotNull(a.b);

        a = getDs().find(A.class, "_id", a.getId()).get();
        Assert.assertNull(a.b);
    }

    @Test
    public void testSizeOnEmptyElements() {
        User u = new User();
        u.setFullName("User Name");
        u.setUserId("USERID");
        getDs().save(u);

        Assert.assertNull("Should not find the user.", getDs().find(User.class).filter("rights size", 0).get());
        Assert.assertNull("Should not find the user.", getDs().find(User.class).field("rights").sizeEq(0).get());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class).field("rights").doesNotExist().get());
        getDs().delete(getDs().createQuery(User.class));

        u = new User();
        u.setFullName("User Name");
        u.setUserId("USERID");
        u.getRights().add(Rights.ADMIN);
        getDs().save(u);

        Assert.assertNotNull("Should find the user.", getDs().find(User.class).filter("rights size", 1).get());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class).field("rights").sizeEq(1).get());
        Assert.assertNotNull("Should find the user.", getDs().find(User.class).field("rights").exists().get());
    }


    @Entity
    public enum Rights {
        ADMIN
    }

    @Entity
    static class A extends TestEntity {
        @Embedded
        private B b;
    }

    @Embedded
    static class B {
        @Transient
        private String foo;
    }

    @Entity
    public static class UserType extends TestEntity {
    }

    @Entity
    public static class NotificationAddress extends TestEntity {
    }

    @Entity
    public static class User extends TestEntity {

        private String userId = null;
        private String fullName = null;
        @Embedded
        private UserType userType = null;
        @Embedded
        private Set<Rights> rights = new HashSet<Rights>();
        @Embedded
        private Set<NotificationAddress> notificationAddresses = new HashSet<NotificationAddress>();

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
}
