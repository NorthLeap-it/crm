# 05 — Workflow engine

Secondo modulo per complessità dopo il motore dinamico. L'originale gira su BullMQ (coda Redis)
con un worker asincrono; replichiamo lo stesso comportamento osservabile con strumenti Spring
nativi, senza necessariamente introdurre una coda esterna fin da subito.

## Entity

**Workflow**: `id`, `name`, `description`, `isActive`, `trigger` (JSON — `{type, objectKey?,
cron?, field?}`), `conditions` (JSON, nullable — albero AND/OR legacy, fallback), `actions`
(JSON — lista di step, fallback lineare), `graph` (JSON, nullable — `{nodes, edges}` per
l'editor visuale a grafo), `createdAt`/`updatedAt`.

**WorkflowRun**: log di ogni esecuzione. `id`, `workflowId` (FK), `status` (enum PENDING/
RUNNING/SUCCESS/FAILED), `input` (JSON), `steps` (JSON — log per-step), `error` (nullable),
`startedAt`, `finishedAt`.

Stessa tecnica per i campi JSON vista nel motore dinamico (`@JdbcTypeCode(SqlTypes.JSON)`).

## I due modi di eseguire un workflow

L'originale supporta sia un formato "legacy" lineare (`trigger` + `conditions` + `actions[]`,
eseguito in sequenza) sia un formato "a grafo" (`graph.nodes`/`graph.edges`, con branch/loop/
delay) per l'editor visuale nel frontend. Se `graph.nodes` è popolato, ha precedenza; altrimenti
fallback al formato lineare. Manteniamo lo stesso doppio supporto — è un comportamento visibile
dal frontend (che probabilmente erediteremo o ricostruiremo identico), non un dettaglio interno
da semplificare.

## Valutazione condizioni (`ConditionEvaluator`)

Classe pura, senza stato, equivalente a `condition.ts`. Dato un albero `{all: [...]}` o
`{any: [...]}` di regole (`{field, op, value}`) e un Record, ritorna true/false. Operatori da
supportare: `eq`, `neq`, `gt`, `lt` (con caso speciale `value: "today"` per confronti con la
data corrente), `contains`, `is_set`, `in_days` (per i reminder "scade tra N giorni").

Stessa classe gestisce l'interpolazione di template stringa (`{{record.campo}}` → valore reale
dal Record, supportando path annidati come `{{record.data.campo}}`).

## Esecutore a grafo (`GraphWorkflowRunner`)

Equivalente a `graph-runner.ts`. Cammina il grafo a partire dal nodo `trigger`, seguendo gli
archi (`edges`) in base al tipo di nodo corrente:
- **action**: esegue l'azione (delegando a `WorkflowActionExecutor`, vedi sotto), poi segue
  l'arco uscente singolo.
- **condition/branch**: valuta la condizione, segue l'arco con handle `true` o `false`.
- **loop**: itera su una lista (presa da `node.data.items` o da un campo del record), per
  ogni elemento segue l'arco `each`, poi al termine l'arco `done`.
- **delay**: attende N millisecondi (con un cap massimo, l'originale usa 60 secondi, per non
  bloccare indefinitamente un worker) poi prosegue.

Guardia anti-loop-infinito: un contatore di step massimo (l'originale usa 500), decrementato ad
ogni nodo visitato; se si esaurisce, l'esecuzione si interrompe. Mantenerla identica.

## Esecutore di azioni (`WorkflowActionExecutor`)

Un metodo con uno switch sul `type` dell'azione, equivalente a `WorkflowEngine.execute()`
nell'originale. Azioni da implementare: `update_record`, `create_record` (con interpolazione
template sui valori), `create_link`, `create_task`/`create_reminder` (con logica di calcolo
della data di scadenza da campo del record + offset, o da delay fisso), `create_calendar_event`,
`notify_user` (crea una `Notification` e pubblica un evento per il realtime), `send_email`
(integrazione HTTP con provider esterno tipo Resend — vedi nota sotto), `send_webhook`/`call_api`
(richiesta HTTP esterna).

