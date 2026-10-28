package com.sanctumkeeper.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.sanctumkeeper.model.Rule;
import com.sanctumkeeper.validation.RuleValidator;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

public class RuleStorage {

    private final Path rulesFile;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public RuleStorage(Path configDirectory) {
        this.rulesFile = configDirectory.resolve("rules.json");
    }

    public List<Rule> loadRules() throws IOException{

        //Start with empty rules if file is missing
        if(Files.notExists(rulesFile))
            return new ArrayList<>();
        
        //Read the existing configuration
        String content=Files.readString(rulesFile);
        JsonElement rootElement;

        try{
            rootElement=JsonParser.parseString(content);
        }
        catch(JsonParseException exception){
            throw new IOException("Rules configuration contain invalid Json. "
            +"The original file was preserved.",exception);
        }

        //Configuration must be a JSON object
        if(!rootElement.isJsonObject()){
            throw new IOException("Rules configuration must be a JSON object. "
            +"The original file was preserved.");
        }

        JsonObject root=rootElement.getAsJsonObject();
        JsonElement rulesElement=root.get("rules");

        //Object must contain a rules array.
        if(rulesElement==null || !rulesElement.isJsonArray()){
            throw new IOException("Rules configuration must contain a 'rules' array. "
            +"The original file was preserved.");
        }

        //Convert JSON array into Java objects
        Type rulesType=new TypeToken<List<Rule>>() {}.getType();
        List<Rule> rules;

        try{
            rules=gson.fromJson(rulesElement, rulesType);
        }
        catch(JsonParseException exception){
            throw new IOException("Could not read the rule definitions. "
            +"The original file was preserved.",exception);
        }

        //Validaye every loaded rule before accepting the configuration
        for(int i=0; i<rules.size(); i++){
            String error=RuleValidator.validate(rules.get(i));
            if(error != null){
                throw new IOException("Invalid rule at position "+(i+1)
                +": "+error+ "The original file was preserved.");
            }
        }

        return rules;
    }

    public void saveRules(List<Rule> rules) throws IOException{
        if(rules==null)
            throw new IOException("Rules list cannot be null.");
        
        //Validate rules before saving
        for(int i=0; i<rules.size(); i++){
            String error=RuleValidator.validate(rules.get(i));
             if(error != null){
                throw new IOException("Cannot save rules. Invalid rule at position "+(i+1)
                +": "+error);
             }
        }

        //Ensure every rule has unique ID
        Set<String> ruleIds=new HashSet<>();
        for(Rule rule:rules){
            if(!ruleIds.add(rule.getId()))
                throw new IOException("Cannot save rules. Duplicate rule ID: "+rule.getId());
        }

        //Convert rules into formatted JSON text
        JsonObject root=new JsonObject();
        root.add("rules",gson.toJsonTree(rules));
        String json=gson.toJson(root);

        //Ensuring configuration directory exists
        Files.createDirectories(rulesFile.getParent());

        //Creating temporary file in the same directory as rules.json
        Path tempFile=Files.createTempFile(rulesFile.getParent(),"rules-",".tmp");

        try{
            //Write the new JSON into the temporary file
            Files.writeString(tempFile, json);

            //Attempt atomic replacement
            try{
                Files.move(tempFile, rulesFile,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
            }
            catch(AtomicMoveNotSupportedException exception){
                //Fall back if atomic isn't supported
                Files.move(tempFile, rulesFile,
                StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally{
            //Remove temporary file if it still exists
            Files.deleteIfExists(tempFile);
        }
    }
}
