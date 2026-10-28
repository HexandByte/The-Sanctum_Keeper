
package com.sanctumkeeper.storage;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void savesValidRulesToJsonFile() throws IOException {

        // Arrange: Create storage and one valid rule.
        RuleStorage storage = new RuleStorage(tempDirectory);

        Rule rule = new Rule(
                "rule-1",
                new Condition("extension", null, "pdf"),
                new Action("move", "Documents")
        );

        // Act: Save the rule.
        storage.saveRules(List.of(rule));

        // Assert: Check that the file exists.
        Path rulesFile = tempDirectory.resolve("rules.json");

        assertTrue(Files.exists(rulesFile));

        // Assert: Check that the saved JSON contains our rule.
        String content = Files.readString(rulesFile);

        assertTrue(content.contains("\"rules\""));
        assertTrue(content.contains("rule-1"));
        assertTrue(content.contains("Documents"));
    }
    
@Test
void rejectsInvalidRulesWithoutOverwritingExistingFile() throws IOException {

    RuleStorage storage = new RuleStorage(tempDirectory);

    // Arrange: Save a valid rule first.
    Rule validRule = new Rule(
            "rule-1",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    storage.saveRules(List.of(validRule));

    Path rulesFile = tempDirectory.resolve("rules.json");
    String originalContent = Files.readString(rulesFile);

    // Create an invalid rule with an empty destination.
    Rule invalidRule = new Rule(
            "rule-2",
            new Condition("extension", null, "txt"),
            new Action("move", " ")
    );

    // Act + Assert: Saving the invalid rule must fail.
    assertThrows(
            IOException.class,
            () -> storage.saveRules(List.of(invalidRule))
    );

    // Verify that the original file remains unchanged.
    assertEquals(originalContent, Files.readString(rulesFile));
}

@Test
void loadsSavedRulesFromJsonFile() throws IOException {

    // Arrange: Save a rule.
    RuleStorage storage = new RuleStorage(tempDirectory);

    Rule originalRule = new Rule(
            "rule-1",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    storage.saveRules(List.of(originalRule));

    // Act: Load the saved rules.
    List<Rule> loadedRules = storage.loadRules();

    // Assert: Verify the loaded rule.
    assertEquals(1, loadedRules.size());

    Rule loadedRule = loadedRules.get(0);

    assertEquals("rule-1", loadedRule.getId());
    assertEquals("extension", loadedRule.getCondition().getType());
    assertEquals("pdf", loadedRule.getCondition().getValue());
    assertEquals("move", loadedRule.getAction().getType());
    assertEquals("Documents", loadedRule.getAction().getDestination());
}

@Test
void returnsEmptyListWhenConfigFileDoesNotExist() throws IOException {

    // Arrange: Create storage without saving any rules.
    RuleStorage storage = new RuleStorage(tempDirectory);

    // Act: Try to load rules.
    List<Rule> loadedRules = storage.loadRules();

    // Assert: Missing configuration means no rules.
    assertNotNull(loadedRules);
    assertTrue(loadedRules.isEmpty());

    // Loading should not create a configuration file.
    assertFalse(Files.exists(tempDirectory.resolve("rules.json")));
}

@Test
void preservesInvalidJsonAndReportsAnError() throws IOException {

    // Arrange: Write deliberately corrupted JSON.
    Path rulesFile = tempDirectory.resolve("rules.json");
    String corruptedContent = "{ this is not valid JSON";

    Files.writeString(rulesFile, corruptedContent);

    RuleStorage storage = new RuleStorage(tempDirectory);

    // Act + Assert: Loading must report an error.
    assertThrows(
            IOException.class,
            () -> storage.loadRules()
    );

    // Verify that the original corrupted file is preserved.
    assertEquals(corruptedContent, Files.readString(rulesFile));
}

@Test
void rejectsDuplicateRuleIdsWithoutOverwritingExistingFile() throws IOException {

    RuleStorage storage = new RuleStorage(tempDirectory);

    // Arrange: Save one valid rule.
    Rule originalRule = new Rule(
            "rule-1",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
    );

    storage.saveRules(List.of(originalRule));

    Path rulesFile = tempDirectory.resolve("rules.json");
    String originalContent = Files.readString(rulesFile);

    // Create two rules with the same ID.
    Rule firstRule = new Rule(
            "duplicate-id",
            new Condition("extension", null, "txt"),
            new Action("move", "Text")
    );

    Rule secondRule = new Rule(
            "duplicate-id",
            new Condition("extension", null, "jpg"),
            new Action("move", "Images")
    );

    // Act + Assert: Duplicate IDs must be rejected.
    assertThrows(
            IOException.class,
            () -> storage.saveRules(List.of(firstRule, secondRule))
    );

    // The previously saved configuration must remain untouched.
    assertEquals(originalContent, Files.readString(rulesFile));
}

}
