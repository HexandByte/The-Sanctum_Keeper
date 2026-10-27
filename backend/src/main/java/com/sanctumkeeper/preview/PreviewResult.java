package com.sanctumkeeper.preview;

public class PreviewResult{

    private final String fileName;
    private final String status;
    private final String ruleId;
    private final String action;
    private final String destination;

    public PreviewResult(String fileName, String status, String ruleId,
        String action, String destination){

            this.fileName=fileName;
            this.status=status;
            this.ruleId=ruleId;
            this.action=action;
            this.destination=destination;

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

}