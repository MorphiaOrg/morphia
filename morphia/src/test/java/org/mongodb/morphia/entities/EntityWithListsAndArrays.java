package org.mongodb.morphia.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
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
    private ArrayList<String> arrayListOfStrings;
    private ArrayList<Integer> arrayListOfIntegers;
    private Set<Integer> setOfIntegers;
    private String notAnArrayOrList;

    public ArrayList<Integer> getArrayListOfIntegers() {
        return arrayListOfIntegers;
    }

    public void setArrayListOfIntegers(final ArrayList<Integer> arrayListOfIntegers) {
        this.arrayListOfIntegers = arrayListOfIntegers;
    }

    public ArrayList<String> getArrayListOfStrings() {
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
}
