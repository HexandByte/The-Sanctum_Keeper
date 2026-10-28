package com.sanctumkeeper.validation;

import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;
import com.sanctumkeeper.model.Action;

public class RuleValidator{

    public static String validate(Rule rule){
        //Checking existence
        if(rule==null)
            return "Rule cannot be null.";
       
        //Check rule ID
        if(rule.getId()==null || rule.getId().isBlank())
            return "Rule ID cannot be empty.";
       
        //Check condition
        Condition condition=rule.getCondition();
        if(condition==null)
            return "Rule condition cannot be null.";
        String type=condition.getType();
        String value=condition.getValue();
     
        //Check condition type
        if(type==null || (!type.equals("extension") && !type.equals("filename")))
            return "Condition value cannot be empty";
        
        //Filename matching validation
        if(type.equals("filename")){
            String matchMode=condition.getMatchMode();
            if(matchMode==null || (!matchMode.equals("startsWith")
            && !matchMode.equals("contains")
            && !matchMode.equals("equals")))
                return "Invalid filename match mode.";
        }

        //Now we check action (I'm tired)
        Action action=rule.getAction();

        if(action==null)
            return "Rule action cannot be null.";
        
        //Cehck if action is move
        if(!"move".equals(action.getType()))
            return "Invalid Action type.";

        //Checking destination next
        String destination=action.getDestination();
        
        if(destination==null || destination.isBlank())
            return "Destination cannot be empty.";

        //Rejecting absolute paths
        java.nio.file.Path destinationPath;

        try{
            destinationPath=java.nio.file.Path.of(destination);
        }    
        catch(java.nio.file.InvalidPathException exception){
            return "Destination path is invalid.";
        }
        if(destinationPath.isAbsolute()){
            return "Destination must be a relative path.";
        }

        //Reject paths that escape selected folder
        if(destinationPath.normalize().startsWith(".."))
            return "Destination must remain inside the selected folder.";

        //Safe input
        return null;
    }
}