
package com.sanctumkeeper.organize;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrganizeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void matchingRuleMovesFileToDestination() throws Exception {
        // Arrange: create a test file inside a temporary folder.
        Path sourceFile = tempDir.resolve("report.pdf");
        Files.writeString(sourceFile, "test content");

        // Arrange: create a rule that moves PDF files to Documents.
        Rule rule = new Rule(
                "pdf-rule",
                new Condition("extension", null, "pdf"),
                new Action("move", "Documents")
        );

        OrganizeService service = new OrganizeService();

        // Act: run the organize operation.
        List<OrganizeResult> results =
                service.organize(tempDir, List.of(rule));

        // Assert: the file moved to the expected location.
        Path movedFile = tempDir.resolve("Documents").resolve("report.pdf");

        assertFalse(Files.exists(sourceFile),
                "The original file should no longer exist.");

        assertTrue(Files.exists(movedFile),
                "The file should exist in the destination folder.");

        assertEquals(1, results.size());
        assertEquals("ORGANIZED", results.get(0).getStatus());

        // Assert: the successful move was recorded for Undo.
        assertEquals(1, service.getLastSuccessfulMoves().size());
    }
@Test
void fileWithoutMatchingRuleRemainsUntouched() throws Exception {
    // Arrange: create a file that won't match our rule.
    Path sourceFile = tempDir.resolve("photo.jpg");
    Files.writeString(sourceFile, "image content");

    // Arrange: create a rule that only matches PDF files.
    Rule rule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    OrganizeService service = new OrganizeService();

    // Act: organize using the PDF-only rule.
    List<OrganizeResult> results =
            service.organize(tempDir, List.of(rule));

    // Assert: the original file remains in place.
    assertTrue(Files.exists(sourceFile),
            "A file without a matching rule must remain untouched.");

    assertFalse(Files.exists(tempDir.resolve("Documents")),
            "No destination folder should be created.");

    assertEquals(1, results.size());
    assertEquals("NO_MATCH", results.get(0).getStatus());

    // No file was moved, so Undo history should be empty.
    assertTrue(service.getLastSuccessfulMoves().isEmpty());
}

@Test
void existingDestinationFileIsNotOverwritten() throws Exception {
    // Arrange: create the source file.
    Path sourceFile = tempDir.resolve("report.pdf");
    Files.writeString(sourceFile, "original source content");

    // Arrange: create the destination and an existing file with the same name.
    Path destinationFolder = tempDir.resolve("Documents");
    Files.createDirectories(destinationFolder);

    Path existingFile = destinationFolder.resolve("report.pdf");
    Files.writeString(existingFile, "important existing content");

    Rule rule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    OrganizeService service = new OrganizeService();

    // Act: attempt to organize the source file.
    List<OrganizeResult> results =
            service.organize(tempDir, List.of(rule));

    // Assert: both files' contents remain unchanged.
    assertTrue(Files.exists(sourceFile));
    assertEquals("original source content",
            Files.readString(sourceFile));

    assertEquals("important existing content",
            Files.readString(existingFile));

    assertEquals(1, results.size());
    assertEquals("SKIPPED_CONFLICT", results.get(0).getStatus());

    // The skipped move must not be recorded for Undo.
    assertTrue(service.getLastSuccessfulMoves().isEmpty());
}

@Test
void failedFileDoesNotStopOtherFiles() throws Exception {
    // Arrange: create two PDF files.
    Path blockedFile = tempDir.resolve("blocked.pdf");
    Path normalFile = tempDir.resolve("normal.pdf");

    Files.writeString(blockedFile, "blocked content");
    Files.writeString(normalFile, "normal content");

    // Replace blocked.pdf with a symbolic link to a separate file.
    Path linkTarget = tempDir.resolve("link-target.txt");
    Files.writeString(linkTarget, "link target content");
    Files.delete(blockedFile);

    try {
        Files.createSymbolicLink(blockedFile, linkTarget.getFileName());
    } catch (UnsupportedOperationException | SecurityException
             | java.io.IOException exception) {
        // Some Windows configurations don't allow symbolic links.
        org.junit.jupiter.api.Assumptions.assumeTrue(
                false,
                "Symbolic links are unavailable in this environment."
        );
    }

    Rule rule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    OrganizeService service = new OrganizeService();

    // Act: organize both files.
    List<OrganizeResult> results =
            service.organize(tempDir, List.of(rule));

    // Assert: the normal file was moved successfully.
    Path movedNormalFile =
            tempDir.resolve("Documents").resolve("normal.pdf");

    assertTrue(Files.exists(movedNormalFile));
    assertFalse(Files.exists(normalFile));

    // The symbolic link should be reported as failed.
    OrganizeResult failedResult = results.stream()
            .filter(result -> result.getFileName().equals("blocked.pdf"))
            .findFirst()
            .orElseThrow();

    assertEquals("FAILED", failedResult.getStatus());

    // Only the successful move should be recorded for Undo.
    assertEquals(1, service.getLastSuccessfulMoves().size());
}

