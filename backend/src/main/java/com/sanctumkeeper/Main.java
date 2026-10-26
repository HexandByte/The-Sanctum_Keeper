package com.sanctumkeeper;

import com.sanctumkeeper.storage.RuleStorage;
import com.sanctumkeeper.model.Rule;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.JsonParseException;

import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sanctumkeeper.preview.PreviewResult;
import com.sanctumkeeper.preview.PreviewService;
import com.sanctumkeeper.organize.OrganizeResult;
import com.sanctumkeeper.organize.OrganizeService;

public class Main{

    private static final Gson GSON = new Gson();

    //Shared storage instance for the entire engine
    private static RuleStorage ruleStorage;

    //Shared organize instance for the entire engine
    private static OrganizeService organizeService;

    public static void main(String[] args) throws IOException{

        //Electron must provide the configuration directory
        if(args.length != 1 || args[0].isBlank()){
            System.err.println("Missing configuration directory argument.");
            return;
        }

        //Initialize rule storage using Electron's directory path
        try{
            ruleStorage=new RuleStorage(Path.of(args[0]));
        }
        catch(InvalidPathException exception){
            System.err.println("Invalid configuration directory path.");
            return;
        }

        //Initialize organise service
        organizeService=new OrganizeService();

        //Here we tell Electron that Engine has started
        JsonObject ready = new JsonObject();
        ready.addProperty("type","ready");
        ready.addProperty("protocolVersion",1);
        writeJson(ready);

        //Below code will read one JSON request per line (hopefully)
        try(BufferedReader reader = new BufferedReader(new InputStreamReader
                                    (System.in, StandardCharsets.UTF_8)))
        {
            String line;
            while((line = reader.readLine()) != null){
                if(line.isBlank()){
                    continue;
                }
                handleRequest(line);
            }
        }
    }

