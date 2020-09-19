package dev.morphia.entities;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@SuppressWarnings("unused")
public class EntityWithListsAndArrays {
    @Id
    private ObjectId id;
    private String[] arrayOfStrings;
    private int[] arrayOfInts;
    private List<String> listOfStrings;
    private List<Integer> listOfIntegers;
    private List<String> arrayListOfStrings;
    private List<Integer> arrayListOfIntegers;
    private List<EmbeddedType> listEmbeddedType;
    private Set<Integer> setOfIntegers;
    private String notAnArrayOrList;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<Integer> getArrayListOfIntegers() {
        return arrayListOfIntegers;
    }

    public void setArrayListOfIntegers(ArrayList<Integer> arrayListOfIntegers) {
        this.arrayListOfIntegers = arrayListOfIntegers;
    }

    public List<String> getArrayListOfStrings() {
        return arrayListOfStrings;
    }

    public void setArrayListOfStrings(ArrayList<String> arrayListOfStrings) {
        this.arrayListOfStrings = arrayListOfStrings;
    }

    public int[] getArrayOfInts() {
        return arrayOfInts;
    }

    public void setArrayOfInts(int[] arrayOfInts) {
        this.arrayOfInts = arrayOfInts;
    }

    public String[] getArrayOfStrings() {
        return arrayOfStrings;
    }

    public void setArrayOfStrings(String[] arrayOfStrings) {
        this.arrayOfStrings = arrayOfStrings;
    }

    public List<Integer> getListOfIntegers() {
        return listOfIntegers;
    }

    public void setListOfIntegers(List<Integer> listOfIntegers) {
        this.listOfIntegers = listOfIntegers;
    }

    public List<String> getListOfStrings() {
        return listOfStrings;
    }

    public void setListOfStrings(List<String> listOfStrings) {
        this.listOfStrings = listOfStrings;
    }

    public String getNotAnArrayOrList() {
        return notAnArrayOrList;
    }

    public void setNotAnArrayOrList(String notAnArrayOrList) {
        this.notAnArrayOrList = notAnArrayOrList;
    }

    public Set<Integer> getSetOfIntegers() {
        return setOfIntegers;
    }

    public void setSetOfIntegers(Set<Integer> setOfIntegers) {
        this.setOfIntegers = setOfIntegers;
    }

    public List<EmbeddedType> getListEmbeddedType() {
        return listEmbeddedType;
    }

    public void setListEmbeddedType(List<EmbeddedType> listEmbeddedType) {
        this.listEmbeddedType = listEmbeddedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityWithListsAndArrays)) {
            return false;
        }

        final EntityWithListsAndArrays that = (EntityWithListsAndArrays) o;

        if (!Objects.equals(id, that.id)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(arrayOfStrings, that.arrayOfStrings)) {
            return false;
        }
        if (!Arrays.equals(arrayOfInts, that.arrayOfInts)) {
            return false;
        }
        if (!Objects.equals(listOfStrings, that.listOfStrings)) {
            return false;
        }
        if (!Objects.equals(listOfIntegers, that.listOfIntegers)) {
            return false;
        }
        if (!Objects.equals(arrayListOfStrings, that.arrayListOfStrings)) {
            return false;
        }
        if (!Objects.equals(arrayListOfIntegers, that.arrayListOfIntegers)) {
            return false;
        }
        if (!Objects.equals(listEmbeddedType, that.listEmbeddedType)) {
            return false;
        }
        if (!Objects.equals(setOfIntegers, that.setOfIntegers)) {
            return false;
        }
        return Objects.equals(notAnArrayOrList, that.notAnArrayOrList);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(arrayOfStrings);
        result = 31 * result + Arrays.hashCode(arrayOfInts);
        result = 31 * result + (listOfStrings != null ? listOfStrings.hashCode() : 0);
        result = 31 * result + (listOfIntegers != null ? listOfIntegers.hashCode() : 0);
        result = 31 * result + (arrayListOfStrings != null ? arrayListOfStrings.hashCode() : 0);
        result = 31 * result + (arrayListOfIntegers != null ? arrayListOfIntegers.hashCode() : 0);
        result = 31 * result + (listEmbeddedType != null ? listEmbeddedType.hashCode() : 0);
        result = 31 * result + (setOfIntegers != null ? setOfIntegers.hashCode() : 0);
        result = 31 * result + (notAnArrayOrList != null ? notAnArrayOrList.hashCode() : 0);
        return result;
    }
}
