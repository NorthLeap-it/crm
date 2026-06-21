package it.northleap.backend.config;

import it.northleap.backend.entities.Workflow;
import it.northleap.backend.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Porting letterale delle 12 automazioni di default in prisma/seed.ts (righe 226-311, Fase 5,
// 05-WORKFLOW-ENGINE.md). Idempotenza per riga (findByName, skip se gia' esiste) - stesso
// pattern dell'originale (findFirst per nome), non uno skip-se-tabella-non-vuota.
@Component
@RequiredArgsConstructor
@Order(4)
public class WorkflowSeeder implements ApplicationRunner {

    private record WorkflowSpec(String name, String description, Map<String, Object> trigger,
                                 Map<String, Object> conditions, List<Map<String, Object>> actions) {
    }

    private static Map<String, Object> rule(String field, String op, Object value) {
        return Map.of("field", field, "op", op, "value", value);
    }

    // l'originale non passa "value" affatto per is_set (non lo legge) - Map.of a 3 chiavi non
    // accetta null, quindi un overload dedicato invece di un valore sentinella
    private static Map<String, Object> rule(String field, String op) {
        return Map.of("field", field, "op", op);
    }

    @SafeVarargs
    private static Map<String, Object> all(Map<String, Object>... rules) {
        return Map.of("all", List.of(rules));
    }

    private static final List<WorkflowSpec> WORKFLOWS = List.of(
            new WorkflowSpec("Lead qualificato → crea Opportunità",
                    "Quando un lead diventa qualificato, genera un'opportunità e avvisa il responsabile.",
                    Map.of("type", "field.changed", "objectKey", "lead", "field", "status"),
                    all(rule("status", "eq", "qualified")),
                    List.of(
                            Map.of("type", "create_record", "objectKey", "opportunity",
                                    "map", Map.of("name", "Opp. da {{record.name}}", "stage", "prospect")),
                            Map.of("type", "notify_user", "target", "owner",
                                    "title", "Nuova opportunità dal lead {{record.name}}"))),

            new WorkflowSpec("Opportunità vinta → Progetto + Fattura",
                    "Quando un'opportunità è vinta, apre il progetto e prepara la fattura.",
                    Map.of("type", "field.changed", "objectKey", "opportunity", "field", "stage"),
                    all(rule("stage", "eq", "won")),
                    List.of(
                            Map.of("type", "create_record", "objectKey", "project",
                                    "map", Map.of("name", "Progetto {{record.name}}", "status", "planned", "budget", "{{record.amount}}")),
                            Map.of("type", "create_record", "objectKey", "invoice",
                                    "map", Map.of("number", "FT-{{record.name}}", "amount", "{{record.amount}}", "status", "draft")),
                            Map.of("type", "notify_user", "target", "owner",
                                    "title", "Opportunità vinta: {{record.name}} 🎉"))),

            new WorkflowSpec("Progetto creato → reminder scadenza",
                    "Programma un promemoria 3 giorni prima della scadenza del progetto (visibile a calendario).",
                    Map.of("type", "record.created", "objectKey", "project"),
                    all(rule("deadline", "is_set")),
                    List.of(Map.of("type", "create_reminder", "title", "Scadenza progetto {{record.name}}",
                            "dueField", "deadline", "offsetDays", 3, "_objectKey", "project"))),

            new WorkflowSpec("Attività con scadenza → reminder + evento",
                    "Ogni attività con scadenza crea un promemoria e un evento in calendario.",
                    Map.of("type", "record.created", "objectKey", "task"),
                    all(rule("dueDate", "is_set")),
                    List.of(
                            Map.of("type", "create_reminder", "title", "Attività: {{record.title}}",
                                    "dueField", "dueDate", "_objectKey", "task"),
                            Map.of("type", "create_calendar_event", "title", "{{record.title}}", "startField", "dueDate"))),

            new WorkflowSpec("Ticket urgente → escalation",
                    "Notifica immediata e promemoria di escalation a 4 ore per i ticket urgenti.",
                    Map.of("type", "record.created", "objectKey", "ticket"),
                    all(rule("priority", "eq", "urgent")),
                    List.of(
                            Map.of("type", "notify_user", "target", "owner", "title", "Ticket URGENTE: {{record.subject}}"),
                            Map.of("type", "create_reminder", "title", "Escalation ticket {{record.subject}}",
                                    "delayHours", 4, "_objectKey", "ticket"))),

            new WorkflowSpec("Fatture scadute (giornaliero)",
                    "Ogni mattina marca come scadute le fatture non pagate oltre la data e avvisa.",
                    Map.of("type", "schedule", "objectKey", "invoice", "cron", "0 6 * * *"),
                    all(rule("status", "neq", "paid"), rule("dueDate", "lt", "today")),
                    List.of(
                            Map.of("type", "update_record", "set", Map.of("status", "overdue")),
                            Map.of("type", "notify_user", "target", "owner", "title", "Fattura scaduta: {{record.number}}"))),

            new WorkflowSpec("Rinnovo abbonamento tra 7 giorni",
                    "Promemoria automatico una settimana prima del rinnovo abbonamento.",
                    Map.of("type", "schedule", "objectKey", "subscription", "cron", "0 7 * * *"),
                    all(rule("renewalDate", "in_days", 7)),
                    List.of(Map.of("type", "create_reminder", "title", "Rinnovo {{record.name}}",
                            "dueField", "renewalDate", "_objectKey", "subscription"))),

            new WorkflowSpec("Contratto in scadenza (30 giorni)",
                    "Promemoria di rinnovo 30 giorni prima della fine del contratto.",
                    Map.of("type", "schedule", "objectKey", "contract", "cron", "0 7 * * *"),
                    all(rule("endDate", "in_days", 30)),
                    List.of(Map.of("type", "create_reminder", "title", "Rinnovo contratto {{record.title}}",
                            "dueField", "endDate", "offsetDays", 7, "_objectKey", "contract"))),

            new WorkflowSpec("Preventivo accettato → Contratto",
                    "Quando un preventivo è accettato, genera la bozza di contratto.",
                    Map.of("type", "field.changed", "objectKey", "quote", "field", "status"),
                    all(rule("status", "eq", "accepted")),
                    List.of(Map.of("type", "create_record", "objectKey", "contract",
                            "map", Map.of("title", "Contratto {{record.number}}", "value", "{{record.total}}", "status", "active")))),

            new WorkflowSpec("Opportunità persa → analisi",
                    "Crea un'attività di analisi quando si perde un'opportunità.",
                    Map.of("type", "field.changed", "objectKey", "opportunity", "field", "stage"),
                    all(rule("stage", "eq", "lost")),
                    List.of(Map.of("type", "create_task", "title", "Analisi perdita: {{record.name}}", "delayHours", 48))),

            new WorkflowSpec("Benvenuto nuovo contatto",
                    "Invia email di benvenuto ai nuovi contatti con indirizzo email.",
                    Map.of("type", "record.created", "objectKey", "contact"),
                    all(rule("email", "is_set")),
                    List.of(
                            Map.of("type", "send_email", "template", "welcome", "to", "{{record.email}}", "subject", "Benvenuto in NorthLeap"),
                            Map.of("type", "create_task", "title", "Primo contatto con {{record.firstName}}", "delayHours", 24))),

            new WorkflowSpec("Nuova fattura → reminder pagamento",
                    "Promemoria di verifica pagamento alla scadenza della fattura.",
                    Map.of("type", "record.created", "objectKey", "invoice"),
                    all(rule("dueDate", "is_set")),
                    List.of(Map.of("type", "create_reminder", "title", "Verifica pagamento {{record.number}}",
                            "dueField", "dueDate", "_objectKey", "invoice")))
    );

    private final WorkflowRepository workflowRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (WorkflowSpec spec : WORKFLOWS) {
            if (workflowRepository.findByName(spec.name()).isPresent()) {
                continue;
            }
            Workflow wf = new Workflow();
            wf.setName(spec.name());
            wf.setDescription(spec.description());
            wf.setTrigger(spec.trigger());
            wf.setConditions(spec.conditions());
            wf.setActions(spec.actions());
            wf.setActive(true);
            workflowRepository.save(wf);
        }
    }
}
