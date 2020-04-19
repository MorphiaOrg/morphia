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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedField;
import dev.morphia.query.Query;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static dev.morphia.converters.DefaultConverters.JAVA_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Since15")
public class TestSerializedFormat extends TestBase {
    @Test
    @SuppressWarnings("deprecation")
    public void testQueryFormat() {
        Assume.assumeTrue("This test requires Java 8", JAVA_8);
        Query<ReferenceType> query = getDs().find(ReferenceType.class)
                                            .field("id").equal(new ObjectId("000000000000000000000000"))
                                            .field("referenceType").equal(new ReferenceType(2, "far"))
                                            .field("embeddedType").equal(new EmbeddedReferenceType(3, "strikes"))

                                            .field("string").equal("some value")

                                            .field("embeddedArray").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                    .filter("number", 3).filter("text", "strikes"))
                                            .field("embeddedSet").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                    .filter("number", 3).filter("text", "strikes"))
                                            .field("embeddedList").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                    .filter("number", 3).filter("text", "strikes"))

                                            .field("map.bar").equal(new EmbeddedReferenceType(1, "chance"))
                                            .field("mapOfList.bar").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .field("mapOfList.foo").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                     .filter("number", 1)
                                                                                     .filter("text", "chance"))

                                            .field("selfReference").equal(new ReferenceType(1, "blah"))

                                            .field("mixedTypeList").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                     .filter("number", 3).filter("text", "strikes"))
                                            .field("mixedTypeList").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .field("mixedTypeMap.foo").equal(new ReferenceType(3, "strikes"))
                                            .field("mixedTypeMap.bar").equal(new EmbeddedReferenceType(3, "strikes"))
                                            .field("mixedTypeMapOfList.bar").in(singletonList(new EmbeddedReferenceType(1, "chance")))
                                            .field("mixedTypeMapOfList.foo").elemMatch(getDs().find(EmbeddedReferenceType.class)
                                                                                              .filter("number", 3)
                                                                                              .filter("text", "strikes"))

                                            .field("referenceMap.foo").equal(new ReferenceType(1, "chance"))
                                            .field("referenceMap.bar").equal(new EmbeddedReferenceType(1, "chance"));

        DBObject dbObject = query.getQueryObject();
        final DBObject parse = BasicDBObject.parse(readFully("/QueryStructure.json"));
        Assert.assertEquals(parse, dbObject);
    }

    private void verifyCoverage(final DBObject dbObject) {
        for (MappedField field : getMorphia().getMapper().getMappedClass(ReferenceType.class).getPersistenceFields()) {
            String name = field.getNameToStore();
            boolean found = dbObject.containsField(name);
            if (!found) {
                for (String s : dbObject.keySet()) {
                    found |= s.startsWith(name + ".");

                }
            }
            assertTrue("Not found in dbObject: " + name, found);
        }
    }

    @Test
    public void testSavedEntityFormat() {
        Assume.assumeTrue("This test requires Java 8", JAVA_8);

        ReferenceType entity = new ReferenceType(1, "I'm a field value");

        entity.setReferenceType(new ReferenceType(42, "reference"));
        entity.setEmbeddedType(new EmbeddedReferenceType(18, "embedded"));

        entity.setEmbeddedSet(new HashSet<EmbeddedReferenceType>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
                                                                        new EmbeddedReferenceType(1, "Love"))));
        entity.setEmbeddedList(asList(new EmbeddedReferenceType(42, "Douglas Adams"), new EmbeddedReferenceType(1, "Love")));
        entity.setEmbeddedArray(new EmbeddedReferenceType[]{new EmbeddedReferenceType(42, "Douglas Adams"),
            new EmbeddedReferenceType(1, "Love")});

        entity.getMap().put("first", new EmbeddedReferenceType(42, "Douglas Adams"));
        entity.getMap().put("second", new EmbeddedReferenceType(1, "Love"));

        entity.getMapOfList().put("first", asList(new EmbeddedReferenceType(42, "Douglas Adams"), new EmbeddedReferenceType(1, "Love")));
        entity.getMapOfList().put("second", asList(new EmbeddedReferenceType(1, "Love"), new EmbeddedReferenceType(42, "Douglas Adams")));


        entity.getMapOfSet().put("first", new HashSet<EmbeddedReferenceType>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
                                                                                    new EmbeddedReferenceType(1, "Love"))));
        entity.getMapOfSet().put("second", new HashSet<EmbeddedReferenceType>(asList(new EmbeddedReferenceType(42, "Douglas Adams"),
                                                                                     new EmbeddedReferenceType(1, "Love"))));

        entity.setSelfReference(entity);
        entity.setIdOnly(entity);

        entity.setReferenceArray(new ReferenceType[]{new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")});
        entity.setReferenceList(asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")));
        entity.setReferenceSet(new HashSet<ReferenceType>(asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3"))));
        entity.getReferenceMap().put("first", new ReferenceType(2, "text 2"));
        entity.getReferenceMap().put("second", new ReferenceType(3, "text 3"));
        entity.getReferenceMapOfList().put("first", asList(new ReferenceType(2, "text 2"), new ReferenceType(3, "text 3")));
        entity.getReferenceMapOfList().put("second", singletonList(new ReferenceType(3, "text 3")));

        entity.setMixedTypeArray(new ReferenceType[]{new ReferenceType(2, "text 2"), new ClassNameReferenceType(3, "text 3")});
        entity.setMixedTypeList(asList(new ReferenceType(2, "text 2"), new ClassNameReferenceType(3, "text 3")));
        entity.setMixedTypeSet(new HashSet<ReferenceType>(asList(new ReferenceType(2, "text 2"),
                                                                 new ClassNameReferenceType(3, "text 3"))));
        entity.getMixedTypeMap().put("first", new ReferenceType(2, "text 2"));
        entity.getMixedTypeMap().put("second", new ClassNameReferenceType(3, "text 3"));
        entity.getMixedTypeMapOfList().put("first", asList(new ReferenceType(2, "text 2"),
                                                           new ClassNameReferenceType(3, "text 3")));
        entity.getMixedTypeMapOfList().put("second", singletonList(new ClassNameReferenceType(3, "text 3")));

        getDs().save(entity);

        DBObject dbObject = getDs().getCollection(ReferenceType.class).findOne();
        Assert.assertEquals(BasicDBObject.parse(readFully("/ReferenceType.json")), dbObject);
        verifyCoverage(dbObject);
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
    private Map<String, EmbeddedReferenceType> map = new TreeMap<String, EmbeddedReferenceType>();
    private Map<String, List<EmbeddedReferenceType>> mapOfList = new TreeMap<String, List<EmbeddedReferenceType>>();
    private Map<String, Set<EmbeddedReferenceType>> mapOfSet = new TreeMap<String, Set<EmbeddedReferenceType>>();
    @Reference
    private ReferenceType selfReference;
    @Reference(idOnly = true)
    private ReferenceType idOnly;

    private ReferenceType[] referenceArray;
    private Set<ReferenceType> referenceSet;
    private List<ReferenceType> referenceList;
    private Map<String, ReferenceType> referenceMap = new TreeMap<String, ReferenceType>();
    private Map<String, List<ReferenceType>> referenceMapOfList = new TreeMap<String, List<ReferenceType>>();

    private ReferenceType[] mixedTypeArray;
    private Set<? extends ReferenceType> mixedTypeSet;
    private List<? extends ReferenceType> mixedTypeList;
    private Map<String, ? super ReferenceType> mixedTypeMap = new TreeMap<String, ReferenceType>();
    private Map<String, List<? extends ReferenceType>> mixedTypeMapOfList = new TreeMap<String, List<? extends ReferenceType>>();

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
        if (string != null ? !string.equals(that.string) : that.string != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(embeddedArray, that.embeddedArray)) {
            return false;
        }
        if (embeddedSet != null ? !embeddedSet.equals(that.embeddedSet) : that.embeddedSet != null) {
            return false;
        }
        if (embeddedList != null ? !embeddedList.equals(that.embeddedList) : that.embeddedList != null) {
            return false;
        }
        if (map != null ? !map.equals(that.map) : that.map != null) {
            return false;
        }
        if (mapOfList != null ? !mapOfList.equals(that.mapOfList) : that.mapOfList != null) {
            return false;
        }
        if (mapOfSet != null ? !mapOfSet.equals(that.mapOfSet) : that.mapOfSet != null) {
            return false;
        }
        if (selfReference != null ? !selfReference.equals(that.selfReference) : that.selfReference != null) {
            return false;
        }
        if (idOnly != null ? !idOnly.equals(that.idOnly) : that.idOnly != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(referenceArray, that.referenceArray)) {
            return false;
        }
        if (referenceSet != null ? !referenceSet.equals(that.referenceSet) : that.referenceSet != null) {
            return false;
        }
        if (referenceList != null ? !referenceList.equals(that.referenceList) : that.referenceList != null) {
            return false;
        }
        if (referenceMap != null ? !referenceMap.equals(that.referenceMap) : that.referenceMap != null) {
            return false;
        }
        return referenceMapOfList != null ? referenceMapOfList.equals(that.referenceMapOfList) : that.referenceMapOfList == null;

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

        if (number != null ? !number.equals(that.number) : that.number != null) {
            return false;
        }
        return text != null ? text.equals(that.text) : that.text == null;

    }

    @Override
    public int hashCode() {
        int result = number != null ? number.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
