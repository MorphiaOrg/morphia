+++
title = "Validation Extension"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

This is a simple extension to Morphia to process JSR 303 Validation Annotations.

## Using
Add this at the start of your application (or wherever you create your morphia instances).

`````java
new ValidationExtension(morphia);
`````

### Example

Here is a simple example using (as an example) Hibernate validation:

```java
...
import org.hibernate.validator.constraints.Email;
...

@Entity
public class Userlike {
	@Id ObjectId id;
	@Email String email;
}

```

## Implementation

This is a lightweight wrapper around the JSR 303 API. It installs a simple global entity interceptor which listens to all 
[life cycle methods]({{< relref "/guides/lifeCycleMethods.md" >}}) needed for validation. You can use any implementation of JSR 303 by
just adding it to the classpath.

You can look at the code [here]({{< srcref "validation/src/main/java/org/mongodb/morphia/ValidationExtension.java" >}}).

## Dependencies

### Manual
- [Hibernate Validator](http://hibernate.org/validator/)

### Maven

If you use Maven to manage your project, you can reference Morphia as a dependency:
```xml
<dependency>
    <groupId>org.mongodb.morphia</groupId>
    <artifactId>morphia-validation</artifactId>
    <version>1.3.2</version>
</dependency>
```