    private static void handleRequest(String line){

        JsonElement parsed;

        // Preventing engine crash from invalid JSON
       try (JsonReader jsonReader = new JsonReader(new StringReader(line))) {
    jsonReader.setStrictness(Strictness.STRICT);
    parsed = JsonParser.parseReader(jsonReader);

            if (jsonReader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                sendError(
                    JsonNull.INSTANCE,
                    "INVALID_JSON",
                    "Request must contain exactly one JSON value."
                );
                return;
            }
        }
        catch (JsonSyntaxException | IOException exception) {
            sendError(
                JsonNull.INSTANCE,
                "INVALID_JSON",
                "Request is not valid JSON."
            );
            return;
        }
        if(!parsed.isJsonObject()){
            sendError(
                JsonNull.INSTANCE,
                "INVALID_REQUEST",
                "Request must be a JSON Object"
            );
            return;
        }

        JsonObject request=parsed.getAsJsonObject();
        JsonElement id=request.has("id")?request.get("id"):JsonNull.INSTANCE;

        if (id.isJsonNull() || !id.isJsonPrimitive()) {
    sendError(
             JsonNull.INSTANCE,
             "INVALID_REQUEST",
            "Request must contain a string or numeric id."
        );
        return;
        }

        if (id.getAsJsonPrimitive().isBoolean()) {
            sendError(
                JsonNull.INSTANCE,
                "INVALID_REQUEST",
                "Request must contain a string or numeric id."
            );
            return;
        }

        JsonElement commandElement=request.get("command");

        if(commandElement==null || !commandElement.isJsonPrimitive() ||
            !commandElement.getAsJsonPrimitive().isString()){
                sendError(
                    id,
                    "INVALID_REQUEST",
                    "Request must contain a string command."
                );
                return;
            }

        String command=commandElement.getAsString();

        switch(command){
            case "getRules" -> {
                try{
                    //Load the saved rules from the configuration file
                    List<Rule> rules=ruleStorage.loadRules();
                    //Convert the rules into a JSON array
                    JsonObject data=new JsonObject();
                    data.add("rules",GSON.toJsonTree(rules));
                    //Send the rules back to Electron
                    sendSuccess(id,data);
                }
                catch(IOException exception){
                    sendError(id, "RULES_LOAD_FAILED", exception.getMessage());
                }
            }
            case "saveRules" -> {
                //Check that the request contains a rules array
                JsonElement rulesElement=request.get("rules");
                if(rulesElement==null || !rulesElement.isJsonArray()){
                    sendError(id,"INVALID_REQUEST","Request must contain a rules array.");
                    break;
                }
                try{
                    //Convert JSON array into Java Rule object
                    List<Rule> rules=GSON.fromJson(rulesElement,
                    new com.google.gson.reflect.TypeToken<List<Rule>>() {}.getType());

                    //Validate and save rules to rule.json
                    ruleStorage.saveRules(rules);

                    //Confirmation test
                    JsonObject data=new JsonObject();
                    data.addProperty("savedCount",rules.size());
                    sendSuccess(id,data);                
                }
                catch(IOException exception){
                    sendError(id, "RULES_SAVE_FAILED",exception.getMessage());
                }
                catch(com.google.gson.JsonParseException exception){
                    sendError(id,"INVALID_REQUEST","Rules contain invalid data.");
                }
            }
            case "preview" -> {
                // Check that the request contains a non-empty source folder path
                JsonElement sourceFolderElement=request.get("sourceFolder");

                if(sourceFolderElement==null || !sourceFolderElement.isJsonPrimitive()
                || !sourceFolderElement.getAsJsonPrimitive().isString()
                || sourceFolderElement.getAsString().isBlank()){
                    sendError(id, "INVALID_REQUEST",
                    "Request must contain a non-empty SourceFolder string");
                    return;
                }

                //Convert supplied string into a filesystem path
                Path sourceFolder;
                try{
                    sourceFolder=Path.of(sourceFolderElement.getAsString());
                }
                catch(InvalidPathException exception){
                    sendError(id, "INVALID_SOURCE_FOLDER",
                    "Source folder path is invalid.");
                    return;
                }

                //Load the saved rules
                List<Rule> rules;
                try{
                    rules=ruleStorage.loadRules();
                }
                catch(IOException exception){
                    sendError(id, "RULES_LOAD_FAILED", exception.getMessage());
                    return;
                }

                //Call PreviewService clas for file evaluation
                List<PreviewResult> results;
                try{
                    PreviewService previewService= new PreviewService();
                    results=previewService.preview(sourceFolder,rules);
                }
                catch(IOException exception){
                    sendError(id, "PREVIEW_FAILED", exception.getMessage());
                    return;
                }

                //Convert results into JSON array for Electron
                JsonArray resultsArray=new JsonArray();
                int matchedCount=0;
                int unmatchedCount=0;
                for(PreviewResult result: results){
                    JsonObject resultJson=new JsonObject();
                    resultJson.addProperty("fileName",result.getFileName());
                    resultJson.addProperty("status",result.getStatus());
                    resultJson.addProperty("ruleId",result.getRuleId());
                    resultJson.addProperty("action",result.getAction());
                    resultJson.addProperty("destination",result.getDestination());

                    resultsArray.add(resultJson);

                    if("MATCHED".equals(result.getStatus()))
                        matchedCount++;
                    else if("NO_MATCH".equals(result.getStatus()))
                        unmatchedCount++;
                }

                //Prepare summary of the preview
                JsonObject summary=new JsonObject();
                summary.addProperty("totalFiles",results.size());
                summary.addProperty("matchedFiles",matchedCount);
                summary.addProperty("unmatchedFiles",unmatchedCount);

                //Combine results and summary into response data
                JsonObject data=new JsonObject();
                data.add("results",resultsArray);
                data.add("summary",summary);
                sendSuccess(id,data);
            }
            case "organize" -> {
                    // Check that the request contains a non-empty source folder path
                JsonElement sourceFolderElement=request.get("sourceFolder");

                if(sourceFolderElement==null || !sourceFolderElement.isJsonPrimitive()
                || !sourceFolderElement.getAsJsonPrimitive().isString()
                || sourceFolderElement.getAsString().isBlank()){
                    sendError(id, "INVALID_REQUEST",
                    "Request must contain a non-empty SourceFolder string");
                    return;
                }

                //Convert supplied string into a filesystem path
                Path sourceFolder;
                try{
                    sourceFolder=Path.of(sourceFolderElement.getAsString());
                }
                catch(InvalidPathException exception){
                    sendError(id, "INVALID_SOURCE_FOLDER",
                    "Source folder path is invalid.");
                    return;
                }

                //Load the saved rules
                List<Rule> rules;
                try{
                    rules=ruleStorage.loadRules();
                }
                catch(IOException exception){
                    sendError(id, "RULES_LOAD_FAILED", exception.getMessage());
                    return;
                }

                //Perform file operations
                List<OrganizeResult> results;
                try{
                    results=organizeService.organize(sourceFolder,rules);
                }
                catch(IOException exception){
                    sendError(id, "ORGANIZE_FAILED", exception.getMessage());
                    return;
                }

                //Convert the per-file results into JSON
                JsonArray resultsArray=new JsonArray();
                int organizedCount=0;
                int unmatchedCount=0;
                int skippedCount=0;
                int failedCount=0;

                for(OrganizeResult result:results){
                    JsonObject resultJson=new JsonObject();
                    resultJson.addProperty("fileName",result.getFileName());
                    resultJson.addProperty("status",result.getStatus());
                    resultJson.addProperty("ruleId",result.getRuleId());
                    resultJson.addProperty("action",result.getAction());
                    resultJson.addProperty("destination",result.getDestination());
                    resultJson.addProperty("message",result.getMessage());

                    resultsArray.add(resultJson);

                    switch(result.getStatus()){
                        case "ORGANIZED" -> organizedCount++;
                        case "NO_MATCH" -> unmatchedCount++;
                        case "SKIPPED_CONFLICT" -> skippedCount++;
                        case "FAILED" -> failedCount++;
                    }
                }

                //Prepare summary for Electron
                JsonObject summary = new JsonObject();
                summary.addProperty("totalFiles", results.size());
                summary.addProperty("organizedFiles", organizedCount);
                summary.addProperty("unmatchedFiles", unmatchedCount);
                summary.addProperty("skippedFiles", skippedCount);
                summary.addProperty("failedFiles", failedCount);

                JsonObject data = new JsonObject();
                data.add("results", resultsArray);
                data.add("summary", summary);

                 sendSuccess(id, data);
            }
           case "undo" -> {

                // Attempt to undo exactly one file
                List<OrganizeResult> results=organizeService.undo();

                // Convert the result into a JSON array
                JsonArray resultsArray=new JsonArray();

                int restoredCount=0;
                int conflictCount=0;
                int missingCount=0;
                int failedCount=0;

                for(OrganizeResult result:results){

                    JsonObject resultJson=new JsonObject();

                    resultJson.addProperty("fileName",result.getFileName());
                    resultJson.addProperty("status",result.getStatus());
                    resultJson.addProperty("ruleId",result.getRuleId());
                    resultJson.addProperty("action",result.getAction());
                    resultJson.addProperty("destination",result.getDestination());
                    resultJson.addProperty("message",result.getMessage());

                    resultsArray.add(resultJson);

                    switch(result.getStatus()){
                        case "RESTORED" -> restoredCount++;
                        case "SKIPPED_CONFLICT" -> conflictCount++;
                        case "MISSING" -> missingCount++;
                        case "FAILED" -> failedCount++;
                    }
                }

                // Prepare the summary
                JsonObject summary=new JsonObject();

                summary.addProperty("restoredFiles",restoredCount);
                summary.addProperty("conflicts",conflictCount);
                summary.addProperty("missingFiles",missingCount);
                summary.addProperty("failedFiles",failedCount);

                // Prepare the response data
                JsonObject data=new JsonObject();

                data.add("results",resultsArray);
                data.add("summary",summary);

                if(results.isEmpty()){
                data.addProperty("message","Nothing to undo.");
                }

                sendSuccess(id,data);
            }
            default -> sendError(
                id,
                "UNKNOWN_COMMAND",
                "Unknown command: "+command
            );
        }
    }

    private static void sendSuccess(JsonElement id,JsonObject data){
        JsonObject response=new JsonObject();
        response.add("id",id.deepCopy());
        response.addProperty("ok",true);
        response.add("data", data);
        writeJson(response);
    }

    private static void sendError(JsonElement id, String code, String message){
        JsonObject error=new JsonObject();
        error.addProperty("code",code);
        error.addProperty("message",message);

        JsonObject response=new JsonObject();
        response.add("id",id);
        response.addProperty("ok",false);
        response.add("error",error);

        writeJson(response);
    }

    //Kicking out non-JSON messages

    private static void writeJson(JsonElement message){
        System.out.println(GSON.toJson(message));
        System.out.flush();
    }
}