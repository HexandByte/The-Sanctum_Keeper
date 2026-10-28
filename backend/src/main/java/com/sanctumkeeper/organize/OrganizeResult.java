package com.sanctumkeeper.organize;

public class OrganizeResult{

    private final String fileName;
    private final String status;
    private final String ruleId;
    private final String action;
    private final String destination;
    private final String message;

    public OrganizeResult(String fileName, String status,
    String ruleId, String action,
    String destination, String message){

        this.fileName=fileName;
        this.status=status;
        this.ruleId=ruleId;
        this.action=action;
        this.destination=destination;
        this.message=message;

    }

     //Getters
    public String getFileName(){
        return fileName;
    }

    public String getStatus(){
        return status;
    }

    public String getRuleId(){
        return ruleId;
    }

    public String getAction(){
        return action;
    }

    public String getDestination(){
        return destination;
    }

    public String getMessage(){
        return message;
    }
}