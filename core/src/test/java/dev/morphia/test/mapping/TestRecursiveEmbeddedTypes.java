package dev.morphia.test.mapping;

import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

public class TestRecursiveEmbeddedTypes extends TestBase {
    @Test
    public void savesSelfReferencingTypeWithNullChild() {
        getMapper().map(Node.class);
        Node node = new Node();
        node.name = "root";

        getDs().save(node);

        Node loaded = getDs().find(Node.class).filter(eq("_id", node.id)).first();
        assertNotNull(loaded);
        assertEquals("root", loaded.name);
        assertNull(loaded.child);
    }

    @Test
    public void savesAndRefreshesFiniteRecursiveChain() {
        getMapper().map(Node.class);
        Node node = new Node();
        node.name = "root";
        node.child = new Node();
        node.child.name = "middle";
        node.child.child = new Node();
        node.child.child.name = "leaf";
        getDs().save(node);

        Node loaded = getDs().find(Node.class).filter(eq("_id", node.id)).first();
        assertNotNull(loaded);
        assertEquals("middle", loaded.child.name);
        assertEquals("leaf", loaded.child.child.name);
        assertNull(loaded.child.child.child);

        loaded.child.name = "local change";
        getDs().refresh(loaded);
        assertEquals("middle", loaded.child.name);
        assertEquals("leaf", loaded.child.child.name);
    }

    @Test
    public void savesEntityContainingRecursiveEmbeddedType() {
        getMapper().map(Envelope.class, Branch.class);
        Envelope envelope = new Envelope();
        envelope.branch = new Branch();
        envelope.branch.name = "embedded";

        getDs().save(envelope);

        Envelope loaded = getDs().find(Envelope.class).filter(eq("_id", envelope.id)).first();
        assertNotNull(loaded);
        assertEquals("embedded", loaded.branch.name);
        assertNull(loaded.branch.child);
    }

    @Test
    public void savesMutuallyRecursiveTypes() {
        getMapper().map(Left.class, Right.class);
        Left left = new Left();
        left.name = "root";
        left.right = new Right();
        left.right.name = "right";
        left.right.left = new Left();
        left.right.left.name = "leaf";

        getDs().save(left);

        Left loaded = getDs().find(Left.class).filter(eq("_id", left.id)).first();
        assertNotNull(loaded);
        assertEquals("right", loaded.right.name);
        assertEquals("leaf", loaded.right.left.name);
        assertNull(loaded.right.left.right);
    }

    @Test
    public void savesRecursiveContainers() {
        getMapper().map(Tree.class);
        Tree tree = new Tree();
        Tree leaf = new Tree();
        leaf.name = "leaf";
        tree.children = List.of(leaf);
        tree.named = Map.of("leaf", leaf);
        tree.array = new Tree[] { leaf };

        getDs().save(tree);

        Tree loaded = getDs().find(Tree.class).filter(eq("_id", tree.id)).first();
        assertNotNull(loaded);
        assertEquals(1, loaded.children.size());
        assertEquals("leaf", loaded.children.get(0).name);
        assertNull(loaded.children.get(0).children);
        assertEquals("leaf", loaded.named.get("leaf").name);
        assertNull(loaded.named.get("leaf").named);
        assertEquals(1, loaded.array.length);
        assertEquals("leaf", loaded.array[0].name);
        assertNull(loaded.array[0].array);
    }

    @Test
    public void preservesCustomPropertyCodecInRecursiveType() {
        withConfig(morphiaConfig.codecProvider(new TokenCodecProvider()), () -> {
            getMapper().map(CustomNode.class);
            CustomNode node = new CustomNode();
            node.token = new Token("root");
            node.child = new CustomNode();
            node.child.token = new Token("child");

            getDs().save(node);

            CustomNode loaded = getDs().find(CustomNode.class).filter(eq("_id", node.id)).first();
            assertNotNull(loaded);
            assertEquals("root", loaded.token.value);
            assertEquals("child", loaded.child.token.value);
            assertNull(loaded.child.child);
            Document stored = getDs().getCollection(CustomNode.class).withDocumentClass(Document.class)
                    .find(new Document("_id", node.id)).first();
            assertNotNull(stored);
            assertEquals("custom:root", stored.getString("token"));
            assertEquals("custom:child", stored.get("child", Document.class).getString("token"));
        });
    }

    @Entity
    public static class Node {
        @Id
        ObjectId id;
        String name;
        Node child;
    }

    @Entity
    public static class Envelope {
        @Id
        ObjectId id;
        Branch branch;
    }

    @Entity
    public static class Branch {
        String name;
        Branch child;
    }

    @Entity
    public static class Left {
        @Id
        ObjectId id;
        String name;
        Right right;
    }

    @Entity
    public static class Right {
        String name;
        Left left;
    }

    @Entity
    public static class Tree {
        @Id
        ObjectId id;
        String name;
        List<Tree> children;
        Map<String, Tree> named;
        Tree[] array;
    }

    @Entity
    public static class CustomNode {
        @Id
        ObjectId id;
        Token token;
        CustomNode child;
    }

    private record Token(String value) {
    }

    private static class TokenCodecProvider implements CodecProvider {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
            return type == Token.class ? (Codec<T>) new TokenCodec() : null;
        }
    }

    private static class TokenCodec implements Codec<Token> {
        @Override
        public Token decode(BsonReader reader, DecoderContext context) {
            String value = reader.readString();
            assertTrue(value.startsWith("custom:"));
            return new Token(value.substring("custom:".length()));
        }

        @Override
        public void encode(BsonWriter writer, Token value, EncoderContext context) {
            writer.writeString("custom:" + value.value);
        }

        @Override
        public Class<Token> getEncoderClass() {
            return Token.class;
        }
    }
}
