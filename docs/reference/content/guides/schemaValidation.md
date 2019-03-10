+++
title = "Schema Validation"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

Morphia provides annotations that allow developers to define document validations for a collection to be defined alongside the 
other mapping data on an entity's source.  [Schema validation]({{< docsref "core/schema-validation/" >}})provides the capability to 
perform schema validation during updates and insertions.  Validation rules are on a per-collection basis and can be defined via 
annotations just like indexes are.

Below we have a basic entity definition.  Note the new annotation [@Validation](/javadoc/dev/morphia/annotations/Validation.html).

```java
@Entity("validation")
@Validation("{ number : { $gt : 10 } }")
public class DocumentValidation {
    @Id
    private ObjectId id;
    private String string;
    private int number;
    private Date date;
    
    ...
}
```

In this case, only one value is supplied to the annotation.  This string value is the query that will be used to match against any new 
documents or updated documents.  Should this query fail to match the new document, validation will fail on the document and it will be 
rejected.  In addition to the required query, there are two other values that can be configured based on your needs:  __level__ with a 
default of __STRICT__ and __action__ with a default of __ERROR__.  A __MODERATE__ validation level does not apply rules to updates on 
 existing invalid documents.  An __action__ setting of __WARN__ will merely log any validation violations.
