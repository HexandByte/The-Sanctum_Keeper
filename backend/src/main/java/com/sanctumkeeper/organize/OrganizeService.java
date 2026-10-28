package com.sanctumkeeper.organize;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Rule;
import com.sanctumkeeper.preview.RuleMatcher;
import com.sanctumkeeper.scanner.FolderScanner;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class OrganizeService{

    private final FolderScanner folderScanner;
    private final RuleMatcher ruleMatcher;

    private List<MoveRecord> lastSuccessfulMoves=List.of();
    private Path lastSourceRoot;

    public OrganizeService(){
        this.folderScanner=new FolderScanner();
        this.ruleMatcher=new RuleMatcher();
    }

    public List<OrganizeResult> organize(Path sourceFolder,
    List<Rule> rules) throws IOException{

        if(sourceFolder==null){
            throw new IOException("Source folder cannot be null.");
        }

        Path sourceRoot=sourceFolder.toAbsolutePath().normalize();

        //Check for symbolic link
        if(Files.isSymbolicLink(sourceRoot)){
            throw new IOException("Organizing a symbolic-link source folder is not supported.");
        }

        //Scan before processing. Stop if error occurs
        List<Path> files=folderScanner.scan(sourceRoot);

        List<OrganizeResult> results=new ArrayList<>();
        List<MoveRecord> currentMoves=new ArrayList<>();

        for(Path file:files){

            Rule matchedRule=ruleMatcher.findFirstMatch(file,rules);

            //No matching rule case
            if(matchedRule==null){
                results.add(new OrganizeResult(file.getFileName().toString(),
                "NO_MATCH",
                null,null,null,null));
                continue;
            }

            Action action=matchedRule.getAction();

            //Checking if action is empty
            if(action==null){
                 results.add(new OrganizeResult(file.getFileName().toString(),
                "FAILED",
                null,null,null,
                "The matched rule has no action."));
                continue;
            }

            Path targetFile=null;

            try{
                //Don't move symbolic-link files
                if(Files.isSymbolicLink(file)){
                    throw new IOException("Symbolic link files are not supported.");
                }
                //Only move is allowed
                if(!"move".equals(action.getType())){
                    throw new IOException("Unsupported action type.");
                }

                //Resolve and validate destination directory
                Path destinationDirectory= resolveSafeDestination(sourceRoot,
                action.getDestination());

                //Keep the original filename at the destination
                targetFile=destinationDirectory.resolve(file.getFileName()).normalize();

                //Complete destination must remain inside the root
                if(!targetFile.startsWith(sourceRoot)){
                    throw new IOException("Destination escapes the selected folder.");
                }

                //Check destination paths before creating directories
                ensureNoSymbolicLinkComponents(sourceRoot,destinationDirectory);

                //Never overwrite or rename an existing destination file
                if(Files.exists(targetFile,LinkOption.NOFOLLOW_LINKS)){
                    results.add(createConflictResult(file, matchedRule, action, targetFile));
                    continue;
                }

                //Create destination directory when its missing
                if (Files.exists(destinationDirectory, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(destinationDirectory, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                "The destination path exists but is not a directory."
                 );
                }

                Files.createDirectories(destinationDirectory);

                //Check again after creation (I'm paranoid ok)
                ensureNoSymbolicLinkComponents(sourceRoot,destinationDirectory);

                //Recheck in case the destination appeared in the meantime
                if(Files.exists(targetFile,LinkOption.NOFOLLOW_LINKS)){
                    results.add(createConflictResult(file,matchedRule,action,targetFile));
                    continue;
                }

                //Check for existing file (Moving case)
                try{
                    Files.move(file,targetFile);
                }
                catch(FileAlreadyExistsException exception){
                    results.add(createConflictResult(file,matchedRule,action,targetFile));
                    continue;
                }

                //Record the move only after Files.move succeeds
                currentMoves.add(new MoveRecord(file.toAbsolutePath().normalize(),
                targetFile.toAbsolutePath().normalize()));

                results.add(new OrganizeResult(file.getFileName().toString(),
                "ORGANIZED",
                matchedRule.getId(),
                action.getType(),
                targetFile.toString(),
                null));
            }
            catch(IOException | SecurityException exception){
                String message=exception.getMessage();
                if(message==null || message.isBlank())
                    message="The file operation failed.";
                results.add(new OrganizeResult(file.getFileName().toString(),
                "FAILED",
                matchedRule.getId(),
                action.getType(),
                targetFile==null?null:targetFile.toString(),
                message));
            }

        }

        //Only this Organize operation's successful moves can be undone
        lastSuccessfulMoves=List.copyOf(currentMoves);
        lastSourceRoot=currentMoves.isEmpty()?null:sourceRoot;

        return results;

    }

    private Path resolveSafeDestination(Path sourceRoot,
    String relativeDestination) throws IOException{

        if(relativeDestination==null || relativeDestination.isBlank()){
            throw new IOException("Destination cannot be empty.");
        }

        Path relativePath;

        try{
            relativePath=Path.of(relativeDestination);
        }
        catch(InvalidPathException exception){
            throw new IOException("Destination path is inavlid",exception);
        }

        if(relativePath.isAbsolute()){
            throw new IOException("Destination must be relative to the source folder.");
        }

        Path destination=sourceRoot.resolve(relativePath).normalize();

        if(!destination.startsWith(sourceRoot)){
            throw new IOException("Destination escapes the selected source folder.");
        }

        return destination;

    }

    private void ensureNoSymbolicLinkComponents(Path sourceRoot,
    Path destinationDirectory) throws IOException{

        if(Files.isSymbolicLink(sourceRoot)){
            throw new IOException("The source folder cannot be a symbolic link.");
        }

        Path relativePath=sourceRoot.relativize(destinationDirectory);
        Path currentPath=sourceRoot;

        for(Path component: relativePath){
            currentPath=currentPath.resolve(component);

            if(Files.isSymbolicLink(currentPath)){
                throw new IOException("Destination cannot pass through a symbolic link.");
            }
        }

    }

    private OrganizeResult createUndoResult(Path originalPath, String status,
    String message){

        return new OrganizeResult(originalPath.getFileName().toString(),
        status,
        null,
        "undo",
        originalPath.toString(),
        message);

    }
    private OrganizeResult createConflictResult(Path file, Rule rule,
    Action action, Path targetFile){

        return new OrganizeResult(file.getFileName().toString(),
        "SKIPPED_CONFLICT",
        rule.getId(),
        action.getType(),
        targetFile.toString(),
        "A file with this name already exists at the destination.");

    }

    public List<OrganizeResult> undo(){

        //Nothing is available to undo
        if(lastSuccessfulMoves.isEmpty() || lastSourceRoot==null){
            return List.of();
        }

        //Get only the most recently moved file
        int lastIndex=lastSuccessfulMoves.size()-1;
        MoveRecord move=lastSuccessfulMoves.get(lastIndex);
        Path originalPath=move.getOriginalPath();
        Path movedPath=move.getMovedPath();

        try{
            //Check if both paths must remain inside the original source folder
            if(!originalPath.startsWith(lastSourceRoot) ||
            !movedPath.startsWith(lastSourceRoot)){
                throw new IOException("Recorded move path is outside the source folder.");
            }

            Path originalParent=originalPath.getParent();
            Path movedParent=movedPath.getParent();
            if( originalParent==null || movedParent==null){
                throw new IOException("Recorded path file has no parent directory.");
            }

            //Reject symbolic links in parent directory path
            ensureNoSymbolicLinkComponents(lastSourceRoot,movedParent);
            ensureNoSymbolicLinkComponents(lastSourceRoot,originalParent);

            //Check whether the moved file still exists
            if(!Files.exists(movedPath,LinkOption.NOFOLLOW_LINKS)){
                return List.of(createUndoResult(originalPath,
                "MISSING",
                "The organized file no longer exists at its destination."));
            }

            //Never restore a symbolic-link file
            if(Files.isSymbolicLink(movedPath)){
                throw new IOException("Cannot restore a symbolic-link file.");
            }

            //Never overwritten an existing path
            if(Files.exists(originalPath, LinkOption.NOFOLLOW_LINKS)){
                return List.of(createUndoResult(originalPath,
                "SKIPPED_CONFLICT",
                "The original location already contains a file or another path entry."));
            }

            //Ensure the original parent is a directory
            if(Files.exists(originalParent,LinkOption.NOFOLLOW_LINKS)
            && !Files.isDirectory(originalParent,LinkOption.NOFOLLOW_LINKS)){
                throw new IOException("The original parent path exists but is not a directory.");
            }

            //Recreate the original directory if necessary
            Files.createDirectories(originalParent);

            //Check again after directory creation
            ensureNoSymbolicLinkComponents(lastSourceRoot,originalParent);

            //Recheck immediately before moving
            if(!Files.exists(movedPath,LinkOption.NOFOLLOW_LINKS)){
                return List.of(createUndoResult(originalPath,
                "MISSING",
                "The organized file no longer exists at its destination."));
            }

            if(Files.isSymbolicLink(movedPath)){
                throw new IOException("cannot restore a symbolic-link file.");
            }

            if(Files.exists(originalPath,LinkOption.NOFOLLOW_LINKS)){
                return List.of(createUndoResult(originalPath,
                "SKIPPED_CONFLICT",
                "The original location became occupied during restoration."));
            }

            //Move back without requesting replacement
            try{
                Files.move(movedPath,originalPath);
            }
            catch(FileAlreadyExistsException exception){
                return List.of(createUndoResult(originalPath,
                "SKIPPED_CONFLICT",
                "The original location became occupied during restoration."));
            }

            //Remove this move only after successful restoration
            lastSuccessfulMoves=List.copyOf(lastSuccessfulMoves.subList(0,lastIndex));

            if(lastSuccessfulMoves.isEmpty()){
                lastSourceRoot=null;
            }
            return List.of(createUndoResult(originalPath,
            "RESTORED",
            "File restored to its original location."));
        }
        catch(IOException | SecurityException exception){
            String message=exception.getMessage();
            if(message==null || message.isBlank())
                message="The file could not be restored.";
            return List.of(createUndoResult(originalPath,
            "FAILED",
            message));
        }
    }

    public List<MoveRecord> getLastSuccessfulMoves(){
        return List.copyOf(lastSuccessfulMoves);
    }

}