package com.sanctumkeeper.preview;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviewServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void previewsMatchingFileWithCorrectDestination() throws IOException {

        // Arrange: create a PDF inside the temporary source folder.
        Files.createFile(tempDirectory.resolve("report.pdf"));

        // Create a rule that matches PDF files.
        Rule rule = new Rule(
                "pdf-rule",
                new Condition("extension", null, "pdf"),
                new Action("move", "Documents")
        );

        PreviewService previewService = new PreviewService();

        // Act: preview the folder without moving any files.
        List<PreviewResult> results = previewService.preview(
                tempDirectory,
                List.of(rule)
        );

        // Assert: the file has the expected preview result.
        assertEquals(1, results.size());

        PreviewResult result = results.get(0);

        assertEquals("report.pdf", result.getFileName());
        assertEquals("MATCHED", result.getStatus());
        assertEquals("pdf-rule", result.getRuleId());
        assertEquals("move", result.getAction());

        Path expectedDestination = tempDirectory.resolve("Documents")
                .toAbsolutePath()
                .normalize();

        assertEquals(
                expectedDestination.toString(),
                result.getDestination()
        );
    }
    @Test
void includesFilesThatHaveNoMatchingRule() throws IOException {

    // Arrange: create a file with no matching rule.
    Files.createFile(tempDirectory.resolve("photo.jpg"));

    PreviewService previewService = new PreviewService();

    // Act: preview with an empty rule list.
    List<PreviewResult> results = previewService.preview(
            tempDirectory,
            List.of()
    );

    // Assert: the file is reported as unmatched.
    assertEquals(1, results.size());

    PreviewResult result = results.get(0);

    assertEquals("photo.jpg", result.getFileName());
    assertEquals("NO_MATCH", result.getStatus());

    org.junit.jupiter.api.Assertions.assertNull(result.getRuleId());
    org.junit.jupiter.api.Assertions.assertNull(result.getAction());
    org.junit.jupiter.api.Assertions.assertNull(result.getDestination());
}
@Test
void previewDoesNotMoveFilesOrCreateDestinationFolders()
        throws IOException {

    // Arrange: create a file and a rule that matches it.
    Path sourceFile = Files.createFile(
            tempDirectory.resolve("report.pdf")
    );

    Rule rule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    PreviewService previewService = new PreviewService();

    // Act: preview the folder.
    previewService.preview(tempDirectory, List.of(rule));

    // Assert: the original file remains in place.
    org.junit.jupiter.api.Assertions.assertTrue(
            Files.exists(sourceFile)
    );

    // Assert: the destination folder was not created.
    Path destinationFolder = tempDirectory.resolve("Documents");

    org.junit.jupiter.api.Assertions.assertFalse(
            Files.exists(destinationFolder)
    );
}
@Test
void rejectsDestinationOutsideSourceFolder() throws IOException {

    // Arrange: create a PDF and a rule with an escaping destination.
    Files.createFile(tempDirectory.resolve("report.pdf"));

    String unsafeDestination = Path.of(
            "..", "..", "OutsideFolder"
    ).toString();

    Rule rule = new Rule(
            "unsafe-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", unsafeDestination)
    );

    PreviewService previewService = new PreviewService();

    // Act and assert: an escaping destination must be rejected.
    org.junit.jupiter.api.Assertions.assertThrows(
            IOException.class,
            () -> previewService.preview(tempDirectory, List.of(rule))
    );
}
}