package dev.morphia.test.mapping;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;

public class TestEnumMapping extends TestBase {
    @Test
    public void getMapOfEnum() {
        Class1 entity = new Class1();
        entity.map.put("key", Foo.BAR);
        getDs().save(entity);

        getMapper().map(Class1.class);

        entity = getDs().find(Class1.class).first();
        Assert.assertNotNull(entity.map.get("key"));
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
        assertEquals(customer.map, loaded.map);
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

        assertEquals(customer.mapWithArrayList, loaded.mapWithArrayList);
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

        assertEquals(customer.mapWithList, loaded.mapWithList);
    }

    @Test
    public void testEnumMapping() {
        getMapper().map(ContainsEnum.class);

        getDs().save(new ContainsEnum());
        assertEquals(getDs().find(ContainsEnum.class).filter(eq("foo", Foo.BAR))
                            .count(), 1);
        assertEquals(getDs().find(ContainsEnum.class).disableValidation().filter(eq("foo", Foo.BAR))
                            .count(), 1);
    }

    private enum Foo {
        BAR,
        BAZ
    }

    private enum WebTemplateType {
        CrewContract("Contract"),
        CrewContractHeader("Contract Header");

        private final String text;

        WebTemplateType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

    }

    @Entity("user")
    private static class Class1 {
        @Id
        private ObjectId id;
        private final Map<String, Foo> map = new HashMap<>();
    }

    @Entity
    private static class ContainsEnum {
        @Id
        private ObjectId id;
        private final Foo foo = Foo.BAR;
    }

    @Entity(useDiscriminator = false)
    private static class Customer {
        private final Map<WebTemplateType, WebTemplate> map = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(WebTemplateType type, WebTemplate template) {
            map.put(type, template);
        }

    }

    @Entity(useDiscriminator = false)
    private static class CustomerWithArrayList {
        private final Map<WebTemplateType, List<WebTemplate>> mapWithArrayList
            = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(WebTemplateType type, List<WebTemplate> templates) {
            mapWithArrayList.put(type, templates);
        }
    }

    @Entity(useDiscriminator = false)
    private static class CustomerWithList {
        private final Map<WebTemplateType, List<WebTemplate>> mapWithList = new HashMap<>();
        @Id
        private ObjectId id;

        public void add(WebTemplateType type, List<WebTemplate> templates) {
            mapWithList.put(type, templates);
        }
    }

    @Entity
    private static class WebTemplate {
        private final ObjectId id = new ObjectId();
        private String templateName;
        private String content;

        public WebTemplate() {
        }

        public WebTemplate(String content) {
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
        public boolean equals(Object o) {
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
            return templateName != null ? templateName.equals(that.templateName) : that.templateName == null;
        }
    }

}
