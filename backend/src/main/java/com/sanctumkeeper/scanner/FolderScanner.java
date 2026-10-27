package com.sanctumkeeper.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FolderScanner{

    public List<Path> scan(Path sourceFolder) throws IOException {

        //Source folder cannot be null
        if (sourceFolder == null) {
            throw new IOException("Source folder cannot be null.");
        }

        //Get absolute path
        Path folder = sourceFolder.toAbsolutePath().normalize();

        if (!Files.isDirectory(folder)) {
            throw new IOException("Source path is not a directory.");
        }

        List<Path> files = new java.util.ArrayList<>();

        try (Stream<Path> entries = Files.list(folder)) {
            for (Path path : entries.toList()) {

                // Skip directories and other non-regular files.
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                // Check hidden status; IOException reaches the caller.
                if (Files.isHidden(path)) {
                continue;
                }

                files.add(path);
            }
         }

        // Keep the results in a predictable order.
        files.sort(Path::compareTo);

        return files;
    }
}