package com.sanctumkeeper.preview;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Rule;
import com.sanctumkeeper.scanner.FolderScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PreviewService{

    private final FolderScanner folderScanner;
    private final RuleMatcher ruleMatcher;

    public PreviewService(){
        this.folderScanner=new FolderScanner();
        this.ruleMatcher=new RuleMatcher();
    }

    public List<PreviewResult> preview(Path sourceFolder, List<Rule> rules) throws IOException{

        //Find eligible files in teh selected folder
        List<Path> files=folderScanner.scan(sourceFolder);
        Path sourceRoot = sourceFolder.toAbsolutePath().normalize();

        //Store one result for every scanned file
        List<PreviewResult> results=new ArrayList<>();

        //Examine each file independently
        for(Path file:files){

            //Find first spell wins
            Rule matchedRule=ruleMatcher.findFirstMatch(file,rules);

            //Record files that have no matching Rule
            if(matchedRule==null){
                results.add(new PreviewResult(file.getFileName().toString(),
                "NO_MATCH",
                null,null,null));

                continue;
            }

            //Get the action specified by the matching rule
            Action action=matchedRule.getAction();

            //Calculate where the file would go
           Path destination = sourceRoot
            .resolve(action.getDestination())
            .normalize();

            // Reject destinations outside the selected source folder.
            if (!destination.startsWith(sourceRoot)) {
            throw new IOException(
                "Rule destination escapes the selected source folder."
            );
}

            //Record the intended operation. DO NOT PERFORM
            results.add(new PreviewResult(file.getFileName().toString(),
            "MATCHED",
            matchedRule.getId(),
            action.getType(),
            destination.toString()));

        }

        return results;

    }

}