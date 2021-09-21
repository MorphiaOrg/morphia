package dev.morphia.test.mapping.experimental;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.experimental.filters.Filters.lte;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

abstract class AbstractPerson {
    private Long age;

    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }
}

@Embedded
class Address {
    @Property("c")
    private final String city;
    private final String state;
    private final String zip;


    public Address(String city, String state, String zip) {
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, state, zip);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address)) {
            return false;
        }
        Address address = (Address) o;
        boolean equals = Objects.equals(city, address.city);
        boolean equals1 = Objects.equals(state, address.state);
        boolean equals2 = Objects.equals(zip, address.zip);
        return equals && equals1 &&
               equals2;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Address.class.getSimpleName() + "[", "]")
            .add("city='" + city + "'")
            .add("state='" + state + "'")
            .add("zip='" + zip + "'")
            .toString();
    }
}

public class ConstructorCreatorTest extends TestBase {

    @Test
    public void embeds() {
        Person person = new Person("Mike", "Bloomberg");
        getDs().save(person);
        Invoice invoice = new Invoice(LocalDateTime.now(), person, new Address("New York City", "NY", "10036"));
        getDs().save(invoice);

        person = new Person("Andy", "Warhol");
        getDs().save(person);

        invoice = new Invoice(LocalDateTime.now(), person, new Address("NYC", "NY", "10018"));

        getDs().save(invoice);

        MorphiaCursor<Invoice> criteria1 = getDs().find(Invoice.class)
                                                  .filter(lte("orderDate", LocalDateTime.now().plusDays(5)))
                                                  .iterator(new FindOptions()
                                                      .sort(ascending("addresses")));
        List<Invoice> list = criteria1.toList();
        assertEquals(list.get(0).getAddresses().get(0).getCity(), "NYC", list.stream().map(Invoice::getId).collect(
            Collectors.toList()).toString());
        assertEquals(list.get(0), invoice, list.stream().map(Invoice::getId).collect(Collectors.toList()).toString());

        MorphiaCursor<Invoice> criteria2 = getDs().find(Invoice.class)
                                                  .iterator(new FindOptions()
                                                      .sort(descending("addresses")));
        assertEquals(criteria2.toList().get(0).getAddresses().get(0).getCity(), "New York City");
    }

    @Test
    public void testBestConstructor() {

        Constructor<?> constructor = ConstructorCreator.bestConstructor(getDs().getMapper().map(SomeProps.class).get(0));
        assertNotNull(constructor);
        assertEquals(constructor.getParameterCount(), 2);

        constructor = ConstructorCreator.bestConstructor(getDs().getMapper().map(AllProps.class).get(0));
        assertNotNull(constructor);
        assertEquals(constructor.getParameterCount(), 3);

        constructor = ConstructorCreator.bestConstructor(getDs().getMapper().map(NoProps.class).get(0));
        assertNull(constructor);

        constructor = ConstructorCreator.bestConstructor(getDs().getMapper().map(Default.class).get(0));
        assertNotNull(constructor);
        assertEquals(constructor.getParameterCount(), 0);
    }

    @Entity
    private static class AllProps {
        private final String name;
        private final int count;
        @Id
        private ObjectId id;

        public AllProps(ObjectId id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
    }

    @Entity
    private static class Default {
        @Id
        private ObjectId id;
        private String name;
        private int count;

        public Default() {
        }
    }

    @Entity
    private static class NoProps {
        @Id
        private ObjectId id;
        private String name;
        private int count;

        public NoProps(Boolean whatever, LocalDateTime yikes) {
        }
    }

    @Entity
    private static class SomeProps {
        private final String name;
        private final int count;
        @Id
        private ObjectId id;

        public SomeProps(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }
}

@Entity
class Invoice {
    @Id
    private ObjectId id = new ObjectId();
    private LocalDateTime orderDate;
    @Reference
    private Person person;
    private List<List<List<Address>>> listListList = new ArrayList<>();
    private List<Address> addresses = new ArrayList<>();
    private Map<String, List<Address>> mapList = new LinkedHashMap<>();
    private Double total = 0.0;
    private List<Item> items = new ArrayList<>();
    private transient boolean postLoad;
    private transient boolean preLoad;
    private transient boolean prePersist;
    private transient boolean postPersist;

    public Invoice() {
    }

    public Invoice(LocalDateTime orderDate, Person person, Address addresses, Item... items) {
        this.orderDate = orderDate.withNano(0);
        this.person = person;
        if (addresses != null) {
            this.addresses.add(addresses);
            mapList.put("1", this.addresses);
            listListList = List.of(List.of(this.addresses));
        }
        this.items.addAll(asList(items));
    }

    public Invoice(LocalDateTime orderDate, Person person, List<Address> addresses, List<Item> items) {
        setOrderDate(orderDate);
        this.person = person;
        if (addresses != null) {
            this.addresses.addAll(addresses);
        }
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public void add(Item item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        total += item.getPrice();
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<List<List<Address>>> getListListList() {
        return listListList;
    }

    public void setListListList(List<List<List<Address>>> listListList) {
        this.listListList = listListList;
    }

    public Map<String, List<Address>> getMapList() {
        return mapList;
    }

    public void setMapList(Map<String, List<Address>> mapList) {
        this.mapList = mapList;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate != null ? orderDate.withNano(0) : null;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderDate, person, listListList, addresses, mapList, total, items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invoice)) {
            return false;
        }
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id) && Objects.equals(orderDate, invoice.orderDate) &&
               Objects.equals(person, invoice.person) && Objects.equals(listListList, invoice.listListList) &&
               Objects.equals(addresses, invoice.addresses) && Objects.equals(mapList, invoice.mapList) &&
               Objects.equals(total, invoice.total) && Objects.equals(items, invoice.items);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Invoice.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("orderDate=" + orderDate)
            .add("person=" + person)
            .add("listListList=" + listListList)
            .add("addresses=" + addresses)
            .add("mapList=" + mapList)
            .add("total=" + total)
            .add("items=" + items)
            .toString();
    }

    public boolean isPostLoad() {
        return postLoad;
    }

    public boolean isPostPersist() {
        return postPersist;
    }

    public boolean isPreLoad() {
        return preLoad;
    }

    public boolean isPrePersist() {
        return prePersist;
    }

    @PostLoad
    public void postLoad() {
        postLoad = true;
    }

    @PostPersist
    public void postPersist() {
        postPersist = true;
    }

    @PreLoad
    public void preLoad() {
        preLoad = true;
    }

    @PrePersist
    public void prePersist() {
        prePersist = true;
    }
}

@Embedded
class Item {
    private String name;
    private Double price;

    public Item() {
    }

    public Item(String name, Double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item)) {
            return false;
        }
        Item item = (Item) o;
        return Objects.equals(name, item.name) && Objects.equals(price, item.price);
    }
}

@Entity(cap = @CappedAt(count = 12))
@Indexes({
    @Index(fields = @Field("1")),
    @Index(fields = @Field("2")),
    @Index(fields = @Field("3"))})
class Person extends AbstractPerson {
    @Id
    private ObjectId id;

    private String firstName;

    private String lastName;

    public Person() {
    }

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Person)) {
            return false;
        }
        Person person = (Person) o;
        return Objects.equals(id, person.id) && Objects.equals(firstName, person.firstName) &&
               Objects.equals(lastName, person.lastName);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Person.class.getSimpleName() + "[", "]")
            .add("age=" + getAge())
            .add("id=" + id)
            .add("firstName='" + firstName + "'")
            .add("lastName='" + lastName + "'")
            .toString();
    }
}