**Punto di sicurezza non negoziabile**: la guardia anti-SSRF (`isSafeUrl` nell'originale) va
riportata identica. Prima di fare qualsiasi richiesta HTTP verso un URL fornito da
configurazione di un workflow (quindi potenzialmente controllato da un utente con permessi di
scrittura sui workflow), bisogna verificare che non punti a host interni: localhost, IP privati
(10.x, 172.16-31.x, 192.168.x), link-local (169.254.x), domini `.local`/`.internal`. Senza
questo controllo, un workflow malevolo o mal configurato potrebbe essere usato per accedere a
servizi interni della rete (incluso, potenzialmente, l'endpoint dei metadati cloud su molte
piattaforme — un vettore di attacco SSRF noto e serio). Va scritta come funzione di utilità
testata a parte, non come controllo informale dentro l'azione HTTP.

## Trigger: come si attivano i workflow

Tre famiglie, stesso comportamento dell'originale:

1. **Trigger da evento record** (`record.created`/`record.updated`/`record.deleted`/
   `field.changed`): un listener (`@EventListener` Spring, equivalente a `@OnEvent` NestJS)
   ascolta gli eventi pubblicati dal modulo Record (Fase 3). Per ogni evento, cerca i workflow
   attivi con trigger compatibile (stesso tipo, stesso `objectKey` se specificato; per
   `field.changed`, controlla che il campo indicato sia effettivamente cambiato confrontando
   stato prima/dopo).
2. **Trigger schedulati** (`schedule`, con espressione cron): un componente con
   `@Scheduled(cron = ...)` — nell'originale è un singolo job ogni ora che scandisce tutti i
   workflow con trigger `schedule` e valuta le rispettive condizioni; manteniamo lo stesso
   approccio "polling orario" piuttosto che registrare dinamicamente un cron Spring per ogni
   workflow (più semplice, e i cron dei workflow originali sono comunque a granularità
   giornaliera, non hanno bisogno di precisione al minuto).
3. **Esecuzione manuale**: endpoint `POST /workflows/{id}/run`, protetto da permesso `execute`
   (la X di RBAC).

## Come gestire l'asincronia senza BullMQ/Redis

Per l'MVP, la proposta è: invece di una vera coda esterna, usare `@Async` di Spring (con un
`TaskExecutor` dedicato, pool di thread configurabile) per non bloccare la request HTTP che ha
generato l'evento trigger. Ogni esecuzione di workflow gira su un thread del pool, scrive il
suo `WorkflowRun` con stato RUNNING poi SUCCESS/FAILED.

Limite consapevole di questo approccio rispetto a BullMQ: nessuna persistenza della coda stessa
(se l'app si riavvia mentre un workflow è "in coda" — non ancora preso da un thread — quel job
si perde, non c'è retry automatico). Per un MVP è un compromesso accettabile; se in futuro
servirà robustezza maggiore (retry, backoff, persistenza della coda), si valuta introdurre
Spring AMQP con RabbitMQ, o Redis con una libreria equivalente a BullMQ per Java. Non è una
decisione da prendere ora, va segnalata come "rivisitare se il volume di workflow cresce".

## Ordine di implementazione

1. Entity `Workflow`, `WorkflowRun`.
2. `ConditionEvaluator` (con test unitari sugli operatori, incluso `in_days` e il caso
   speciale `lt` su "today").
3. `WorkflowActionExecutor` con tutte le azioni — **incluso il controllo SSRF da subito**, non
   come refactor successivo.
4. `GraphWorkflowRunner`.
5. `WorkflowEngine` (orchestratore: ascolta eventi, valuta trigger, lancia esecuzioni async,
   scrive `WorkflowRun`).
6. Scheduler per i trigger `schedule`.
7. `WorkflowsController` (CRUD + run manuale).
8. Seed delle automazioni di default (le ~12 dell'originale: lead qualificato → opportunità,
   opportunità vinta → progetto+fattura, ecc.) — utile soprattutto per verificare che
   l'esecutore funzioni end-to-end su casi realistici, non solo su test unitari isolati.
