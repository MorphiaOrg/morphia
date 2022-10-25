package dev.morphia.test.mapping.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;

public class InstanceCreationTest extends TestBase {
    @Test
    public void basicReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).iterator(new FindOptions()
                .limit(1)).tryNext();
        assertEquals(author, loaded);
    }

    @Test
    public void duplicates() {
        SaveData toSave = new SaveData("id", new double[] { 1, 2, 3 }, List.of(
                new SaveData.ChildData(new double[] { 4, 5, 6 }, new ArrayList<>()),
                new SaveData.ChildData(new double[] { 7, 8, 9 }, new ArrayList<>()),
                new SaveData.ChildData(new double[] { 10, 11, 12 }, new ArrayList<>())));
        getDs().save(toSave);

        SaveData loadedData = getDs().find(SaveData.class).first();

        // Make sure the child objects are not the same
        assertNotSame(loadedData.elements.get(0), loadedData.elements.get(1));
        assertNotSame(loadedData.elements.get(0), loadedData.elements.get(2));
        assertNotSame(loadedData.elements.get(1), loadedData.elements.get(2));

        // Make sure the child position arrays are not the same
        assertFalse(Arrays.equals(loadedData.elements.get(0).child_position, loadedData.elements.get(1).child_position));
        assertFalse(Arrays.equals(loadedData.elements.get(0).child_position, loadedData.elements.get(2).child_position));
        assertFalse(Arrays.equals(loadedData.elements.get(1).child_position, loadedData.elements.get(2).child_position));
    }

    @Entity
    private static class Author extends BaseEntity {

        private final String name;

        public Author(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Author author = (Author) o;

            if (id != null ? !id.equals(author.id) : author.id != null) {
                return false;
            }
            return name != null ? name.equals(author.name) : author.name == null;
        }

        @Override
        public String toString() {
            return "Author{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    private static class BaseEntity {
        @Id
        protected ObjectId id;

        public ObjectId getId() {
            return id;
        }
    }

    @Entity("saved_stuff")
    public static class SaveData {
        @Id
        public final String saveName;
        public final double[] position;
        public final List<ChildData> elements;

        public SaveData(String saveName, double[] position, List<ChildData> elements) {
            this.saveName = saveName;
            this.position = position;
            this.elements = elements;
        }

        @Override
        public String toString() {
            return "SaveData{" +
                    "saveName='" + saveName + '\'' +
                    ", position=" + Arrays.toString(position) +
                    ", elements=" + elements +
                    '}';
        }

        @Entity
        public static class ChildData {
            public final double[] child_position;
            public final List<ChildData> child_elements;

            public ChildData(double[] child_position, List<ChildData> child_elements) {
                this.child_position = child_position;
                this.child_elements = child_elements;
            }

            @Override
            public String toString() {
                return "\nChildData{" +
                        "child_position=" + Arrays.toString(child_position) +
                        ", child_elements=" + child_elements +
                        '}';
            }
        }
    }
}
