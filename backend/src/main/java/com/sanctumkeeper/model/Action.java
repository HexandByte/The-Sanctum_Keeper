package com.sanctumkeeper.model;

public class Action{
    private String type;
    private String destination;

    public Action(){
    }

    //Constructor to create Action manually
    public Action(String type, String destination){
        this.type=type;
        this.destination=destination;
    }

    //Getters
    public String getType(){
        return type;
    }
    public String getDestination(){
        return destination;
    }
}