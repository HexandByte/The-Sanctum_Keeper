package com.sanctumkeeper.model;

public class Condition{
    private String type;
    private String matchMode;
    private String value;

    //No argument constructor for Gson
    public Condition(){
    }

    //Constructor to create a Condition manually
    public Condition(String type, String matchMode, String value){
        this.type=type;
        this.matchMode=matchMode;
        this.value=value;
    }

    //Getters (NOT SETTERS)
    public String getType(){
        return type;
    }
    public String getMatchMode(){
        return matchMode;
    }
    public String getValue(){
        return value;
    }
}