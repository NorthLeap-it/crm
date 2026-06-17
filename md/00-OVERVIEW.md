# Piano di migrazione: NorthLeap CRM da NestJS a Spring Boot

## Cosa stiamo migrando

Il backend originale (`crm_geck`, repo dell'amico) è un CRM **metadata-driven**: non ha tabelle
fisse per "Contatto" o "Azienda", ma un motore generico dove l'utente definisce a runtime nuovi
tipi di record (`ObjectType`), i loro campi (`FieldDef`, con ~35 tipi possibili), e il sistema
genera automaticamente liste, form, filtri e pagine di dettaglio. I dati effettivi di ogni
record vivono in una colonna JSON (`Record.data`), non in colonne dedicate.

Questo è strutturalmente diverso da un CRM "tradizionale" a entità fisse. La migrazione non è
una semplice riscrittura sintattica NestJS → Spring: alcune parti (auth, RBAC, audit) sono CRUD
relazionale puro e si traducono quasi 1:1; altre (il motore dinamico, il workflow engine) vanno
ripensate per gli strumenti che Spring/Hibernate offrono.

## Stack di destinazione

| Layer | Originale (NestJS) | Destinazione (Spring) |
|---|---|---|
| Framework | NestJS 11 | Spring Boot 4.1 |
| ORM | Prisma 6 | Spring Data JPA + Hibernate 7 |
| Database | MySQL 8 | MySQL 8 (restiamo compatibili, nessun motivo di cambiare) |
| Auth | JWT (@nestjs/jwt) + argon2 | JWT (jjwt) + Spring Security 7 + BCrypt |
| Validazione | class-validator | jakarta.validation (Bean Validation) |
| Code/coda | BullMQ + Redis | Spring Scheduling (@Scheduled) + Spring Events, valutare RabbitMQ/Redis solo se serve scalare |
| WebSocket | Socket.io | Spring WebSocket (STOMP) o SSE, da decidere nel modulo notifiche |
| API docs | Swagger (@nestjs/swagger) | springdoc-openapi |
| File upload | Multer + disco locale | Spring MVC MultipartFile + disco locale (stesso approccio) |

Manteniamo **MySQL** invece di passare a Postgres: il progetto originale lo usa per il supporto
nativo a colonne JSON con path query (`JSON_EXTRACT`), e Hibernate 6+ supporta lo stesso pattern
su MySQL via `@JdbcTypeCode(SqlTypes.JSON)`. Cambiare DB ora aggiungerebbe rischio senza benefici.

## Decisione tecnica: colonna `data` JSON

Useremo `@JdbcTypeCode(SqlTypes.JSON)` di Hibernate 6+ (disponibile e stabile anche su
Hibernate 7 / Spring Boot 4.1), mappato su `Map<String, Object>` lato Java. Hibernate userà
l'`ObjectMapper` di Spring già configurato nel contesto per la serializzazione, quindi nessuna
dipendenza aggiuntiva oltre a quelle già presenti (Jackson è incluso di default).

Perché questa scelta e non una stringa JSON serializzata a mano:
- **Query sui campi dinamici.** Il motore originale filtra/ordina sui campi dentro `data` con
  path JSON (`$.campo`). Con JSON nativo possiamo fare lo stesso con `JSON_EXTRACT` in query
  native o `@Query` JPQL/native, mantenendo le stesse capacità del filtro Prisma. Con una
  stringa serializzata a mano perderemmo questa capacità lato DB e dovremmo caricare tutto in
  memoria per filtrare in Java — inaccettabile su tabelle che potenzialmente contengono
  decine di migliaia di record.
- **Meno boilerplate.** Niente conversioni manuali `ObjectMapper.writeValueAsString` /
  `readValue` sparse nei service.
- **Limite noto da gestire**: Hibernate non crea automaticamente indici funzionali sui path
  JSON. Per i campi dinamici marcati `isIndexed=true` nel `FieldDef`, prevediamo migration SQL
  manuali con indici generati (`ALTER TABLE ... ADD INDEX ... ((CAST(data->>'$.campo' AS ...)))`)
  da gestire come task dedicato quando arriveremo a quel punto, non bloccante per l'MVP.

## Moduli e ordine di lavoro

L'ordine segue le dipendenze reali tra moduli (lo stesso ordine in cui NestJS li carica in
`app.module.ts`), non un ordine arbitrario. Ogni modulo ha un documento dedicato quando la
complessità lo giustifica; i moduli più semplici sono descritti solo qui.

