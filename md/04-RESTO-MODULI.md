# 04 — Moduli di supporto (Pages, Charts, Analytics, Webhooks, ApiKey, Files, Notifications, Audit)

Questi moduli sono CRUD relativamente diretti, senza la complessità del motore dinamico o del
workflow engine. Li raggruppo in un solo documento perché nessuno richiede decisioni
architetturali importanti — sono tutti varianti dello stesso pattern (entity + repository +
service + controller) che abbiamo già consolidato con `User`/Auth.

## Pages

Entity `Page`: `id`, `objectTypeId` (FK, nullable — null significa dashboard libera non legata
a un tipo), `key` (univoco), `label`, `type` (enum LIST/BOARD/DETAIL/DASHBOARD), `layout`
(JSON — struttura libera: colonne per le liste, sezioni per i dettagli), `isSystem`,
`isGenerated` (true se creata dal `PageGeneratorService`, Fase 3), `createdAt`/`updatedAt`.

CRUD standard con permesso RBAC sulla risorsa `"page"`. Unica cosa da rispettare: le pagine con
`isSystem=true` non sono eliminabili (stesso vincolo applicato a ObjectType/Role).

## Charts

Entity `Chart`: `id`, `pageId` (FK, nullable), `label`, `type` (enum BAR/LINE/PIE/FUNNEL/KPI/
TABLE), `query` (JSON — `{objectKey, groupBy?, aggregate?, field?, filters?}`), `createdAt`.

L'endpoint interessante è `GET /charts/{id}/run`: esegue la query dichiarativa del grafico
contro i Record dell'ObjectType indicato, raggruppa per `groupBy` (che può essere `status` o un
campo dentro `data`), aggrega (count o sum su un campo), ritorna `[{label, value}]`. Riusa la
stessa logica di lettura del JSON `data` già scritta per il motore dinamico (Fase 3) — non va
duplicata, va richiamata.

## Analytics

Non ha entity proprie: legge i `Record` esistenti e calcola serie temporali per la dashboard
(revenue, efficiency, pipeline, activity), aggregando per mese sugli ultimi 6 mesi. Stesso
discorso del modulo Charts: riusa l'accesso ai Record del motore dinamico, è solo un livello di
query/aggregazione sopra.

Endpoint: `GET /analytics/{metric}` con `metric` ∈ {revenue, efficiency, pipeline, activity}.

## Webhooks

Entity `Webhook`: `id`, `direction` (enum INBOUND/OUTBOUND), `name`, `url` (nullable, solo per
outbound), `secret` (per firma/verifica HMAC), `events` (JSON, nullable — quali eventi accetta
se inbound), `isActive`, `createdAt`.

Due endpoint da notare:
- `POST /webhooks/in/{id}` — **pubblico** (nessuna autenticazione Bearer/ApiKey, perché chi
  chiama è un sistema esterno), ma protetto da verifica HMAC: il chiamante deve fornire header
  `X-Signature` con `HMAC-SHA256(secret, body)`; se non corrisponde, 403. Questo è il punto di
  ingresso per integrazioni esterne che vogliono notificare eventi al CRM.
- Gli endpoint di gestione (list/create/delete) sono invece protetti normalmente con permesso
  `"workflow"` (l'originale riusa la stessa risorsa RBAC dei workflow per i webhook, scelta da
  mantenere per coerenza, anche se concettualmente potrebbe avere senso una risorsa dedicata).

## ApiKey

Entity `ApiKey`: `id`, `name`, `keyHash` (SHA-256, univoco), `prefix` (primi caratteri della
chiave in chiaro, solo per riconoscerla nelle liste — mai la chiave intera), `roleId` (FK,
nullable — il ruolo che definisce cosa quella chiave può fare via RBAC), `lastUsedAt`,
`expiresAt`, `revokedAt`, `createdAt`.

Punto delicato: la chiave in chiaro (`nl_` + 24 byte random in hex, nell'originale) viene
restituita **una sola volta**, alla creazione. Da quel momento esiste solo il suo hash nel DB.
Se l'utente la perde, non è recuperabile, va revocata e ricreata. Replicare lo stesso
comportamento — non aggiungere un modo di "rivedere" la chiave dopo la creazione, sarebbe una
regressione di sicurezza rispetto all'originale.

L'autenticazione effettiva via API key (header `X-Api-Key` → risoluzione dell'Actor) è già
descritta nel documento RBAC (Fase 2), perché è il filtro di autenticazione che la usa, non
questo modulo — questo modulo gestisce solo CRUD/lifecycle delle chiavi.

## Files

Nessuna sorpresa rispetto a un upload Spring MVC standard: `MultipartFile` in ingresso, salvato
su disco in una directory configurabile (`UPLOAD_DIR`), con nome file randomizzato (per evitare
collisioni e path traversal) mantenendo l'estensione originale. Entity `FileObject` traccia
metadati: `filename` originale, `mime`, `size`, `path` (il nome randomizzato salvato su disco,
non il path completo), `uploadedBy` (FK User, nullable), `recordId` (collegamento opzionale a
un Record).

Limite dimensione file: l'originale impone 25 MB lato Multer; replicarlo nella configurazione
Spring (`spring.servlet.multipart.max-file-size`).

Download: l'endpoint deve impostare `Content-Disposition: attachment` con il filename
**originale** (encodato correttamente per caratteri speciali/Unicode), non il nome
randomizzato su disco.

## Notifications

Entity `Notification`: `id`, `userId` (FK), `title`, `body` (nullable), `link` (nullable),
`readAt` (nullable), `createdAt`.

Endpoint: lista per l'utente corrente, mark-read singola, mark-all-read.

**Decisione aperta**: il realtime (l'originale usa un WebSocket gateway Socket.io per spingere
notifiche istantanee al browser) non è banale da replicare 1:1 in Spring senza introdurre
complessità extra (Spring WebSocket + STOMP richiede broker di messaggi configurato, anche se
in-memory). Per l'MVP, la proposta è partire SENZA realtime: il frontend fa polling periodico
su `GET /notifications` (es. ogni 30-60 secondi) o si aggiorna alla navigazione. Se in futuro
serve vero realtime, si valuta Spring WebSocket o Server-Sent Events (SSE, più semplice di
STOMP per un caso d'uso unidirezionale come "spingi notifiche al client"). Da confermare con
te quando arriviamo a questo modulo, non bloccante per il resto del piano.

## Audit

Nessuna scrittura propria — `AuditLog` viene scritto da TUTTI gli altri moduli (ogni mutazione
su Record, login, esecuzione workflow, ecc., come abbiamo già visto). Questo modulo espone solo
una query di lettura: `GET /logs?resource=...&resourceId=...`, filtrata e limitata (l'originale
limita a 100 righe, ordina per `createdAt desc`), protetta da permesso `"user"` (stessa
risorsa RBAC usata per la gestione utenti, per restringere l'audit a chi ha già accesso
amministrativo sugli utenti — scelta dell'originale, da mantenere per coerenza).

Entity `AuditLog`: `id`, `actorId` (nullable — può essere user o apikey, o null per azioni di
sistema), `actorType` (default "user"), `action` (stringa libera: create/update/delete/login/
run_workflow...), `resource`, `resourceId` (nullable), `diff` (JSON, nullable — per le
modifiche, before/after), `ip` (nullable), `createdAt`.
