# NorthLeap CRM — Contesto di sessione (handoff per nuova chat)

> Documento di riepilogo, scritto per essere incollato come contesto in una nuova chat.
> Copre stato del progetto, convenzioni, lavoro svolto in questa sessione, gotcha imparati e backlog.
> **Non è committato** (sta nella root `crm/`, untracked, come `DB.md`/`ROADMAP.md`).

---

## 1. Cos'è il progetto

CRM full-stack, **migrazione** di un CRM di riferimento (NestJS + Prisma + frontend React/Next,
chiamato `crm_geck`) verso un nuovo stack:

- **Backend**: Spring Boot 4.1 / Java 21 / Hibernate 7.2 / PostgreSQL. Cartella `crm/backend`.
- **Frontend**: Angular 22 (standalone + signals) / daisyUI 5 + Tailwind 4 / lucide-angular.
  Cartella `crm/frontend`.

### Architettura: motore "metadata-driven" (il cuore)
Niente tabelle fisse tipo `contacts`/`companies`. C'è **una sola** entità generica `Record`:
- `Record` (`entities/Record.java`): colonne "vere" minime (`objectType` FK, `title`/`status`
  denormalizzati, `ownerId` per RBAC, `isDeleted` soft-delete) + **`data` come `jsonb`**
  (`@JdbcTypeCode(SqlTypes.JSON)` → `Map<String,Object>`). I valori dei campi dinamici stanno nel JSON.
- `ObjectType` = definizione di un "tipo" (key/label/icon/color). È "la tabella, come riga DB".
- `FieldDef` = definizione di un campo (type tra 39 `FieldType`, required, options, config, icon…).
  È "la colonna, come riga DB".
- Flusso richiesta: `POST /api/records/{key}` → risolve l'`ObjectType` dalla key → `RecordValidator`
  valida/coerce il JSON campo per campo secondo i `FieldDef` → salva in `jsonb`. Un solo
  controller/service serve **tutti** i tipi.
- Query su jsonb: `RecordQueryService` + `RecordFilterCompiler` costruiscono SQL nativo
  (`(data->>'campo')::numeric > :p`), con `SAFE_KEY` `^[a-zA-Z0-9_]{1,64}$` anti-injection e
  valori sempre come parametri JDBC.
- Eventi `RecordCreated/Updated/Deleted` → agganciano il motore workflow.

**Conseguenza pratica**: aggiungere un tipo o un campo = inserire una riga `ObjectType`/`FieldDef`,
senza toccare il DB né scrivere codice. È esattamente ciò che fa la UI "Oggetti" nei Settings.

---

## 2. Stato generale

- **Backend: funzionalmente completo** rispetto al reference (tutti i moduli portati: auth, RBAC,
  motore dinamico, pages/charts/analytics, workflow, webhooks, apikey, files, notifications, audit,
  relations, users/invite, + hardening). Vedi `crm/backend/CLAUDE.md` per il dettaglio fase per fase.
- **Frontend: a buon punto**. Con il lavoro di questa sessione ha ora UI per quasi tutte le
  capability backend.

