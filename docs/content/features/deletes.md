---
title: "Deletes"
weight: 8
description: "Learn how to delete documents from the database using Morphia queries"
---

## Deletes

Queries are used to delete documents from the database as well.
Using
[Query#delete()](/javadoc/dev/morphia/query/Query.html#delete()), we can delete documents matching the query.
The default operation will only delete the first matching document.
However, you can opt to delete all matches by passing in the appropriate options:

```java
datastore
    .find(Hotel.class)
    .filter(gt("stars", 100))
    .delete(new DeleteOptions()
                     .multi(true));
```