@Test
void destinationOutsideSourceFolderIsRejected() throws Exception {
    // Arrange: create a PDF inside the selected source folder.
    Path sourceFile = tempDir.resolve("report.pdf");
    Files.writeString(sourceFile, "important content");

    Rule rule = new Rule(
            "unsafe-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "../outside")
    );

    OrganizeService service = new OrganizeService();

    // Act: attempt to organize using the unsafe destination.
    List<OrganizeResult> results =
            service.organize(tempDir, List.of(rule));

    // Assert: the source file remains untouched.
    assertTrue(Files.exists(sourceFile));
    assertEquals("important content", Files.readString(sourceFile));

    // The operation should report failure.
    assertEquals(1, results.size());
    assertEquals("FAILED", results.get(0).getStatus());

    // The unsafe destination must not be created.
    assertFalse(Files.exists(
            tempDir.getParent().resolve("outside")
    ));

    // Failed moves must not enter Undo history.
    assertTrue(service.getLastSuccessfulMoves().isEmpty());
}

@Test
void successfulMoveRecordsOriginalAndDestinationPaths() throws Exception {
    // Arrange: create a file to move.
    Path sourceFile = tempDir.resolve("notes.pdf");
    Files.writeString(sourceFile, "notes");

    Rule rule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    OrganizeService service = new OrganizeService();

    // Act: organize the file.
    service.organize(tempDir, List.of(rule));

    // Retrieve the history recorded by the service.
    List<MoveRecord> moves = service.getLastSuccessfulMoves();

    // Assert: exactly one successful move was recorded.
    assertEquals(1, moves.size());

    MoveRecord record = moves.get(0);

    assertEquals(
            sourceFile.toAbsolutePath().normalize(),
            record.getOriginalPath()
    );

    assertEquals(
            tempDir.resolve("Documents")
                    .resolve("notes.pdf")
                    .toAbsolutePath()
                    .normalize(),
            record.getMovedPath()
    );
}

@Test
void newOrganizeOperationReplacesPreviousUndoHistory()
        throws Exception {

    // Arrange: create the first file and its rule.
    Path firstFile = tempDir.resolve("report.pdf");
    Files.writeString(firstFile, "report");

    Rule pdfRule = new Rule(
            "pdf-rule",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    Rule jpgRule = new Rule(
            "jpg-rule",
            new Condition("extension", null, "jpg"),
            new Action("move", "Images")
    );

    OrganizeService service = new OrganizeService();

    // Act 1: organize the first file.
    service.organize(tempDir, List.of(pdfRule, jpgRule));

    // Verify that the first operation recorded one move.
    assertEquals(1, service.getLastSuccessfulMoves().size());

    // Arrange: create a second file for a new operation.
    Path secondFile = tempDir.resolve("photo.jpg");
    Files.writeString(secondFile, "photo");

    // Act 2: organize again using the same service instance.
    service.organize(tempDir, List.of(pdfRule, jpgRule));

    // Assert: history now contains only the second operation's move.
    List<MoveRecord> moves = service.getLastSuccessfulMoves();

    assertEquals(1, moves.size());

    assertEquals(
            secondFile.toAbsolutePath().normalize(),
            moves.get(0).getOriginalPath()
    );

    assertEquals(
            tempDir.resolve("Images")
                    .resolve("photo.jpg")
                    .toAbsolutePath()
                    .normalize(),
            moves.get(0).getMovedPath()
    );
}


}
