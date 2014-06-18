package org.mongodb.morphia.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class EntityWithListsAndArrays {
    private String[] arrayOfStrings;
    private int[] arrayOfInts;
    private List<String> listOfStrings;
    private List<Integer> listOfIntegers;
    private ArrayList<String> arrayListOfStrings;
    private ArrayList<Integer> arrayListOfIntegers;
    private Set<Integer> setOfIntegers;
    private String notAnArrayOrList;
}
