package com.sanctumkeeper.validation;

import com.sanctumkeeper.model.Action;
import com.sanctumkeeper.model.Condition;
import com.sanctumkeeper.model.Rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleValidatorTest {

    @Test
    void acceptsValidRule() {
        Condition condition =
                new Condition("extension", null, "pdf");

        Action action =
                new Action("move", "Documents");

        Rule rule =
                new Rule("rule-1", condition, action);

        String error = RuleValidator.validate(rule);

        assertNull(error);
    }
    
@Test
void rejectsEmptyDestination() {
    Condition condition =
            new Condition("extension", null, "pdf");

    Action action =
            new Action("move", " ");

    Rule rule =
            new Rule("rule-2", condition, action);

    String error = RuleValidator.validate(rule);

    assertNotNull(error);
    assertEquals("Destination cannot be empty.", error);
}

@Test
void rejectsDestinationOutsideSourceFolder() {
    Condition condition =
            new Condition("extension", null, "pdf");

    Action action =
            new Action("move", "..\\..\\OtherFolder");

    Rule rule =
            new Rule("rule-3", condition, action);

    String error = RuleValidator.validate(rule);

    assertNotNull(error);
    assertEquals(
            "Destination must remain inside the selected folder.",
            error
    );
}

}
