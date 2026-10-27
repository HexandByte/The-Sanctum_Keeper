package com.sanctumkeeper.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolderScannerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void scansDirectFilesWithoutEnteringSubfolders() throws IOException {

        // Arrange: create a file and a subfolder containing another file.
        Files.createFile(tempDirectory.resolve("Report.pdf"));

        Path subfolder = Files.createDirectory(
            tempDirectory.resolve("Documents")
        );

        Files.createFile(subfolder.resolve("Secret.pdf"));

        FolderScanner scanner = new FolderScanner();

        // Act: scan the temporary directory.
        List<Path> result = scanner.scan(tempDirectory);

        // Assert: only the direct file should be returned.
        assertEquals(1, result.size());
        assertEquals(
            "Report.pdf",
            result.get(0).getFileName().toString()
        );

        assertEquals(
            false,
            result.stream().anyMatch(
                path -> path.getFileName().toString().equals("Documents")
            )
        );
    }
    @Test
void rejectsPathThatIsNotADirectory() throws IOException {

    // Arrange: create a regular file, not a folder.
    Path file = Files.createFile(
        tempDirectory.resolve("Report.pdf")
    );

    FolderScanner scanner = new FolderScanner();

    // Act and assert: scanning a file should fail.
    org.junit.jupiter.api.Assertions.assertThrows(
        IOException.class,
        () -> scanner.scan(file)
    );
}
@Test
void excludesHiddenFiles() throws IOException {

    // Arrange: create a file and mark it hidden.
    Path hiddenFile = Files.createFile(
        tempDirectory.resolve("Hidden.txt")
    );

    Files.setAttribute(hiddenFile, "dos:hidden", true);

    FolderScanner scanner = new FolderScanner();

    // Act: scan the temporary directory.
    List<Path> result = scanner.scan(tempDirectory);

    // Assert: the hidden file should not appear.
    org.junit.jupiter.api.Assertions.assertFalse(
        result.contains(hiddenFile)
    );
}
}