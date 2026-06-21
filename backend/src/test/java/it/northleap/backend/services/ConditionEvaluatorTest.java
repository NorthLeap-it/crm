package it.northleap.backend.services;

import it.northleap.backend.entities.Record;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    private Record record(String status, Map<String, Object> data) {
        Record r = new Record();
        r.setStatus(status);
        r.setData(data);
        return r;
    }

    @Test
    void nullConditionsAlwaysTrue() {
        assertTrue(evaluator.evaluate(null, record(null, Map.of())));
    }

    @Test
    void eqOperator() {
        Map<String, Object> cond = Map.of("field", "stage", "op", "eq", "value", "won");
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("stage", "won"))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("stage", "lost"))));
    }

    @Test
    void neqOperator() {
        Map<String, Object> cond = Map.of("field", "stage", "op", "neq", "value", "won");
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("stage", "won"))));
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("stage", "lost"))));
    }

    @Test
    void gtOperator() {
        Map<String, Object> cond = Map.of("field", "amount", "op", "gt", "value", 1000);
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("amount", 2000))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("amount", 500))));
    }

    @Test
    void ltOperatorNumeric() {
        Map<String, Object> cond = Map.of("field", "amount", "op", "lt", "value", 1000);
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("amount", 500))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("amount", 2000))));
    }

    @Test
    void ltOperatorTodaySpecialCase() {
        Map<String, Object> cond = Map.of("field", "dueDate", "op", "lt", "value", "today");
        Instant past = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant future = Instant.now().plus(5, ChronoUnit.DAYS);
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("dueDate", past.toString()))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("dueDate", future.toString()))));
    }

    @Test
    void containsOperator() {
        Map<String, Object> cond = Map.of("field", "name", "op", "contains", "value", "lead");
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("name", "Big lead corp"))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("name", "Acme"))));
    }

    @Test
    void isSetOperator() {
        Map<String, Object> cond = Map.of("field", "email", "op", "is_set");
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("email", "a@b.com"))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of())));
    }

    @Test
    void inDaysOperator() {
        Map<String, Object> cond = Map.of("field", "renewalDate", "op", "in_days", "value", 7);
        Instant in3days = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant in30days = Instant.now().plus(30, ChronoUnit.DAYS);
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("renewalDate", in3days.toString()))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("renewalDate", in30days.toString()))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("renewalDate", past.toString()))));
    }

    @Test
    void statusFieldResolvesToRecordStatusColumn() {
        Map<String, Object> cond = Map.of("field", "status", "op", "eq", "value", "qualified");
        assertTrue(evaluator.evaluate(cond, record("qualified", Map.of())));
    }

    @Test
    void allCombinatorRequiresEveryRule() {
        Map<String, Object> cond = Map.of("all", List.of(
                Map.of("field", "stage", "op", "eq", "value", "won"),
                Map.of("field", "amount", "op", "gt", "value", 1000)
        ));
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("stage", "won", "amount", 2000))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("stage", "won", "amount", 500))));
    }

    @Test
    void anyCombinatorRequiresOneRule() {
        Map<String, Object> cond = Map.of("any", List.of(
                Map.of("field", "stage", "op", "eq", "value", "won"),
                Map.of("field", "stage", "op", "eq", "value", "negotiation")
        ));
        assertTrue(evaluator.evaluate(cond, record(null, Map.of("stage", "negotiation"))));
        assertFalse(evaluator.evaluate(cond, record(null, Map.of("stage", "lost"))));
    }

    @Test
    void interpolateSimplePath() {
        Record r = record(null, Map.of("name", "Acme Corp"));
        assertEquals("Hello Acme Corp!", evaluator.interpolate("Hello {{record.name}}!", r));
    }

    @Test
    void interpolateStatusPath() {
        Record r = record("qualified", Map.of());
        assertEquals("Stato: qualified", evaluator.interpolate("Stato: {{record.status}}", r));
    }

    @Test
    void interpolateNestedPath() {
        Record r = record(null, Map.of("address", Map.of("city", "Roma")));
        assertEquals("Città: Roma", evaluator.interpolate("Città: {{record.address.city}}", r));
    }

    @Test
    void interpolateMissingPathYieldsEmptyString() {
        Record r = record(null, Map.of());
        assertEquals("Value: ", evaluator.interpolate("Value: {{record.missing}}", r));
    }
}
