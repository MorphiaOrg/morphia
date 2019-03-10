package dev.morphia.entities;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public List<Integer> getArrayListOfIntegers() {
        return arrayListOfIntegers;
    }

    public void setArrayListOfIntegers(final ArrayList<Integer> arrayListOfIntegers) {
        this.arrayListOfIntegers = arrayListOfIntegers;
    }

    public List<String> getArrayListOfStrings() {
        return arrayListOfStrings;
    }

    public void setArrayListOfStrings(final ArrayList<String> arrayListOfStrings) {
        this.arrayListOfStrings = arrayListOfStrings;
    }

    public int[] getArrayOfInts() {
        return arrayOfInts;
    }

    public void setArrayOfInts(final int[] arrayOfInts) {
        this.arrayOfInts = arrayOfInts;
    }

    public String[] getArrayOfStrings() {
        return arrayOfStrings;
    }

    public void setArrayOfStrings(final String[] arrayOfStrings) {
        this.arrayOfStrings = arrayOfStrings;
    }

    public List<Integer> getListOfIntegers() {
        return listOfIntegers;
    }

    public void setListOfIntegers(final List<Integer> listOfIntegers) {
        this.listOfIntegers = listOfIntegers;
    }

    public List<String> getListOfStrings() {
        return listOfStrings;
    }

    public void setListOfStrings(final List<String> listOfStrings) {
        this.listOfStrings = listOfStrings;
    }

    public String getNotAnArrayOrList() {
        return notAnArrayOrList;
    }

    public void setNotAnArrayOrList(final String notAnArrayOrList) {
        this.notAnArrayOrList = notAnArrayOrList;
    }

    public Set<Integer> getSetOfIntegers() {
        return setOfIntegers;
    }

    public void setSetOfIntegers(final Set<Integer> setOfIntegers) {
        this.setOfIntegers = setOfIntegers;
    }

    public List<EmbeddedType> getListEmbeddedType() {
        return listEmbeddedType;
    }

    public void setListEmbeddedType(final List<EmbeddedType> listEmbeddedType) {
        this.listEmbeddedType = listEmbeddedType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityWithListsAndArrays)) {
            return false;
        }

        final EntityWithListsAndArrays that = (EntityWithListsAndArrays) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(arrayOfStrings, that.arrayOfStrings)) {
            return false;
        }
        if (!Arrays.equals(arrayOfInts, that.arrayOfInts)) {
            return false;
        }
        if (listOfStrings != null ? !listOfStrings.equals(that.listOfStrings) : that.listOfStrings != null) {
            return false;
        }
        if (listOfIntegers != null ? !listOfIntegers.equals(that.listOfIntegers) : that.listOfIntegers != null) {
            return false;
        }
        if (arrayListOfStrings != null ? !arrayListOfStrings.equals(that.arrayListOfStrings) : that.arrayListOfStrings != null) {
            return false;
        }
        if (arrayListOfIntegers != null ? !arrayListOfIntegers.equals(that.arrayListOfIntegers) : that.arrayListOfIntegers != null) {
            return false;
        }
        if (listEmbeddedType != null ? !listEmbeddedType.equals(that.listEmbeddedType) : that.listEmbeddedType != null) {
            return false;
        }
        if (setOfIntegers != null ? !setOfIntegers.equals(that.setOfIntegers) : that.setOfIntegers != null) {
            return false;
        }
        return notAnArrayOrList != null ? notAnArrayOrList.equals(that.notAnArrayOrList) : that.notAnArrayOrList == null;

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
