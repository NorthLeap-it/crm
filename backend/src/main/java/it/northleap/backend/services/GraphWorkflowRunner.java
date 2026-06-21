package it.northleap.backend.services;

import it.northleap.backend.entities.Record;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Porting di graph-runner.ts. Semplificazione deliberata rispetto all'originale: l'originale
// inietta un ActionExecutor come parametro funzionale e un callback "sleep" per testabilità in
// un contesto a moduli JS — qui WorkflowActionExecutor è semplicemente @Autowired direttamente,
// niente parametri funzionali (stesso tipo di semplificazione già fatta in Fase 3 unendo i due
// call-site di PageGeneratorService.generate in un metodo con parametro invece di duplicare la
// logica). graph/nodi/archi restano Map<String,Object> grezzi navigati a runtime (stesso
// duck-typing strutturale già usato per FilterGroup in Fase 3 - niente JsonNode, niente DTO
// tipizzato per {nodes,edges}).
@Service
@RequiredArgsConstructor
public class GraphWorkflowRunner {

    // guardia anti-loop-infinito, identica all'originale
    private static final int MAX_STEPS = 500;
    private static final long MAX_DELAY_MS = 60_000;

    private final WorkflowActionExecutor workflowActionExecutor;
    private final ConditionEvaluator conditionEvaluator;

    @SuppressWarnings("unchecked")
    public void run(Map<String, Object> graph, Record record, List<Map<String, Object>> stepsOut) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.getOrDefault("nodes", List.of());
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.getOrDefault("edges", List.of());

        Map<String, Map<String, Object>> nodesById = new LinkedHashMap<>();
        for (Map<String, Object> n : nodes) {
            nodesById.put(String.valueOf(n.get("id")), n);
        }

        Map<String, Object> start = nodes.stream()
                .filter(n -> "trigger".equals(n.get("type")))
                .findFirst().orElse(null);
        if (start == null) {
            return;
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        int[] budget = {MAX_STEPS};
        walk(String.valueOf(start.get("id")), nodesById, edges, record, vars, stepsOut, budget);
    }

    private List<Map<String, Object>> outEdges(List<Map<String, Object>> edges, String nodeId, String handle) {
        return edges.stream()
                .filter(e -> nodeId.equals(e.get("source")) && handle.equals(e.getOrDefault("sourceHandle", "out")))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void walk(String nodeId, Map<String, Map<String, Object>> nodesById, List<Map<String, Object>> edges,
                       Record record, Map<String, Object> vars, List<Map<String, Object>> stepsOut, int[] budget) {
        Map<String, Object> node = nodesById.get(nodeId);
        if (node == null || budget[0]-- <= 0) {
            return;
        }
        String type = String.valueOf(node.get("type"));
        Map<String, Object> data = (Map<String, Object>) node.getOrDefault("data", Map.of());

        switch (type) {
            case "trigger" -> {
                for (Map<String, Object> e : outEdges(edges, nodeId, "out")) {
                    walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                }
            }
            case "action" -> {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("nodeId", nodeId);
                step.put("type", data.getOrDefault("type", "action"));
                try {
                    Map<String, Object> result = workflowActionExecutor.execute(data, record);
                    step.put("result", result);
                    stepsOut.add(step);
                } catch (RuntimeException ex) {
                    step.put("error", ex.getMessage());
                    stepsOut.add(step);
                    throw ex;
                }
                for (Map<String, Object> e : outEdges(edges, nodeId, "out")) {
                    walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                }
            }
            case "condition", "branch" -> {
                Map<String, Object> conditions = data.get("conditions") != null
                        ? (Map<String, Object>) data.get("conditions") : data;
                boolean ok = conditionEvaluator.evaluate(conditions, record);
                stepsOut.add(Map.of("nodeId", nodeId, "type", "condition", "result", ok));
                for (Map<String, Object> e : outEdges(edges, nodeId, ok ? "true" : "false")) {
                    walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                }
            }
            case "loop" -> {
                List<Object> items = resolveList(data, record);
                String as = data.get("as") != null ? String.valueOf(data.get("as")) : "item";
                for (Object item : items) {
                    vars.put(as, item);
                    for (Map<String, Object> e : outEdges(edges, nodeId, "each")) {
                        walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                    }
                }
                stepsOut.add(Map.of("nodeId", nodeId, "type", "loop", "result", Map.of("iterations", items.size())));
                for (Map<String, Object> e : outEdges(edges, nodeId, "done")) {
                    walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                }
            }
            case "delay" -> {
                long ms = data.get("ms") != null ? ((Number) data.get("ms")).longValue() : 0;
                if (ms > 0 && ms <= MAX_DELAY_MS) {
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                stepsOut.add(Map.of("nodeId", nodeId, "type", "delay", "result", Map.of("ms", ms)));
                for (Map<String, Object> e : outEdges(edges, nodeId, "out")) {
                    walk(String.valueOf(e.get("target")), nodesById, edges, record, vars, stepsOut, budget);
                }
            }
            default -> {
                // tipo nodo sconosciuto: nessuna azione, stesso comportamento implicito dell'originale
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> resolveList(Map<String, Object> data, Record record) {
        Object items = data.get("items");
        if (items instanceof List<?> list) {
            return (List<Object>) list;
        }
        Object fieldName = data.get("field");
        if (fieldName != null && record != null && record.getData() != null) {
            Object v = record.getData().get(String.valueOf(fieldName));
            if (v != null) {
                return v instanceof List<?> list ? (List<Object>) list : List.of(v);
            }
        }
        return List.of();
    }
}
