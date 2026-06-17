# 03 — Motore dinamico (ObjectType / FieldDef / Record / RecordLink)

Questo è il modulo più importante e più diverso da qualsiasi CRUD "normale" che abbiamo scritto
finora. Vale la pena leggerlo con calma prima di iniziare a scrivere codice, perché le decisioni
prese qui condizionano tutto il resto (pagine, workflow, analytics si appoggiano sui Record).

## L'idea di fondo

Invece di avere tabelle fisse `contacts`, `companies`, `tickets` ecc., esiste UNA tabella
`Record` generica. Ogni riga ha un `objectTypeId` che dice "che tipo di cosa sei" (un contatto?
un'azienda? un ticket?), e i valori effettivi dei suoi campi stanno in una colonna `data` di
tipo JSON. Quali campi un dato `ObjectType` ha, con che tipo, se sono obbligatori, le opzioni
valide per le select — tutto questo è descritto da righe in `FieldDef`, non dallo schema SQL.

Il vantaggio: l'utente finale del CRM può creare un nuovo "tipo di record" (es. "Evento
Formativo") con i suoi campi custom, senza che nessuno scriva una riga di codice o faccia una
migration. Lo svantaggio: perdiamo i vincoli che il database normalmente ci dà gratis (un
campo `email` non è automaticamente "di tipo email" per il DB, dobbiamo validarlo noi a mano in
applicazione ad ogni scrittura).

## Entity

### ObjectType
`id`, `key` (univoco, slug es. "contact"), `label`, `pluralLabel`, `icon`, `color`, `isSystem`
(i tipi seed non sono eliminabili), `isEnabled`, `order` (per l'ordinamento nel menu).
Relazione `@OneToMany` verso `FieldDef` e verso `Record`.

### FieldDef
Descrive un campo di un ObjectType. Campi principali: `objectTypeId` (FK), `key` (univoco
*dentro* l'ObjectType, vincolo composito `(objectTypeId, key)`), `label`, `type` (enum
`FieldType`), poi un gruppo di flag (`required`, `unique`, `isIndexed`, `isReadonly`,
`isHidden`, `isFilterable`, `isSortable`, `showInList`), poi vincoli di validazione opzionali
(`min`, `max`, `step`, `pattern`), poi `options` (JSON — per select/multiselect/status:
array di `{value, label, color}`) e `config` (JSON — per relation/lookup: che ObjectType punta,
se è una relazione multipla).

L'enum `FieldType` ha ~35 valori (TEXT, NUMBER, CURRENCY, DATE, RELATION, FORMULA, ROLLUP,
USER, STATUS, ecc. — vedi lo schema Prisma originale per la lista completa). Lo replichiamo
identico come `enum` Java.

### Record
`id`, `objectTypeId` (FK), `title` (String, denormalizzato — calcolato dal validatore al
salvataggio per essere ricercabile/ordinabile senza aprire il JSON), `status` (String,
nullable, denormalizzato per le viste kanban), `ownerId` (FK User, nullable, per lo scope
RBAC `OWN`), `data` (JSON, vedi sezione dedicata sotto), `isDeleted` (soft-delete, mai
cancellare fisicamente per motivi di audit/GDPR-export), `createdAt`/`updatedAt`.

Indici da creare esplicitamente: `(objectTypeId, status)`, `(objectTypeId, ownerId)`, `(title)`
— stessi indici dell'originale Prisma, servono per le query di lista che filtrano sempre
almeno per tipo.

### RecordLink
Relazione tipizzata generica fra due Record qualsiasi. `id`, `sourceId` (FK Record),
`targetId` (FK Record), `relationKey` (String — es. "contact.company", libera, non vincolata a
un enum perché le relazioni possono essere definite dall'utente), `data` (JSON, nullable —
metadati della relazione stessa, es. quantità in una relazione preventivo↔prodotto),
`createdAt`. Vincolo unico `(sourceId, targetId, relationKey)`.

## Il campo `data` JSON: come lo mappiamo

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "json")
private Map<String, Object> data;
```

Hibernate useià l'ObjectMapper Jackson già nel contesto Spring per serializzare/deserializzare
automaticamente. Lato Java lavoriamo con `Map<String, Object>`, non con una classe tipizzata —
non potremmo avere una classe fissa, dato che la "forma" del JSON dipende dai `FieldDef`
dell'ObjectType, che sono dinamici per definizione.

## Il validatore dinamico (`RecordValidator`)

È l'equivalente di `record-validator.ts` nell'originale, e probabilmente la classe più
delicata di tutto il backend. Dato un `ObjectType` (con i suoi `FieldDef` caricati) e una
`Map<String, Object>` di input grezzo, deve:

1. Per ogni `FieldDef`, leggere il valore corrispondente dall'input.
2. Se manca e c'è un `defaultValue`, usare quello.
3. Se manca e il campo è `required`, lanciare un errore di validazione con messaggio chiaro
   (nome del campo, non solo "campo X mancante").
4. Se presente, **coercere e validare** il valore secondo il `type` del campo: i numeri vanno
   parsati e controllati contro `min`/`max`; le stringhe contro `min`/`max` (lunghezza) e
   `pattern` (regex); le email contro un regex fisso; gli URL provando a costruire un
   `java.net.URI`; i SELECT/STATUS controllando che il valore sia tra le `options` ammesse; i
   MULTISELECT/TAGS normalizzati come liste; i RELATION/LOOKUP normalizzati come singolo
   ID o lista di ID secondo il flag `multiple` in `config`; le DATE/DATETIME parsate e
   normalizzate a ISO-8601.
5. Ritornare la mappa "pulita" (solo campi validi, valori coercitati ai tipi giusti), pronta
   per essere salvata in `Record.data`.

Punto di attenzione: questo codice non è "tipizzato" nel senso normale Java — lavora su
`Object` e fa instanceof/parsing manuale per ogni `FieldType`. È inevitabile dato il design,
ma vale la pena isolarlo bene in una classe sola con un metodo pubblico chiaro
(`validate(List<FieldDef> fields, Map<String, Object> rawData) -> Map<String, Object>`), così
il resto del codice non deve mai preoccuparsi dei dettagli di coercizione.

Vanno scritte anche le due funzioni di supporto equivalenti a `deriveTitle`/`deriveStatus`
dell'originale: deducono `Record.title` e `Record.status` dal contenuto di `data` quando non
specificati esplicitamente (cercano chiavi comuni come `title`/`name`/`subject`/`number` per il
titolo, e il primo campo di tipo STATUS per lo stato).

## Query dinamiche e filtri

L'originale ha un `filter-builder.ts` che compila un albero di condizioni AND/OR (alcune sui
campi nativi `title`/`status`/`ownerId`/`createdAt`/`updatedAt`, altre sui campi dentro il JSON
`data` via path Prisma) in una query Prisma. In Spring, l'equivalente è costruire dinamicamente
una `Specification<Record>` (Spring Data JPA Specifications) oppure usare query native con
`JSON_EXTRACT`/`JSON_UNQUOTE` per i campi dentro `data`.

Raccomandazione: usare **query native** (`@Query(nativeQuery = true)` o `EntityManager` con
`createNativeQuery`) per la parte che tocca il JSON, perché le Specification JPA standard non
hanno un modo pulito di esprimere "confronta un path dentro una colonna JSON" — dovremmo comunque
scendere a funzioni SQL native via `function('JSON_EXTRACT', ...)`. Meglio essere espliciti.

Punto di sicurezza da non perdere nella riscrittura: l'originale valida il nome del campo con
una regex (`/^[a-zA-Z0-9_]{1,64}$/`) prima di interpolarlo nel path JSON, per evitare injection
nel path stesso. Replichiamo la stessa whitelist quando costruiamo le query native.

Il multi-sort ha la stessa idea: colonne native ordinabili direttamente, campi dinamici che
nell'originale fanno comunque fallback su `updatedAt` (limite noto, non risolto nemmeno nella
versione Prisma — MySQL può ordinare per path JSON ma è costoso, l'originale lo evita
deliberatamente). Manteniamo lo stesso comportamento, non è un'area dove serve "migliorare"
rispetto all'originale in questa fase.

## CRUD generico sui record

Un solo controller (`RecordsController`, già nominato così per coerenza con l'originale)
gestisce TUTTI i tipi di record tramite il path `{objectKey}` parametrico:

- `GET /records/{key}` — query con filtri base in query string
- `POST /records/{key}/query` — query avanzata con filtri AND/OR + multi-sort nel body
- `GET /records/{key}/{id}` — singolo record con relazioni (outgoing/incoming RecordLink)
- `POST /records/{key}` — crea
- `PATCH /records/{key}/{id}` — aggiorna (merge del JSON esistente con quello nuovo, non
  sovrascrittura totale — replica il comportamento dell'originale)
- `DELETE /records/{key}/{id}` — soft-delete
- `POST /records/{key}/bulk` — operazioni massive su una lista di id (update o delete)
- `GET /records/search?q=...` — ricerca globale cross-object per titolo, filtrata per i
  permessi di lettura dell'Actor su ciascun ObjectType trovato

Ogni operazione di scrittura passa dal `RecordValidator`, controlla i permessi RBAC (Fase 2)
sulla risorsa = `key` (l'ObjectType), applica lo scope OWN se previsto, e scrive una riga di
`AuditLog`. Le creazioni/modifiche/cancellazioni devono anche pubblicare un evento applicativo
(`RecordCreatedEvent`, `RecordUpdatedEvent`, `RecordDeletedEvent` via
`ApplicationEventPublisher`) che il modulo Workflow (Fase 5) ascolterà per i trigger — stesso
schema dell'EventEmitter2 di NestJS, solo con l'equivalente Spring.

## Generazione automatica delle pagine

Quando un `ObjectType` viene creato o un suo `FieldDef` cambia, un `PageGeneratorService`
crea/aggiorna automaticamente due `Page` (list + detail) con un layout di default (prime 5
colonne per la lista, sezioni standard per il dettaglio). Architetturalmente è un service
separato chiamato dal service degli ObjectType dopo ogni scrittura — stesso pattern
dell'originale, nessun motivo di cambiarlo.

## Cosa implementare in che ordine, dentro questa fase

1. Entity `ObjectType`, `FieldDef` (+ enum `FieldType`), `Record`, `RecordLink` — solo lo
   schema, senza logica.
2. `RecordValidator` con test unitari per ogni `FieldType` (è la parte più a rischio di bug
   silenziosi, vale la pena testarla isolata prima di collegarla al resto).
3. Repository + query native per i filtri dinamici.
4. `ObjectsController`/`ObjectsService` (CRUD sugli ObjectType e i loro FieldDef) — più
   semplice del motore Record, conviene farlo prima per avere dati di test.
5. `RecordsController`/`RecordsService` (il CRUD generico) — qui collega RBAC, validator,
   query dinamiche, eventi.
6. `PageGeneratorService`.
7. Seed iniziale: tradurre l'elenco di ~20 ObjectType di default del file `seed.ts` originale
   (company, contact, lead, opportunity, pipeline, quote, product, contract, project, task,
   ticket, document, digital_asset, communication, calendar_event, subscription, invoice,
   payment, reminder) in un componente di inizializzazione Java equivalente.
