package dev.morphia.test.mapping;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import org.bson.Document;
import org.testng.annotations.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestReferenceAnnotationPosition extends TestBase {

	@Test
	public void testReferenceAnnotationShouldNeverEmbedDocuments() {
		getDs().getMapper().map(Box.class, Item.class);

		Item item1 = new Item("item one");
		Item item2 = new Item("item two");
		Box box = new Box();
		box.setItems(Set.of(item1, item2));
		box.setMoreItems(Set.of(item1, item2));
		getDs().save(List.of(item1, item2));
		getDs().save(box);

		MongoCollection<Document> collection = getMapper()
			.getCollection(Box.class)
			.withDocumentClass(Document.class);

		Document firstDoc = collection.find().first();
		Object items = firstDoc.get("items");
		Object moreItems = firstDoc.get("moreItems");

		assertEquals(items, moreItems, "Items should be stored as the same documents");
	}

}

@Entity
class Box {
	@Id
	private UUID id = UUID.randomUUID();
	@Reference
	@AnAnnotation
	private Set<Item> items;

	@AnAnnotation
	@Reference
	private Set<Item> moreItems;

	Box() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Set<Item> getItems() {
		return items;
	}

	public void setItems(Set<Item> items) {
		this.items = items;
	}

	public Set<Item> getMoreItems() {
		return moreItems;
	}

	public void setMoreItems(Set<Item> moreItems) {
		this.moreItems = moreItems;
	}
}

@Entity
class Item {
	@Id
	private UUID id = UUID.randomUUID();
	private String name;

	Item() {
	}

	public Item(String name) {
		this.name = name;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
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

//this is a random annotation which doesn't play any role beside separating @Reference and field
@Target({FIELD})
@Retention(RUNTIME)
@interface AnAnnotation {

}
