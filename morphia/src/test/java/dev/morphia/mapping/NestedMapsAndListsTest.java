package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.FindOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;


public class NestedMapsAndListsTest extends TestBase {

    @Test
    public void testListOfList() {
        getMorphia().map(ListOfList.class);
        ListOfList list = new ListOfList();
        list.list.add(asList(1, 2, 3));
        list.list.add(asList(123, 456));
        getDs().save(list);

        ListOfList listOfList = getDs().find(ListOfList.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals(list, listOfList);
    }

    @Test
    public void testListOfListOfPerson() {
        getMorphia().map(ListListPerson.class);
        ListListPerson list = new ListListPerson();
        list.list.add(asList(new Person("Peter"), new Person("Paul"), new Person("Mary")));
        list.list.add(asList(new Person("Crosby"), new Person("Stills"), new Person("Nash")));
        getDs().save(list);

        ListListPerson result = getDs().find(ListListPerson.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals(list, result);
    }

    @Test
    public void testListOfMap() {
        getMorphia().map(ListOfMap.class);

        ListOfMap entity = new ListOfMap();
        HashMap<String, String> mapA = new HashMap<String, String>();
        mapA.put("a", "b");
        entity.listOfMap.add(mapA);
        final Map<String, String> mapC = new HashMap<String, String>();
        mapC.put("c", "d");
        entity.listOfMap.add(mapC);

        getDs().save(entity);

        ListOfMap object = getDs().find(ListOfMap.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(object);
        Assert.assertEquals(entity, object);
    }

    @Test
    public void testListOfMapOfEntity() {
        getMorphia().map(ListMapPerson.class);
        ListMapPerson listMap = new ListMapPerson();
        listMap.list.add(map("Rick", new Person("Richard")));
        listMap.list.add(map("Bill", new Person("William")));

        getDs().save(listMap);

        Assert.assertEquals(listMap, getDs().find(ListMapPerson.class).find(new FindOptions().limit(1)).tryNext());
    }

    @Test
    public void testMapOfList() throws Exception {
        HashMapOfList map = new HashMapOfList();
        map.mol.put("entry1", Collections.singletonList("val1"));
        map.mol.put("entry2", Collections.singletonList("val2"));

        getDs().save(map);
        map = getDs().find(HashMapOfList.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(map.mol);
        Assert.assertNotNull(map.mol.get("entry1"));
        Assert.assertNotNull(map.mol.get("entry1").get(0));
        Assert.assertEquals("val1", map.mol.get("entry1").get(0));
        Assert.assertNotNull("val2", map.mol.get("entry2").get(0));
    }

    @Test
    public void testUserData() {
        getMorphia().map(MapOfListString.class);
        MapOfListString ud = new MapOfListString();
        ud.id = "123";
        ArrayList<String> d = new ArrayList<String>();
        d.add("1");
        d.add("2");
        d.add("3");
        ud.data.put("123123", d);
        getDs().save(ud);
    }

    @Test
    public void testMapOfListOfMapMap() throws Exception {
        final HashMapOfMap mapOfMap = new HashMapOfMap();
        final Map<String, String> map = new HashMap<String, String>();
        mapOfMap.mom.put("root", map);
        map.put("deep", "values");
        map.put("peer", "lame");


        HashMapOfListOfMapMap mapMap = new HashMapOfListOfMapMap();
        mapMap.mol.put("r1", Collections.singletonList(mapOfMap));
        mapMap.mol.put("r2", Collections.singletonList(mapOfMap));

        getDs().save(mapMap);
        mapMap = getDs().find(HashMapOfListOfMapMap.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(mapMap.mol);
        Assert.assertNotNull(mapMap.mol.get("r1"));
        Assert.assertNotNull(mapMap.mol.get("r1").get(0));
        Assert.assertNotNull(mapMap.mol.get("r1").get(0).mom);
        Assert.assertEquals("values", mapMap.mol.get("r1").get(0).mom.get("root").get("deep"));
        Assert.assertEquals("lame", mapMap.mol.get("r1").get(0).mom.get("root").get("peer"));
        Assert.assertEquals("values", mapMap.mol.get("r2").get(0).mom.get("root").get("deep"));
        Assert.assertEquals("lame", mapMap.mol.get("r2").get(0).mom.get("root").get("peer"));
    }

    @Test
    public void testMapOfMap() throws Exception {
        HashMapOfMap mapOfMap = new HashMapOfMap();
        final Map<String, String> map = new HashMap<String, String>();
        mapOfMap.mom.put("root", map);
        map.put("deep", "values");
        map.put("peer", "lame");

        getDs().save(mapOfMap);
        mapOfMap = getDs().find(HashMapOfMap.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(mapOfMap.mom);
        Assert.assertNotNull(mapOfMap.mom.get("root"));
        Assert.assertNotNull(mapOfMap.mom.get("root").get("deep"));
        Assert.assertEquals("values", mapOfMap.mom.get("root").get("deep"));
        Assert.assertNotNull("lame", mapOfMap.mom.get("root").get("peer"));
    }

    private Map<String, Person> map(final String nick, final Person person) {
        final HashMap<String, Person> map = new HashMap<String, Person>();
        map.put(nick, person);
        return map;
    }
    @Entity
    private static class ListOfMap {
        @Property
        private final List<Map<String, String>> listOfMap = new ArrayList<Map<String, String>>();

        @Id
        private long id;

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + listOfMap.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return format("ListOfMap{id=%d, listOfMap=%s}", id, listOfMap);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ListOfMap listOfMap1 = (ListOfMap) o;

            if (id != listOfMap1.id) {
                return false;
            }
            if (!listOfMap.equals(listOfMap1.listOfMap)) {
                return false;
            }

            return true;
        }

    }
    @Entity
    private static class ListOfList {
        @Property
        private final List<List<Integer>> list = new ArrayList<List<Integer>>();

        @Id
        private long id;

        @Override
        public String toString() {
            return format("ListOfList{id=%d, list=%s}", id, list);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListOfList)) {
                return false;
            }

            final ListOfList that = (ListOfList) o;

            return id == that.id && list.equals(that.list);

        }
        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + list.hashCode();
            return result;
        }

    }
    @Entity
    private static class ListListPerson {
        @Embedded
        private final List<List<Person>> list = new ArrayList<List<Person>>();

        @Id
        private long id;

        @Override
        public String toString() {
            return format("ListListPerson{id=%d, list=%s}", id, list);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListListPerson)) {
                return false;
            }

            final ListListPerson that = (ListListPerson) o;

            if (id != that.id) {
                return false;
            }
            return list.equals(that.list);

        }
        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + list.hashCode();
            return result;
        }

    }
    @Entity
    private static class ListMapPerson {
        @Id
        private ObjectId id;

        private List<Map<String, Person>> list = new ArrayList<Map<String, Person>>();

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListMapPerson)) {
                return false;
            }

            final ListMapPerson that = (ListMapPerson) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            return list.equals(that.list);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + list.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("ListMapPerson{id=%s, list=%s}", id, list);
        }

    }
    @Embedded
    private static class Person {

        private String name;

        Person() {
        }

        Person(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return format("Person{name='%s'}", name);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Person)) {
                return false;
            }

            final Person person = (Person) o;

            return !(name != null ? !name.equals(person.name) : person.name != null);

        }
        @Override
        public int hashCode() {
            return name.hashCode();
        }

    }
    private static class HashMapOfMap {
        @Embedded
        private final Map<String, Map<String, String>> mom = new HashMap<String, Map<String, String>>();
        @Id
        private ObjectId id;

    }
    private static class HashMapOfList {
        private final Map<String, List<String>> mol = new HashMap<String, List<String>>();
        @Id
        private ObjectId id;

    }
    private static class HashMapOfListOfMapMap {
        @Embedded
        private final Map<String, List<HashMapOfMap>> mol = new HashMap<String, List<HashMapOfMap>>();
        @Id
        private ObjectId id;

    }

    public static class MapOfListString {
        @Id
        private String id;

        private Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();

        MapOfListString() {
        }

    }

}
