package it.northleap.backend.services;

import it.northleap.backend.entities.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphWorkflowRunnerTest {

    @Mock
    private WorkflowActionExecutor workflowActionExecutor;
    @Mock
    private ConditionEvaluator conditionEvaluator;

    private GraphWorkflowRunner runner;

    @BeforeEach
    void setUp() {
        runner = new GraphWorkflowRunner(workflowActionExecutor, conditionEvaluator);
    }

    private Map<String, Object> node(String id, String type, Map<String, Object> data) {
        Map<String, Object> n = new java.util.LinkedHashMap<>();
        n.put("id", id);
        n.put("type", type);
        n.put("data", data);
        return n;
    }

    private Map<String, Object> edge(String source, String target, String handle) {
        Map<String, Object> e = new java.util.LinkedHashMap<>();
        e.put("source", source);
        e.put("target", target);
        e.put("sourceHandle", handle);
        return e;
    }

    @Test
    void linearChainTriggerToAction() {
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of("ok", true));
        Map<String, Object> graph = Map.of(
                "nodes", List.of(node("t", "trigger", Map.of()), node("a", "action", Map.of("type", "notify_user"))),
                "edges", List.of(edge("t", "a", "out"))
        );
        List<Map<String, Object>> steps = new ArrayList<>();
        runner.run(graph, null, steps);

        assertEquals(1, steps.size());
        assertEquals("a", steps.get(0).get("nodeId"));
        verify(workflowActionExecutor, times(1)).execute(any(), any());
    }

    @Test
    void conditionBranchFollowsTruePath() {
        when(conditionEvaluator.evaluate(any(), any())).thenReturn(true);
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of());
        Map<String, Object> graph = Map.of(
                "nodes", List.of(
                        node("t", "trigger", Map.of()),
                        node("c", "condition", Map.of("conditions", Map.of("field", "stage", "op", "eq", "value", "won"))),
                        node("yes", "action", Map.of("type", "create_record")),
                        node("no", "action", Map.of("type", "delay"))
                ),
                "edges", List.of(
                        edge("t", "c", "out"),
                        edge("c", "yes", "true"),
                        edge("c", "no", "false")
                )
        );
        List<Map<String, Object>> steps = new ArrayList<>();
        runner.run(graph, null, steps);

        verify(workflowActionExecutor, times(1)).execute(any(), any());
        boolean ranYes = steps.stream().anyMatch(s -> "yes".equals(s.get("nodeId")));
        boolean ranNo = steps.stream().anyMatch(s -> "no".equals(s.get("nodeId")));
        assertTrue(ranYes);
        assertTrue(!ranNo);
    }

    @Test
    void conditionBranchFollowsFalsePath() {
        when(conditionEvaluator.evaluate(any(), any())).thenReturn(false);
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of());
        Map<String, Object> graph = Map.of(
                "nodes", List.of(
                        node("t", "trigger", Map.of()),
                        node("c", "condition", Map.of("conditions", Map.of("field", "stage", "op", "eq", "value", "won"))),
                        node("yes", "action", Map.of("type", "create_record")),
                        node("no", "action", Map.of("type", "delay"))
                ),
                "edges", List.of(
                        edge("t", "c", "out"),
                        edge("c", "yes", "true"),
                        edge("c", "no", "false")
                )
        );
        List<Map<String, Object>> steps = new ArrayList<>();
        runner.run(graph, null, steps);

        boolean ranNo = steps.stream().anyMatch(s -> "no".equals(s.get("nodeId")));
        assertTrue(ranNo);
    }

    @Test
    void loopIteratesOverItemsEachThenDone() {
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of());
        Map<String, Object> graph = Map.of(
                "nodes", List.of(
                        node("t", "trigger", Map.of()),
                        node("l", "loop", Map.of("items", List.of("a", "b", "c"))),
                        node("act", "action", Map.of("type", "notify_user"))
                ),
                "edges", List.of(
                        edge("t", "l", "out"),
                        edge("l", "act", "each")
                )
        );
        List<Map<String, Object>> steps = new ArrayList<>();
        runner.run(graph, null, steps);

        verify(workflowActionExecutor, times(3)).execute(any(), any());
        boolean loopStepLogged = steps.stream().anyMatch(s -> "loop".equals(s.get("type"))
                && Map.of("iterations", 3).equals(s.get("result")));
        assertTrue(loopStepLogged);
    }

    @Test
    void delayStepIsLoggedWithoutBlockingTooLong() {
        Map<String, Object> graph = Map.of(
                "nodes", List.of(node("t", "trigger", Map.of()), node("d", "delay", Map.of("ms", 2))),
                "edges", List.of(edge("t", "d", "out"))
        );
        List<Map<String, Object>> steps = new ArrayList<>();
        long start = System.currentTimeMillis();
        runner.run(graph, null, steps);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 5000);
        assertTrue(steps.stream().anyMatch(s -> "delay".equals(s.get("type"))));
    }

    @Test
    void maxStepsGuardTerminatesOnCyclicGraph() {
        when(workflowActionExecutor.execute(any(), any())).thenReturn(Map.of());
        // ciclo infinito: l'azione "a" punta a se stessa
        Map<String, Object> graph = Map.of(
                "nodes", List.of(node("t", "trigger", Map.of()), node("a", "action", Map.of("type", "noop"))),
                "edges", List.of(edge("t", "a", "out"), edge("a", "a", "out"))
        );
        List<Map<String, Object>> steps = new ArrayList<>();

        long start = System.currentTimeMillis();
        runner.run(graph, null, steps);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 10_000, "la guardia MAX_STEPS deve far terminare l'esecuzione rapidamente");
        verify(workflowActionExecutor, org.mockito.Mockito.atMost(500)).execute(any(), any());
    }

    @Test
    void actionExceptionPropagatesAfterLoggingErrorStep() {
        when(workflowActionExecutor.execute(any(), any())).thenThrow(new RuntimeException("boom"));
        Map<String, Object> graph = Map.of(
                "nodes", List.of(node("t", "trigger", Map.of()), node("a", "action", Map.of("type", "create_record"))),
                "edges", List.of(edge("t", "a", "out"))
        );
        List<Map<String, Object>> steps = new ArrayList<>();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> runner.run(graph, null, steps));
        assertEquals("boom", ex.getMessage());
        assertEquals(1, steps.size());
        assertEquals("boom", steps.get(0).get("error"));
    }
}
