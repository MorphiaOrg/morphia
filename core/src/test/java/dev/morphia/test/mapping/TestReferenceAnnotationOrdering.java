package dev.morphia.test.mapping;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.testng.annotations.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.testng.Assert.assertEquals;

@Entity
class Box {
	@Id
	UUID id = UUID.randomUUID();
	@Reference
	@AnAnnotation
	Set<Item> items;

	@AnAnnotation
	@Reference
	Set<Item> moreItems;
}

@Entity
class Item {
	@Id
	private final UUID id = UUID.randomUUID();
	private String name;
	Item() {
	}

	public Item(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Item item = (Item) o;
		return id.equals(item.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}

public class TestReferenceAnnotationOrdering extends TestBase {

	@Test
	public void testReferenceAnnotationShouldNeverEmbedDocuments() {
		getDs().getMapper().map(Box.class, Item.class);

		Item item1 = new Item("item one");
		Item item2 = new Item("item two");
		Box box = new Box();
		box.items = Set.of(item1, item2);
		box.moreItems = Set.of(item1, item2);
		getDs().save(List.of(item1, item2));
		getDs().save(box);

		MongoCollection<Document> collection = getDs()
			.getCollection(Box.class)
			.withDocumentClass(Document.class);

		Document firstDoc = collection.find().first();
		Object items = firstDoc.get("items");
		Object moreItems = firstDoc.get("moreItems");

		assertEquals(items, moreItems, "Items should be stored as the same documents");
	}

}

//this is a random annotation which doesn't play any role beside separating @Reference and field
@Target({FIELD})
@Retention(RUNTIME)
@interface AnAnnotation {

}
