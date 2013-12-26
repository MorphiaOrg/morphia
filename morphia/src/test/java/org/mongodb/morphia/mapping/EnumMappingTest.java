package org.mongodb.morphia.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PreSave;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EnumMappingTest extends TestBase {
  public static class ContainsEnum {
    @Id
    private ObjectId id;
    private Foo foo = Foo.BAR;

    @PreSave
    void testMapping() {

    }
  }

  enum Foo {
    BAR() {
    },
    BAZ
  }

  public enum WebTemplateType {
    CrewContract("Contract"), CrewContractHeader("Contract Header");

    private String text;

    private WebTemplateType(final String text) {
      this.text = text;
    }

    public String getText() {
      return text;
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

    @Override
    public int hashCode() {
      int result = id != null ? id.hashCode() : 0;
      result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
      result = 31 * result + (content != null ? content.hashCode() : 0);
      return result;
    }
  }

  @Entity(noClassnameStored = true)
  public static class Customer {
    @Id
    private ObjectId id;
    private final Map<WebTemplateType, WebTemplate> map = new HashMap<WebTemplateType, WebTemplate>();

    public void add(final WebTemplateType type, final WebTemplate template) {
      map.put(type, template);
    }

  }

  @Entity(noClassnameStored = true)
  public static class CustomerWithList {
    @Id
    private ObjectId id;

    private final Map<WebTemplateType, List<WebTemplate>> mapWithList = new HashMap<WebTemplateType, List<WebTemplate>>();

    public void add(final WebTemplateType type, final List<WebTemplate> templates) {
      mapWithList.put(type, templates);
    }
  }

  @Entity(noClassnameStored = true)
  public static class CustomerWithArrayList {
    @Id
    private ObjectId id;

    private final Map<WebTemplateType, ArrayList<WebTemplate>> mapWithArrayList = new HashMap<WebTemplateType, ArrayList<WebTemplate>>();

    public void add(final WebTemplateType type, final ArrayList<WebTemplate> templates) {
      mapWithArrayList.put(type, templates);
    }
  }

  @Test
  public void testEnumMapping() throws Exception {
    getMorphia().map(ContainsEnum.class);

    getDs().save(new ContainsEnum());
    Assert.assertEquals(1, getDs().createQuery(ContainsEnum.class).field("foo").equal(Foo.BAR)
        .countAll());
    Assert.assertEquals(1, getDs().createQuery(ContainsEnum.class).filter("foo", Foo.BAR)
        .countAll());
    Assert.assertEquals(1,
        getDs().createQuery(ContainsEnum.class).disableValidation().filter("foo", Foo.BAR)
            .countAll());
  }

  @Test
  public void testCustomer() {
    Customer customer = new Customer();
    customer.add(WebTemplateType.CrewContract, new WebTemplate("template #1"));
    customer.add(WebTemplateType.CrewContractHeader, new WebTemplate("template #2"));

    getDs().save(customer);
    Customer loaded = getDs().get(customer);
    Assert.assertEquals(customer.map, loaded.map);
  }

  @Test
  public void testCustomerWithList() {
    CustomerWithList customer = new CustomerWithList();

    WebTemplate template1_1 = new WebTemplate("template #1.1");
    WebTemplate template1_2 = new WebTemplate("template #1.2");
    List<WebTemplate> templates1 = new ArrayList<WebTemplate>();
    templates1.add(template1_1);
    templates1.add(template1_2);
    customer.add(WebTemplateType.CrewContract, templates1);

    WebTemplate template2_1 = new WebTemplate("template #2.1");
    WebTemplate template2_2 = new WebTemplate("template #2.2");
    List<WebTemplate> templates2 = new ArrayList<WebTemplate>();
    templates1.add(template2_1);
    templates1.add(template2_2);
    customer.add(WebTemplateType.CrewContractHeader, templates2);

    getDs().save(customer);
    CustomerWithList loaded = getDs().get(customer);

    Assert.assertEquals(customer.mapWithList, loaded.mapWithList);
  }

  @Test
  public void testCustomerWithArrayList() {
    CustomerWithArrayList customer = new CustomerWithArrayList();

    WebTemplate template1_1 = new WebTemplate("template #1.1");
    WebTemplate template1_2 = new WebTemplate("template #1.2");
    ArrayList<WebTemplate> templates1 = new ArrayList<WebTemplate>();
    templates1.add(template1_1);
    templates1.add(template1_2);
    customer.add(WebTemplateType.CrewContract, templates1);

    WebTemplate template2_1 = new WebTemplate("template #2.1");
    WebTemplate template2_2 = new WebTemplate("template #2.2");
    ArrayList<WebTemplate> templates2 = new ArrayList<WebTemplate>();
    templates1.add(template2_1);
    templates1.add(template2_2);
    customer.add(WebTemplateType.CrewContractHeader, templates2);

    getDs().save(customer);
    CustomerWithArrayList loaded = getDs().get(customer);

    Assert.assertEquals(customer.mapWithArrayList, loaded.mapWithArrayList);
  }

}
