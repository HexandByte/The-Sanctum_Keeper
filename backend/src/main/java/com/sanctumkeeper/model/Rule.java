package com.sanctumkeeper.model;

public class Rule{
    private String id;
    private Condition condition;
    private Action action;

    public Rule(){}

    //Constructor to create a Rule manually
    public Rule(String id, Condition condition, Action action){
        this.id=id;
        this.condition=condition;
        this.action=action;
    }

    //Getters
    public String getId(){
        return id;
    }
    public Condition getCondition(){
        return condition;
    }
    public Action getAction(){
        return action;
    }
}