### Auth (importante per capire le chiamate)
- JWT in **cookie httpOnly** (mai in localStorage). `AuthResponse` non porta token.
- **CSRF attivo** (cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN`), gestito dall'interceptor Angular.
- `auth.service.ts` tiene solo lo stato "chi è loggato" (signal `user`), popolato da `GET /api/auth/me`.

---

## 3. Convenzioni e filosofia dell'utente (RISPETTARE SEMPRE)

- **HTML e TS separati**: ogni componente nuovo ha `templateUrl: './x.html'` + file `.html` a parte
  (NON template inline). È una richiesta esplicita e ricorrente dell'utente.
- **Angular moderno**: signals ovunque (`signal`/`computed`/`toSignal`/`input.required`/`output`/
  `toObservable`), standalone components, new control flow (`@if`/`@for`/`@switch`), Reactive Forms
  `fb.nonNullable.group`, `takeUntilDestroyed(destroyRef)`, lazy `loadComponent` nelle route.
  Pattern dati idiomatico: `reload$ Subject → startWith → switchMap(service) → toSignal`.
- **`ng generate`**: l'utente preferisce scaffoldare componenti/servizi con la CLI invece che a mano
  (memoria salvata). Es. `ng generate component pages/x --flat --skip-tests --style=none`.
- **Commit**: stile single-line `Tipo: sommario` (es. `Feat: ...`, `Refactor: ...`, `Style: ...`),
  **NESSUN trailer `Co-Authored-By`**. Commit **granulari**, uno per cambiamento logico ("non troppa
  roba"). Spesso: un commit backend + uno frontend per la stessa feature.
- **Verifica prima di committare**: backend `./gradlew.bat compileJava test --tests "..."`,
  frontend `npm run build`. Entrambi verdi prima del commit.

### FILE DA NON COMMITTARE (non scritti dall'assistente)
`.gitignore`, `README.md`, `.idea/`, `DB.md`, `ROADMAP.md`, `frontend/CLAUDE.md`,
`md/06-FRONTEND-OVERVIEW.md`, `tests.http` (contiene credenziali di test — fuori apposta),
`frontend/src/app/services/theme.service.ts` (modificato dall'utente, mai rivisto dall'assistente).
Questo stesso file `SESSION-CONTEXT.md` è untracked, non committarlo.

### Gotcha Docker (RICORRENTE!)
Il backend gira in container `crm-backend` su **:8080**. L'immagine è uno **snapshot del codice al
build**: dopo OGNI modifica backend bisogna **rebuildare**:
```
docker compose up -d --build backend
```
Con `spring.jpa.hibernate.ddl-auto=update` le colonne nuove (nullable) vengono aggiunte da sole al
riavvio, niente migration. Sintomo classico di immagine stale: un endpoint nuovo dà 500/404 mascherato.

### Note tecniche da ricordare
- **Jackson**: mai tipare un campo di colonna JSON come `JsonNode` → usare `Object`/`Map`/`List`.
- **Lombok boolean serialization**: un getter `isXxx` di una entity Lombok serializza in JSON SENZA
  prefisso (`isSystem` → `system`, `isActive` → `active`). MA un componente di `record` Java mantiene
  il nome (`isActive` resta `isActive`). Verificare sempre la forma reale lato frontend.
- **lucide-angular**: i nomi delle icone cambiano tra versioni. PRIMA di importarne una, verificare
  che esista negli export del pacchetto installato:
  `grep -E "as NomeIcona }" node_modules/lucide-angular/icons/lucide-icons.d.ts`.
  Esempi: `Home`/`PieChart`/`BarChart`/`BarChart3`/`LineChart`/`CheckCircle` NON esistono più
  (sono `House`/`ChartPie`/`ChartColumn`/`ChartLine`/`CircleCheck`).

---

## 4. Lavoro svolto in QUESTA sessione (cronologico, con commit)

Tutti i commit sono sul branch `main` (ahead di origin, non ancora pushati).

1. **Sidebar resize "alla VS Code"** (`be08152` *Refactor: make the sidebar resize follow the cursor
   1:1 like VS Code*): il resize della sidebar sembrava "lento/robotico". Causa: la transizione CSS
   sulla width restava attiva durante il drag (la classe `transition-[width]` con parentesi nel
   class-binding era fragile). Fix: `[style.transition]="dragging() ? 'none' : 'width 0.15s ease-out'"`
   + **scrittura diretta della width sul DOM durante il drag** (fuori da Angular, niente CD per-frame)
   + rimosso lo snap magnetico (resta solo il collasso al rail sotto soglia).
   File: `components/sidebar/sidebar.ts` + `.html`.

2. **Gestione campi object-type** (`db22284` backend guard + `33f7f8a` frontend UI): in Settings →
   Oggetti, espandendo un oggetto puoi **aggiungere/eliminare** campi. I campi **`required` sono
   intoccabili** (guard backend in `ObjectsService.updateField/removeField` → `BadRequestException`,
   + UI che non mostra i bottoni). Test `ObjectsServiceTest`. Form campo: key/label/type(39)/required.

3. **Pulizia codice dell'utente** (commit `0b6e15e`, `db276e6`, `cd45109`, `db632a9`, `e7ef6a1`):
   estrazione interfacce in `models/I*.ts` (analytics points, auth/me/onboarding), template
   chart/drawer in file `.html`, rimozione `bottom-nav`, commenti admin service. **Durante la review
   trovati 5 bug reali dell'utente e corretti**: `template:` invece di `templateUrl:` su chart/drawer
   (renderizzava la stringa letterale), import mancanti in `auth.service.ts`, `export` mancante su
   `IAuthResponse`/`IOnboardingPayload`.

4. **Icona sul campo** (`51ded2d` backend + `277222c` frontend): aggiunta colonna `icon` a `FieldDef`
   (l'utente l'aveva messa `nullable=false` → **bug critico**, corretto a nullable, altrimenti
   l'`ALTER TABLE` falliva sulle righe esistenti). Picker icona a griglia nel form campo.

5. **Edit campi + icona object-type** (`a92c9c9`): bottone **Modifica** sui campi non-obbligatori
   (riusa il form, chiave bloccata, `PATCH .../fields/{key}`); + picker icona anche nel form di
   creazione dell'object type (mostra l'icona in sidebar).

6. **Set icone ampliato** (`4a55d0a`): da 18 a **~80 icone** in `core/object-icons.ts`. Aggiunto
   `export const ICON_KEYS = Object.keys(ICONS)`. Tutti i nomi lucide **verificati uno a uno** contro
   gli export del pacchetto. Picker con `max-h-40 overflow-y-auto`.

### Implementazione "tutto ciò che mancava" lato frontend (richiesta esplicita)

7. **Tab Webhooks + Audit** (`82b585c`): in Settings. Webhooks = CRUD (secret mostrato una volta);
   Audit = sola lettura con filtri resource/resourceId. Modelli in `models/admin.ts`, metodi in
   `admin.service.ts` (`listWebhooks/createWebhook/removeWebhook/listLogs`).

8. **Files/allegati** (`3c76982` backend + `17e5a46` frontend): **aggiunto endpoint backend mancante**
   `GET /api/files?recordId=` (repository `findByRecordIdOrderByCreatedAtDesc` + service + controller).
   Frontend: `file.service.ts` + sezione "Allegati" in `record-detail` (upload multipart / lista /
   download via `<a href>`). NB: niente delete (il backend non ha endpoint delete-file).

9. **Editor opzioni/relazioni sui campi** (`a4442ba`): nel form campo, per SELECT/MULTISELECT/STATUS/
   TAGS appare una textarea **opzioni** (`valore` o `valore|etichetta` per riga → `options[]`); per
   RELATION/LOOKUP appaiono **oggetto target** (`config.targetObject`) + **multiplo** (`config.multiple`).
   `object-type.service.ts` ora ha l'interfaccia `FieldUpsert` con `options`/`config`.

10. **Builder di grafici** (`efc55d4`): pagina `/charts` (link in sidebar). `chart.service.ts`
    (list/run/create/remove), `models/chart.ts`, componente `ChartCard` (esegue `/run` e renderizza:
    chart.js per BAR/LINE/PIE/FUNNEL, numero per KPI, tabella per TABLE), pagina `Charts` con form di
    creazione. `ChartCard` usa il pattern `toObservable(input) → switchMap(run) → toSignal`.

**Scoperta**: le **Notifiche erano già fatte** (pannello dropdown completo nel topbar, con polling
60s, unread badge, mark-all-read) — un grep iniziale aveva dato un falso negativo.

---

## 5. Stato attuale del frontend (mappa)

- **Pagine** (`frontend/src/app/pages/`): dashboard, login, onboarding, record-list, record-detail,
  search, settings, **charts** (nuova).
- **Componenti** (`components/`): chart, drawer, dynamic-form, relation-input, sidebar, topbar, ui.
- **Tab Settings** (`pages/settings/tabs/`): objects, users, apikeys, workflows, **webhooks**,
  **audit**, roles, company.
- **Servizi** (`services/`): admin, analytics, auth, chart, file, i18n, notification, object-type,
  records, theme, workspace.
- **Registro icone**: `core/object-icons.ts` (`resolveObjectIcon`, `ICON_KEYS`, ~80 icone kebab-case).

---

## 6. Cosa manca DAVVERO rispetto al reference (verificato sul codice del reference)

La repo di riferimento è già sul disco (cartella sorella, **fuori** dal repo CRM):
`C:\Users\alessio.ferrari\Desktop\crm_geck_reference` (ha `apps/backend` e `apps/frontend`,
frontend in **React/Next**). Si legge con percorsi assoluti, niente da clonare/configurare.

Confronto reale dei frontend → mancano **4 pezzi di UI** (backend già pronto a supportarli):

| Gap | Evidenza nel reference | Ordine consigliato |
|---|---|---|
| **Filter Builder** (filtri avanzati visuali sulle liste) | `components/FilterBuilder.tsx`, usato in `RecordList` | 1° (valore immediato) |
| **Vista Kanban/Board** sui record | `RecordList.tsx` ha `view === 'board'` oltre a `'list'` | 2° |
| **Vista Calendario** | pagina `CalendarView.tsx` | 3° |
| **Workflow Editor** (creazione/modifica visuale del grafo) | `components/WorkflowEditor.tsx` in `Settings` | 4° (il più grosso) |

**Note**:
- **i18n NON è un gap col reference** (il reference non ha traduzioni): è una **aggiunta dell'utente**
  rimasta a metà. Service `i18n.ts` + dizionari `i18n/it.ts`/`en.ts` esistono, ma `i18n.t()` è usato
  in **1 solo template su 23** (il topbar). Cablarlo = lavoro meccanico ma diffuso.
- **Cose che NOI abbiamo in più** del reference: builder grafici `/charts`, tab Webhooks/Audit.
- **Realtime notifiche**: scelta MVP deliberata (polling, niente Socket.io), documentata.

---

## 7. Backlog / prossimi passi

1. **Filter Builder** UI (backend `RecordQueryService`/`RecordFilterCompiler` già pronto — supporta
   gruppi AND/OR annidati, operatori, cast per tipo).
2. **Vista Kanban** sui record (raggruppamento per `status` o per un campo select).
3. **Vista Calendario** (record con campi data).
4. **Workflow Editor** visuale (trigger/condizioni/azioni/grafo — backend workflow engine completo).
5. **Cablare i18n** in tutti i template (passare i testi a `i18n.t()`).

Suggerimento: una feature alla volta, signal + html/ts separati, build verde, commit granulare
(backend e frontend separati quando entrambi toccati), e **rebuild docker backend** se si tocca il server.

---

## 8. Comandi utili

```
# Backend (da crm/backend)
./gradlew.bat compileJava
./gradlew.bat test --tests "*NomeTest"
docker compose up -d --build backend      # da crm/ — RIBUILDA dopo modifiche backend
docker compose logs backend --since 60s   # check avvio

# Frontend (da crm/frontend)
npm run build                              # verifica build (verde prima del commit)
ng serve                                   # dev server :4200

# Verifica nome icona lucide prima di importarla (da crm/frontend)
grep -E "as NomeIcona }" node_modules/lucide-angular/icons/lucide-icons.d.ts
```

Backend in esecuzione su **:8080** (Docker), frontend dev su **:4200**. Swagger/OpenAPI:
`/swagger-ui.html` e `/v3/api-docs` su :8080.
