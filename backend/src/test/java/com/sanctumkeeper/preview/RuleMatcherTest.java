package com.sanctumkeeper.preview;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleMatcherTest {

    @Test
    void matchesFileExtensionWithoutCaseSensitivity() {

        // Arrange: create a rule that matches PDF files.
        Rule rule = new Rule(
            "rule-1",
            new Condition("extension", null, "pdf"),
            new Action("move", "Documents")
        );

        RuleMatcher matcher = new RuleMatcher();

        // Act: check a file whose extension is uppercase.
        Rule result = matcher.findFirstMatch(
            Path.of("Report.PDF"),
            List.of(rule)
        );

        // Assert: the matching rule should be returned.
        assertEquals(rule, result);
    }
    
@Test
void returnsFirstMatchingRuleWhenMultipleRulesMatch() {

    // Arrange: both rules match Report.pdf.
    Rule firstRule = new Rule(
        "rule-1",
        new Condition("extension", null, "pdf"),
        new Action("move", "Documents")
    );

    Rule secondRule = new Rule(
        "rule-2",
        new Condition("filename", "contains", "Report"),
        new Action("move", "Important")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: evaluate the rules in their given order.
    Rule result = matcher.findFirstMatch(
        Path.of("Report.pdf"),
        List.of(firstRule, secondRule)
    );

    // Assert: the first matching rule must win.
    assertEquals(firstRule, result);
}

@Test
void matchesFilenameUsingStartsWithMode() {

    // Arrange: match filenames beginning with "Report".
    Rule rule = new Rule(
        "rule-3",
        new Condition("filename", "startsWith", "Report"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: the base filename is "Report_Final".
    Rule result = matcher.findFirstMatch(
        Path.of("Report_Final.pdf"),
        List.of(rule)
    );

    // Assert: the rule should match.
    assertEquals(rule, result);
}

@Test
void matchesFilenameUsingContainsMode() {

    // Arrange: match filenames containing "final".
    Rule rule = new Rule(
        "rule-4",
        new Condition("filename", "contains", "final"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: "FINAL" appears in the middle of the filename.
    Rule result = matcher.findFirstMatch(
        Path.of("Project_FINAL_Draft.pdf"),
        List.of(rule)
    );

    // Assert: matching should ignore capitalization.
    assertEquals(rule, result);
}

@Test
void matchesFilenameUsingEqualsMode() {

    // Arrange: match the exact base filename.
    Rule rule = new Rule(
        "rule-5",
        new Condition("filename", "equals", "Report"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: the base filename is "Report".
    Rule result = matcher.findFirstMatch(
        Path.of("Report.pdf"),
        List.of(rule)
    );

    // Assert: the rule should match.
    assertEquals(rule, result);
}

@Test
void doesNotMatchPartialFilenameUsingEqualsMode() {

    // Arrange: require the exact base filename "Report".
    Rule rule = new Rule(
        "rule-6",
        new Condition("filename", "equals", "Report"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: the base filename is "Report_Final", not "Report".
    Rule result = matcher.findFirstMatch(
        Path.of("Report_Final.pdf"),
        List.of(rule)
    );

    // Assert: no rule should match.
    assertNull(result);
}

@Test
void returnsNullWhenNoRuleMatches() {

    // Arrange: this rule only matches PDF files.
    Rule rule = new Rule(
        "rule-7",
        new Condition("extension", null, "pdf"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: try to match a text file.
    Rule result = matcher.findFirstMatch(
        Path.of("Notes.txt"),
        List.of(rule)
    );

    // Assert: no matching rule should be returned.
    assertNull(result);
}

@Test
void treatsDotfileAsFilenameWithoutExtension() {

    // Arrange: match the complete dotfile name.
    Rule rule = new Rule(
        "rule-8",
        new Condition("filename", "equals", ".gitignore"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: check a filename beginning with a dot.
    Rule result = matcher.findFirstMatch(
        Path.of(".gitignore"),
        List.of(rule)
    );

    // Assert: the complete filename should match.
    assertEquals(rule, result);
}

@Test
void treatsFilenameWithTrailingDotAsHavingNoExtension() {

    // Arrange: match files with no extension.
    Rule rule = new Rule(
        "rule-9",
        new Condition("extension", null, "pdf"),
        new Action("move", "Documents")
    );

    RuleMatcher matcher = new RuleMatcher();

    // Act: check a filename ending with a dot.
    Rule result = matcher.findFirstMatch(
        Path.of("Report."),
        List.of(rule)
    );

    // Assert: the PDF rule should not match.
    assertNull(result);
}

}