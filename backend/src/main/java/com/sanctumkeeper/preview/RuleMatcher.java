package com.sanctumkeeper.preview;

import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class RuleMatcher{

    public Rule findFirstMatch(Path file, List<Rule> rules){
        //Nothing can match if the file or rule list is missing
        if(file==null || file.getFileName()==null || rules==null)
            return null;
        
        //Extract the filename, excluding its extension when needed
        String fileName=file.getFileName().toString();
        String baseName=getBaseName(fileName);
        String extension=getExtension(fileName);

        //Examine rules in their original order
        for(Rule rule:rules){
            if(rule==null || rule.getCondition()==null)
                continue;
            Condition condition=rule.getCondition();

            if(matches(condition, baseName, extension)){
                //First Spell Wins: stop at first match
                return rule;
            }
        }

        //No rule matched this file
        return null;

    }

    private boolean matches(Condition condition, String baseName, String extension){

        String type=condition.getType();
        String value=condition.getValue();

        if(type==null || value==null || value.isBlank())
            return false;

        //Extension matching is case-insensitive
        if(type.equals("extension")){
            return extension.equalsIgnoreCase(value);
        }

        //Filename conditions use the name without its extension
        if(type.equals("filename")){
            String name=baseName.toLowerCase(Locale.ROOT);
            String target=value.toLowerCase(Locale.ROOT);
            String matchMode=condition.getMatchMode();

            if(matchMode==null)
                return false;
            
            return switch(matchMode){
                case "startsWith" -> name.startsWith(target);
                case "contains" -> name.contains(target);
                case "equals" -> name.equals(target);
                default -> false;
            };
        }

        //Unknown condition types never match
        return false;
    }

    private String getBaseName(String fileName){

        int dotIndex=fileName.lastIndexOf('.');

        //No extension or a file name beginning with a dot
        if(dotIndex<=0)
            return fileName;

        return fileName.substring(0,dotIndex);

    }

    private String getExtension(String fileName){

        int dotIndex=fileName.lastIndexOf('.');

        //No extension or trailing dot
        if(dotIndex<=0 || dotIndex==fileName.length()-1)
            return "";
        
        return fileName.substring(dotIndex+1);
    }

}