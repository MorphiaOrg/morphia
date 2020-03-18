/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedField;
import dev.morphia.query.LegacyQuery;
import dev.morphia.query.Query;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

@Category(Reference.class)
@Ignore("get around to this later")
public class TestSerializedFormat extends TestBase {
    @Test
    public void testQueryFormat() {
        Query<ReferenceType> query = getDs().find(ReferenceType.class)
                                            .filter(eq("id", new ObjectId(0, 0)))
                                            .filter(eq("referenceType", new ReferenceType(2, "far")))
                                            .filter(eq("embeddedType", new EmbeddedReferenceType(3, "strikes")))

                                            .filter(eq("string", "some value"))

                                            .field("embeddedArray").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                     .filter(eq("number", 3)).filter(eq("text", "strikes")))
                                            .field("embeddedSet").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                   .filter(eq("number", 3)).filter(eq("text", "strikes")))
                                            .field("embeddedList").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                    .filter(eq("number", 3)).filter(eq("text", "strikes")))

                                            .filter(eq("map.bar", new EmbeddedReferenceType(1, "chance")))
                                            .field("mapOfList.bar").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .field("mapOfList.foo").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                     .filter(eq("number", 1))
                                                                                     .filter(eq("text", "chance")))

                                            .filter(eq("selfReference", new ReferenceType(1, "blah")))

                                            .field("mixedTypeList").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                     .filter(eq("number", 3)).filter(eq("text", "strikes")))
                                            .field("mixedTypeList").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .filter(eq("mixedTypeMap.foo", new ReferenceType(3, "strikes")))
                                            .filter(eq("mixedTypeMap.bar", new EmbeddedReferenceType(3, "strikes")))
                                            .field("mixedTypeMapOfList.bar").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .field("mixedTypeMapOfList.foo").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                              .filter(eq("number", 3))
                                                                                              .filter(eq("text", "strikes")))

                                            .filter(eq("referenceMap.foo", new ReferenceType(1, "chance")))
                                            .filter(eq("referenceMap.bar", new EmbeddedReferenceType(1, "chance")));

        Document document = ((LegacyQuery) query).toDocument();
        final Document parse = Document.parse(readFully("/QueryStructure.json"));
        Assert.assertEquals(parse, document);
    }

    private void verifyCoverage(final Document document) {
        for (MappedField field : getMapper().getMappedClass(ReferenceType.class).getFields()) {
            String name = field.getMappedFieldName();
            boolean found = document.containsKey(name);
            if (!found) {
                for (String s : document.keySet()) {
                    found |= s.startsWith(name + ".");

                }
            }
            assertTrue("Not found in document: " + name, found);
        }
    }

    @Test
    public void testSavedEntityFormat() {
        ReferenceType entity = new ReferenceType(1, "I'm a field value");

        entity.setReferenceType(new ReferenceType(42, "reference"));
        entity.setEmbeddedType(new EmbeddedReferenceType(18, "embedded"));

        entity.setEmbeddedSet(new HashSet<>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
            new EmbeddedReferenceType(1, "Love"))));
        entity.setEmbeddedList(asList(new EmbeddedReferenceType(42, "Douglas Adams"), new EmbeddedReferenceType(1, "Love")));
        entity.setEmbeddedArray(new EmbeddedReferenceType[]{new EmbeddedReferenceType(42, "Douglas Adams"),
            new EmbeddedReferenceType(1, "Love")});

        entity.getMap().put("first", new EmbeddedReferenceType(42, "Douglas Adams"));
        entity.getMap().put("second", new EmbeddedReferenceType(1, "Love"));

        entity.getMapOfList().put("first", asList(new EmbeddedReferenceType(42, "Douglas Adams"), new EmbeddedReferenceType(1, "Love")));
        entity.getMapOfList().put("second", asList(new EmbeddedReferenceType(1, "Love"), new EmbeddedReferenceType(42, "Douglas Adams")));


        entity.getMapOfSet().put("first", new HashSet<>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
            new EmbeddedReferenceType(1, "Love"))));
        entity.getMapOfSet().put("second", new HashSet<>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
            new EmbeddedReferenceType(1, "Love"))));

        entity.setSelfReference(entity);
        entity.setIdOnly(entity);

        entity.setReferenceArray(new ReferenceType[]{new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")});
        entity.setReferenceList(asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")));
        entity.setReferenceSet(new HashSet<>(asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3"))));
        entity.getReferenceMap().put("first", new ReferenceType(2, "text 2"));
        entity.getReferenceMap().put("second", new ReferenceType(3, "text 3"));
        entity.getReferenceMapOfList().put("first", asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")));
        entity.getReferenceMapOfList().put("second", singletonList(new ReferenceType(3, "text 3")));

        entity.setMixedTypeArray(new ReferenceType[]{new ReferenceType(2, "text 2"), new ClassNameReferenceType(3, "text 3")});
        entity.setMixedTypeList(asList(new ReferenceType(2, "text 2"), new ClassNameReferenceType(3, "text 3")));
        entity.setMixedTypeSet(new HashSet<>(asList(new ReferenceType(2, "text 2"),
            new ClassNameReferenceType(3, "text 3"))));
        entity.getMixedTypeMap().put("first", new ReferenceType(2, "text 2"));
        entity.getMixedTypeMap().put("second", new ClassNameReferenceType(3, "text 3"));
        entity.getMixedTypeMapOfList().put("first", asList(new ReferenceType(2, "text 2"),
                                                           new ClassNameReferenceType(3, "text 3")));
        entity.getMixedTypeMapOfList().put("second", singletonList(new ClassNameReferenceType(3, "text 3")));

        getDs().save(entity);

        String collectionName = getDs().getMapper().getMappedClass(ReferenceType.class).getCollectionName();
        Document document = getDatabase().getCollection(collectionName).find().first();
        Assert.assertEquals(Document.parse(readFully("/ReferenceType.json")), document);
        verifyCoverage(document);
    }

    private String readFully(final String name) {
        return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(name)))
            .lines()
            .collect(java.util.stream.Collectors.joining("\n"));
    }
}

@SuppressWarnings({"WeakerAccess", "unused"})
@Entity("ondisk")
class ReferenceType {
    @Id
    private Integer id;
    private ReferenceType referenceType;
    private EmbeddedReferenceType embeddedType;

    private String string;
    private EmbeddedReferenceType[] embeddedArray;
    private Set<EmbeddedReferenceType> embeddedSet;
    private List<EmbeddedReferenceType> embeddedList;
    private Map<String, EmbeddedReferenceType> map = new TreeMap<>();
    private Map<String, List<EmbeddedReferenceType>> mapOfList = new TreeMap<>();
    private Map<String, Set<EmbeddedReferenceType>> mapOfSet = new TreeMap<>();
    @Reference
    private ReferenceType selfReference;
    @Reference(idOnly = true)
    private ReferenceType idOnly;

    private ReferenceType[] referenceArray;
    private Set<ReferenceType> referenceSet;
    private List<ReferenceType> referenceList;
    private Map<String, ReferenceType> referenceMap = new TreeMap<>();
    private Map<String, List<ReferenceType>> referenceMapOfList = new TreeMap<>();

    private ReferenceType[] mixedTypeArray;
    private Set<? extends ReferenceType> mixedTypeSet;
    private List<? extends ReferenceType> mixedTypeList;
    private Map<String, ? super ReferenceType> mixedTypeMap = new TreeMap<>();
    private Map<String, List<? extends ReferenceType>> mixedTypeMapOfList = new TreeMap<>();

    protected ReferenceType() {
    }

    protected ReferenceType(final int id, final String string) {
        this.id = id;
        this.string = string;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(final ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public EmbeddedReferenceType getEmbeddedType() {
        return embeddedType;
    }

    public void setEmbeddedType(final EmbeddedReferenceType embeddedType) {
        this.embeddedType = embeddedType;
    }

    public EmbeddedReferenceType[] getEmbeddedArray() {
        return embeddedArray;
    }

    public void setEmbeddedArray(final EmbeddedReferenceType[] embeddedArray) {
        this.embeddedArray = embeddedArray;
    }

    public List<EmbeddedReferenceType> getEmbeddedList() {
        return embeddedList;
    }

    public void setEmbeddedList(final List<EmbeddedReferenceType> embeddedList) {
        this.embeddedList = embeddedList;
    }

    public Set<EmbeddedReferenceType> getEmbeddedSet() {
        return embeddedSet;
    }

    public void setEmbeddedSet(final Set<EmbeddedReferenceType> embeddedSet) {
        this.embeddedSet = embeddedSet;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public ReferenceType getIdOnly() {
        return idOnly;
    }

    public void setIdOnly(final ReferenceType idOnly) {
        this.idOnly = idOnly;
    }

    public Map<String, EmbeddedReferenceType> getMap() {
        return map;
    }

    public void setMap(final Map<String, EmbeddedReferenceType> map) {
        this.map = map;
    }

    public Map<String, List<EmbeddedReferenceType>> getMapOfList() {
        return mapOfList;
    }

    public void setMapOfList(final Map<String, List<EmbeddedReferenceType>> mapOfList) {
        this.mapOfList = mapOfList;
    }

    public Map<String, Set<EmbeddedReferenceType>> getMapOfSet() {
        return mapOfSet;
    }

    public void setMapOfSet(final Map<String, Set<EmbeddedReferenceType>> mapOfSet) {
        this.mapOfSet = mapOfSet;
    }

    public ReferenceType[] getMixedTypeArray() {
        return mixedTypeArray;
    }

    public void setMixedTypeArray(final ReferenceType[] mixedTypeArray) {
        this.mixedTypeArray = mixedTypeArray;
    }

    public List<? extends ReferenceType> getMixedTypeList() {
        return mixedTypeList;
    }

    public void setMixedTypeList(final List<? extends ReferenceType> mixedTypeList) {
        this.mixedTypeList = mixedTypeList;
    }

    public Map<String, ? super ReferenceType> getMixedTypeMap() {
        return mixedTypeMap;
    }

    public void setMixedTypeMap(final Map<String, ? super ReferenceType> mixedTypeMap) {
        this.mixedTypeMap = mixedTypeMap;
    }

    public Map<String, List<? extends ReferenceType>> getMixedTypeMapOfList() {
        return mixedTypeMapOfList;
    }

    public void setMixedTypeMapOfList(
        final Map<String, List<? extends ReferenceType>> mixedTypeMapOfList) {
        this.mixedTypeMapOfList = mixedTypeMapOfList;
    }

    public Set<? extends ReferenceType> getMixedTypeSet() {
        return mixedTypeSet;
    }

    public void setMixedTypeSet(final Set<? extends ReferenceType> mixedTypeSet) {
        this.mixedTypeSet = mixedTypeSet;
    }

    public ReferenceType[] getReferenceArray() {
        return referenceArray;
    }

    public void setReferenceArray(final ReferenceType[] referenceArray) {
        this.referenceArray = referenceArray;
    }

    public List<ReferenceType> getReferenceList() {
        return referenceList;
    }

    public void setReferenceList(final List<ReferenceType> referenceList) {
        this.referenceList = referenceList;
    }

    public Map<String, ReferenceType> getReferenceMap() {
        return referenceMap;
    }

    public void setReferenceMap(final Map<String, ReferenceType> referenceMap) {
        this.referenceMap = referenceMap;
    }

    public Map<String, List<ReferenceType>> getReferenceMapOfList() {
        return referenceMapOfList;
    }

    public void setReferenceMapOfList(final Map<String, List<ReferenceType>> referenceMapOfList) {
        this.referenceMapOfList = referenceMapOfList;
    }

    public Set<ReferenceType> getReferenceSet() {
        return referenceSet;
    }

    public void setReferenceSet(final Set<ReferenceType> referenceSet) {
        this.referenceSet = referenceSet;
    }

    public ReferenceType getSelfReference() {
        return selfReference;
    }

    public void setSelfReference(final ReferenceType selfReference) {
        this.selfReference = selfReference;
    }

    public String getString() {
        return string;
    }

    public void setString(final String string) {
        this.string = string;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceType)) {
            return false;
        }

        final ReferenceType that = (ReferenceType) o;

        if (!id.equals(that.id)) {
            return false;
        }
        if (!Objects.equals(string, that.string)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(embeddedArray, that.embeddedArray)) {
            return false;
        }
        if (!Objects.equals(embeddedSet, that.embeddedSet)) {
            return false;
        }
        if (!Objects.equals(embeddedList, that.embeddedList)) {
            return false;
        }
        if (!Objects.equals(map, that.map)) {
            return false;
        }
        if (!Objects.equals(mapOfList, that.mapOfList)) {
            return false;
        }
        if (!Objects.equals(mapOfSet, that.mapOfSet)) {
            return false;
        }
        if (!Objects.equals(selfReference, that.selfReference)) {
            return false;
        }
        if (!Objects.equals(idOnly, that.idOnly)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(referenceArray, that.referenceArray)) {
            return false;
        }
        if (!Objects.equals(referenceSet, that.referenceSet)) {
            return false;
        }
        if (!Objects.equals(referenceList, that.referenceList)) {
            return false;
        }
        if (!Objects.equals(referenceMap, that.referenceMap)) {
            return false;
        }
        return Objects.equals(referenceMapOfList, that.referenceMapOfList);

    }

}

@Entity
@SuppressWarnings("unused")
class ClassNameReferenceType extends ReferenceType {
    ClassNameReferenceType() {
    }

    ClassNameReferenceType(final int id, final String string) {
        super(id, string);
    }
}

@Embedded
@SuppressWarnings({"unused", "WeakerAccess"})
class EmbeddedReferenceType {
    private Integer number;
    private String text;

    EmbeddedReferenceType(final int number, final String text) {
        this.number = number;
        this.text = text;
    }

    EmbeddedReferenceType() {
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddedReferenceType)) {
            return false;
        }

        final EmbeddedReferenceType that = (EmbeddedReferenceType) o;

        if (!Objects.equals(number, that.number)) {
            return false;
        }
        return Objects.equals(text, that.text);

    }

    @Override
    public int hashCode() {
        int result = number != null ? number.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