### Fase 0 — Fondamenta (in corso / quasi completa)
- [x] Setup progetto Gradle, Spring Boot 4.1, struttura cartelle
- [x] Entity `User` (rivista rispetto all'originale: vedi nota sotto)
- [x] JWT access token (`JwtService`)
- [x] Login (`AuthController`/`AuthService`) — **da estendere**: nell'originale c'è anche
      refresh token con sessioni revocabili in DB, onboarding, logout. Vedi `01-AUTH.md`.
- [x] Spring Security config, filtro JWT, `UserDetailsService`

**Differenza da segnalare**: la nostra entity `User` attuale non ha ancora `avatarUrl`,
`lastLoginAt`, relazione con `Role`/`Session`. Vanno aggiunti nel modulo Auth completo.

### Fase 1 — Auth completo + Workspace
Documento: `01-AUTH.md`
- Estendere `User` (avatarUrl, lastLoginAt)
- Entity `Workspace`, `Session`, `Invite`
- Refresh token flow (access 15 min + refresh 30gg, sessioni revocabili)
- Onboarding (primo avvio: crea workspace + utente owner)
- Invito utenti via token, accettazione invito
- Logout (revoca sessione)

### Fase 2 — RBAC
Documento: `02-RBAC.md`
- Entity `Role`, `UserRole` (join), `Permission` (R/W/X + scope OWN/TEAM/ALL)
- `RbacService`: risoluzione permessi per (ruoli utente, risorsa, azione) → allowed + scope
- Annotazione custom `@RequirePerm(resource, action)` + `HandlerInterceptor` o AOP equivalente
  al guard NestJS
- Seed dei ruoli di sistema (owner/admin/manager/agent/viewer) e permessi default

### Fase 3 — Motore dinamico (il cuore del sistema)
Documento: `03-MOTORE-DINAMICO.md` — è la parte più delicata, merita lettura separata.
- Entity `ObjectType`, `FieldDef` (con enum `FieldType`, ~35 valori)
- Entity `Record` (con colonna JSON `data`) e `RecordLink` (relazioni N-N generiche)
- Validatore dinamico: dato un `ObjectType` + payload, valida/coerce ogni campo secondo il
  proprio `FieldDef` (required, min/max, pattern, opzioni select, ecc.)
- Query dinamiche: filtri AND/OR annidati su campi JSON + colonne native, multi-sort
- CRUD generico sui record (create/update/delete/query/bulk/search globale)
- Generazione automatica pagine list/detail (PageGenerator)

### Fase 4 — Pages, Charts, Analytics
Documento: solo sezione in `04-RESTO-MODULI.md` (sono CRUD relativamente semplici)
- Entity `Page`, `Chart` con `layout`/`query` JSON
- Analytics: query aggregate sui Record per le dashboard (revenue, efficiency, pipeline, activity)

### Fase 5 — Workflow engine
Documento: `05-WORKFLOW-ENGINE.md` — secondo per complessità dopo il motore dinamico.
- Entity `Workflow`, `WorkflowRun`
- Valutazione condizioni (AND/OR su campi record)
- Esecutore "a grafo" (nodi trigger/action/condition/loop/delay) — nell'originale gira su
  BullMQ; in Spring valutiamo `@Async` + `@Scheduled` per i trigger schedulati, e Spring
  Events (`ApplicationEventPublisher`) per i trigger su record.created/updated/deleted
- Azioni: update_record, create_record, create_link, notify_user, send_email, send_webhook
  (con guardia SSRF), create_task/reminder, create_calendar_event

### Fase 6 — Moduli di supporto
Documento: sezioni in `04-RESTO-MODULI.md`
- Webhooks (inbound con verifica HMAC, outbound)
- ApiKey (autenticazione alternativa via header, hash SHA-256)
- Files (upload/download, multipart, disco locale)
- Notifications (lista, mark-read; il realtime via WebSocket è una decisione separata, vedi nota)
- Audit log (query di sola lettura sui log già scritti dagli altri moduli)

## Cosa NON portiamo (per ora) / decisioni da riconfermare

- **WebSocket/Socket.io per notifiche realtime**: portarlo identico richiede Spring WebSocket
  con STOMP, che ha un setup non banale. Alternativa più semplice: polling lato frontend, o
  Server-Sent Events. Da decidere quando arriviamo al modulo Notifications.
- **BullMQ**: non esiste un equivalente diretto in Spring. La sostituzione naturale per i
  trigger "a evento" (record.created ecc.) sono gli Spring Events, già forniti dal framework.
  Per i trigger schedulati (cron), `@Scheduled`. Se in futuro serve vera scalabilità orizzontale
  della coda, si valuta RabbitMQ con Spring AMQP — non necessario per l'MVP.
- **Frontend**: fuori scope di questo piano (resta il frontend React esistente, o si valuta
  in un secondo momento se riscriverlo).

## Differenze di naming/struttura da tenere a mente

- NestJS usa `cuid()` per gli ID; noi useremo `UUID` (coerente con `User` già fatto). Se mai
  serve interoperabilità con dati esportati dal vecchio sistema, è un punto di attenzione.
- Il progetto originale ha moduli NestJS che mischiano controller+service+DTO in un solo file
  (es. `analytics.module.ts`). Noi manteniamo la separazione per cartella che abbiamo già
  impostato (`controllers/`, `services/`, `dtos/`, `entities/`, `repositories/`), più pulita
  e già coerente col resto del progetto.
