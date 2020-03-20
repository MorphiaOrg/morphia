package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EnumMappingTest extends TestBase {
    @Test
    public void getMapOfEnum() {
        Class1 entity = new Class1();
        entity.getMap().put("key", Foo.BAR);
        getDs().save(entity);

        getMapper().map(Class1.class);

        entity = getDs().find(Class1.class).iterator(new FindOptions().limit(1)).tryNext();
        final Map<String, Foo> map = entity.getMap();
        Foo b = map.get("key");
        Assert.assertNotNull(b);
    }

    @Test
    public void testCustomer() {
        Customer customer = new Customer();
        customer.add(WebTemplateType.CrewContract, new WebTemplate("template #1"));
        customer.add(WebTemplateType.CrewContractHeader, new WebTemplate("template #2"));

        getDs().save(customer);
        Customer loaded = getDs().find(Customer.class)
                                 .filter(eq("_id", customer.id))
                                 .first();
        Assert.assertEquals(customer.map, loaded.map);
    }

    @Test
    public void testCustomerWithArrayList() {
        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .storeEmpties(true)
                                             .storeNulls(true)
                                             .build();
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);

        Mapper mapper = datastore.getMapper();
        mapper.map(CustomerWithArrayList.class);

        CustomerWithArrayList customer = new CustomerWithArrayList();

        List<WebTemplate> templates1 = new ArrayList<>();
        templates1.add(new WebTemplate("template #1.1"));
        templates1.add(new WebTemplate("template #1.2"));
        customer.add(WebTemplateType.CrewContract, templates1);

        List<WebTemplate> templates2 = new ArrayList<>();
        templates1.add(new WebTemplate("template #2.1"));
        templates1.add(new WebTemplate("template #2.2"));
        customer.add(WebTemplateType.CrewContractHeader, templates2);

        getDs().save(customer);
        final Datastore datastore1 = getDs();
        CustomerWithArrayList loaded = datastore1.find(CustomerWithArrayList.class)
                                                 .filter(eq("_id", customer.id))
                                                 .first();

        Assert.assertEquals(customer.mapWithArrayList, loaded.mapWithArrayList);
    }

    @Test
    public void testCustomerWithList() {

        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .storeEmpties(true)
                                             .storeNulls(true)
                                             .build();
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);
        Mapper mapper = datastore.getMapper();

        mapper.map(CustomerWithArrayList.class);
        CustomerWithList customer = new CustomerWithList();

        List<WebTemplate> templates1 = new ArrayList<>();
        templates1.add(new WebTemplate("template #1.1"));
        templates1.add(new WebTemplate("template #1.2"));
        customer.add(WebTemplateType.CrewContract, templates1);

        List<WebTemplate> templates2 = new ArrayList<>();
        templates1.add(new WebTemplate("template #2.1"));
        templates1.add(new WebTemplate("template #2.2"));
        customer.add(WebTemplateType.CrewContractHeader, templates2);

        getDs().save(customer);
        final Datastore datastore1 = getDs();
        CustomerWithList loaded = datastore1.find(CustomerWithList.class)
                                            .filter(eq("_id", customer.id))
                                            .first();

        Assert.assertEquals(customer.mapWithList, loaded.mapWithList);
    }

    @Test
    public void testEnumMapping() {
        getDs().getDatabase().drop();

        getMapper().map(ContainsEnum.class);

        getDs().save(new ContainsEnum());
        Assert.assertEquals(1, getDs().find(ContainsEnum.class).filter(eq("foo", Foo.BAR))
                                      .count());
        Assert.assertEquals(1, getDs().find(ContainsEnum.class).disableValidation().filter(eq("foo", Foo.BAR))
                                      .count());
    }

    enum Foo {
        BAR,
        BAZ
    }

    public enum WebTemplateType {
        CrewContract("Contract"),
        CrewContractHeader("Contract Header");

        private String text;

        WebTemplateType(final String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

    }

    @Entity("user")
    public static class Class1 {
        @Id
        private ObjectId id;
        private Map<String, Foo> map = new HashMap<>();

        public Map<String, Foo> getMap() {
            return map;
        }
    }

    @Entity
    public static class ContainsEnum {
        @Id
        private ObjectId id;
        private Foo foo = Foo.BAR;
    }

    @Entity(useDiscriminator = false)
    public static class Customer {
        private final Map<WebTemplateType, WebTemplate> map = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(final WebTemplateType type, final WebTemplate template) {
            map.put(type, template);
        }

    }

    @Entity(useDiscriminator = false)
    public static class CustomerWithArrayList {
        private final Map<WebTemplateType, List<WebTemplate>> mapWithArrayList
            = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(final WebTemplateType type, final List<WebTemplate> templates) {
            mapWithArrayList.put(type, templates);
        }
    }

    @Entity(useDiscriminator = false)
    public static class CustomerWithList {
        private final Map<WebTemplateType, List<WebTemplate>> mapWithList = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(final WebTemplateType type, final List<WebTemplate> templates) {
            mapWithList.put(type, templates);
        }
    }

    @Embedded
    public static class WebTemplate {
        private ObjectId id = new ObjectId();
        private String templateName;
        private String content;

        public WebTemplate() {
        }

        public WebTemplate(final String content) {
            this.content = content;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
            result = 31 * result + (content != null ? content.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final WebTemplate that = (WebTemplate) o;

            if (content != null ? !content.equals(that.content) : that.content != null) {
                return false;
            }
            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            if (templateName != null ? !templateName.equals(that.templateName)
                                     : that.templateName != null) {
                return false;
            }

            return true;
        }
    }

}
