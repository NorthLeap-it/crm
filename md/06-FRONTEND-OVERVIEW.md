# 06 — Pianificazione frontend: da React (crm_geck) ad Angular (NorthLeap CRM)

## Cosa stiamo migrando

Il frontend originale (`crm_geck/apps/frontend`) è una SPA React 19 + Vite + TypeScript +
Tailwind v4 che consuma il backend NestJS. È sottile: nessun design system pesante, componenti
pochi e riusati ovunque (`ui.tsx`, `DynamicForm`, `Drawer`), stato globale minimale (Zustand solo
per auth), tutto il resto demandato a TanStack Query. Il punto centrale è che **non esistono
pagine scritte a mano per "Contatti" o "Aziende"**: `RecordList`/`RecordDetail` sono generiche e
si adattano a qualsiasi `ObjectType` leggendo i suoi `FieldDef` a runtime. Lo stesso pattern del
backend (motore dinamico) si riflette quindi 1:1 nel frontend.

Il nuovo frontend (`crm/frontend`) è oggi uno scheletro Angular 22 vuoto: `ng new` con Tailwind 4
e Vitest preconfigurati, `app.routes.ts: Routes = []`, nessun componente applicativo
(`frontend/package.json:14-33`, `frontend/src/app/app.routes.ts:3`). Si parte da zero, ma con un
riferimento di prodotto completo da cui partire (il React) e un backend Spring già implementato
(non quello che il React si aspetta — vedi sezione disallineamenti).

**Differenza di fondo rispetto al piano di migrazione del backend**: lì si traduceva codice
NestJS verso Spring mantenendo le stesse entity/endpoint quasi 1:1. Qui la traduzione è React→
Angular sì, ma il bersaglio delle chiamate HTTP è un backend **diverso** da quello per cui il
frontend React era stato scritto. Quindi questo non è un porting di UI puro: è un porting di UI
+ un riallineamento di contratto API.

---

## A. Cosa fa oggi il frontend React (panoramica per area)

### Bootstrap, routing, auth gate
`src/main.tsx:35-58` monta un unico componente `App` con `react-router-dom` v7 (`BrowserRouter`),
avvolto da `QueryClientProvider` (TanStack Query, `staleTime: 15s`, `retry: 1`,
`main.tsx:21`). Il routing è piatto, senza nesting: `/onboarding`, `/login`, `/` (Dashboard),
`/o/:objectKey` (lista), `/o/:objectKey/:id` (dettaglio), `/search`, `/settings`, fallback `*` →
`/`. Il guard `Protected` (`main.tsx:23-29`) decide in base allo stato di `useAuth()`
(store Zustand): se `loading` mostra uno spinner, se non c'è `user` reindirizza a `/login`
salvando `location` in `state.from` (mai effettivamente riletto altrove — dead code minore),
altrimenti avvolge i children in `AppLayout`. Prima del routing, `App` chiama
`GET /auth/status` (`main.tsx:41`) per sapere se il workspace è già "onboarded" e decidere se
mostrare `/onboarding` o `/login`.

### Auth e onboarding
`src/store/auth.ts` è uno store Zustand minimale: `user`, `loading`, `load()` (chiama
`GET /auth/me` se c'è un access token in `localStorage`, pulisce i token se fallisce),
`login(email, password)` (`POST /auth/login` poi `GET /auth/me`), `logout()`
(`POST /auth/logout` con `refreshToken`, poi pulizia locale anche se la chiamata fallisce). I
token vivono in `localStorage` (`src/lib/api.ts:5-13`, chiavi `access`/`refresh`, **non
httpOnly**, leggibili da qualunque script — punto di hardening da rivalutare). `src/lib/api.ts`
implementa un fetch wrapper con refresh automatico: su 401 prova `POST /auth/refresh` una sola
volta (`retry` booleano, non un contatore — quindi al massimo un retry, niente loop) e rilancia
la richiesta originale. `src/pages/Login.tsx` e `src/pages/Onboarding.tsx` (form a due step:
workspace+brand color, poi owner+password) sono form locali senza validazione client oltre
`required`/`minLength` HTML, gestione errori con `e.message` grezzo mostrato in UI.

### Layout e navigazione
`src/layouts/AppLayout.tsx` è l'unico layout applicativo: topbar sticky (logo, ricerca, toggle
tema, campanella notifiche, logout), sidebar desktop generata dinamicamente da `useObjects()`
(`AppLayout.tsx:14,31-38` — un `NavItem` per ogni `ObjectType.isEnabled`, icona risolta via
`objectIcon(o.icon, o.key)`), drawer mobile equivalente, bottom-nav mobile a 3 voci fisse
(Home/Cerca/Impostazioni). `TopProgress` (`AppLayout.tsx:95-106`) mostra una barra di
caricamento globale basata su `useIsFetching()` di TanStack Query + un timer euristico di 350ms
sul cambio rotta (non un vero stato "router navigating").

### Tema e icone
`src/lib/theme.ts` — dark/light via classe `dark` su `<html>`, persistito in `localStorage`,
sincronizzato tra istanze tramite `useSyncExternalStore` + un pub/sub manuale (`Set<() => void>`
di listener). Applicato **prima del render** (`theme.ts:36`, side-effect a livello di modulo) per
evitare flash of unstyled theme. `src/lib/icons.tsx` mappa una stringa libera (`ObjectType.icon`
o `ObjectType.key`) a un componente `lucide-react`, con fallback a un'icona generica (`Box`).

### Motore dinamico generico — il cuore del frontend
- **`src/lib/hooks.ts`** — definisce i tipi `FieldDef`/`ObjectType` lato client (duplicati a
  mano dai DTO NestJS, nessuno share via `packages/shared` per i tipi dei record — solo Zod per
  validazione, non visto in questi file) e gli hook TanStack Query: `useObjects`, `useObject`,
  `useRecords` (query string semplice), `useRecordsQuery` (POST con filtri/sort nel body),
  `useRecord`, `useSaveRecord`/`useDeleteRecord` (mutation con invalidazione su
  `['records', key]`).
- **`src/components/DynamicForm.tsx`** — dato un array di `FieldDef`, raggruppa i campi per
  `section` (preservando l'ordine di apparizione, non un sort esplicito) e renderizza un input
  per ciascun `FieldType` (`renderInput`, righe 66-188): ~20 case espliciti (TEXT/LONGTEXT/
  BOOLEAN/SELECT/MULTISELECT/TAGS/RATING/numerici/COLOR/DATE/DATETIME/TIME/EMAIL/URL/PHONE,
  RELATION/LOOKUP delegati a `RelationInput`). Stato locale con `useState` piatto
  (`Record<string, any>`), nessuna validazione client (si fida del 400 del backend), nessun
  campo "dirty"/"touched" — il bottone Salva è sempre abilitato (tranne durante submit).
- **`src/components/FieldEditor.tsx`** — form per creare/modificare un `FieldDef` da Settings:
  switch su categorie di tipo (`HAS_OPTIONS`, `RELATIONAL`, `NUMERIC`, `TEXTUAL`) per mostrare
  sezioni di configurazione condizionali (opzioni select con colore, min/max/pattern, target
  relazione + flag multiplo). Genera la `key` slug-ificando la label se è un campo nuovo
  (`FieldEditor.tsx:159-161`).
- **`src/components/RelationInput.tsx`** — picker di record collegati con ricerca live
  (`GET /records/{targetObject}?q=...&pageSize=8`, debounce assente — ogni keystroke con
  `q.length >= 1` scatena una query, mitigato solo dalla cache TanStack Query). Risolve i record
  già selezionati con `Promise.all` di GET singoli (N+1 lato client, accettabile per i volumi di
  un picker ma da tenere a mente).
- **`src/components/FilterBuilder.tsx`** — albero di condizioni AND/OR ricorsivo
  (`FilterGroup { combinator, conditions: (Condition|FilterGroup)[] }`), 13 operatori
  (`eq/ne/contains/startsWith/endsWith/gt/gte/lt/lte/between/in/isEmpty/isNotEmpty` — coerente
  con `RecordFilterCompiler` lato Spring, **manca `nin`** nella UI pur essendo supportato dal
  backend). Type discrimination via duck-typing (`isGroup`, riga 27) sulla presenza di
  `combinator`.
- **`src/pages/RecordList.tsx`** — la pagina più complessa: vista tabella o board (kanban se
  esiste un campo `STATUS`), ricerca testuale, filtri (`FilterBuilder` in un pannello
  collassabile), multi-sort via click sull'header (ciclo asc→desc→nessuno,
  `RecordList.tsx:55-62`), selezione multipla con bulk update/delete
  (`POST /records/{key}/bulk`), creazione/visualizzazione record in un `Drawer` laterale
  (non in pagina separata — la pagina dedicata `/o/:key/:id` esiste ma è raggiunta solo dal
  link "Apri pagina intera" nel drawer). Colonne: rispetta `showInList`, fallback alle prime 6
  `FieldDef`. Caso speciale: se `objectKey === 'calendar_event'` rende `CalendarView` invece
  della lista tabellare (`RecordList.tsx:14-17`).
- **`src/pages/RecordDetail.tsx`** — vista a pagina intera di un singolo record: tutti i campi in
  `<dl>`, sezione "Collegamenti" che unisce `rec.outgoing`/`rec.incoming` (i due array di
  `RecordLink` che il backend Spring compone in `RecordsService.findOne`, vedi
  `BACKEND_OVERVIEW.md` sezione Relazioni — combacia). Modifica via modal (non drawer, diversa
  dalla lista — incoerenza minore di UX nell'originale).

### Dashboard e analytics
`src/pages/Dashboard.tsx` — griglia di KPI card (una per `ObjectType.isEnabled`, max 8, ognuna fa
una `GET /records/{key}?pageSize=1` e legge solo `data.total` per mostrare il conteggio — query
sprecata solo per un numero, nessun endpoint `count` dedicato) più 4 grafici `recharts`
(area/linea/torta/barre) alimentati da `GET /analytics/{revenue|efficiency|pipeline|activity}`
con **fallback locale a dati finti** se la chiamata fallisce (`useAnalytics`,
`Dashboard.tsx:73-79` — `.catch(() => fallback)`: in produzione mostrerebbe numeri inventati
senza alcun indicatore visivo che sono fittizi, comportamento da non portare 1:1).

### Calendario
`src/pages/CalendarView.tsx` — vista mensile pura client-side: aggrega 4 query separate
(`calendar_event`, `reminder`, `task`, `project`, ognuna `pageSize=200`) leggendo campi data
specifici per tipo (`data.start`/`data.dueAt`/`data.dueDate`/`data.deadline` — nomi hardcoded,
fragili se i campi vengono rinominati nel modello dati), le proietta su una griglia 7 colonne
calcolata a mano (no libreria calendario).

### Ricerca globale
`src/pages/Search.tsx` — input con query abilitata solo da 2+ caratteri,
`GET /records/search?q=...`, risultati cliccabili che linkano a `/o/{objectType.key}/{id}`.
Combacia con `RecordsService.globalSearch` lato Spring (ILIKE su title, min 2 char, max 30
risultati, filtrato per permesso READ).

### Settings (5 tab)
`src/pages/Settings.tsx` è il file più grande (407 righe), tab-based, senza routing per-tab
(stato locale `useState<Tab>`, quindi niente URL diretti a una tab specifica):
1. **Oggetti** — lista `ObjectType`, creazione, editor di schema (`SchemaEditor`) che aggiunge/
   modifica/rimuove `FieldDef` via `FieldEditor` in modal.
2. **Utenti** — lista utenti con badge ruoli, invito via `POST /users/invite` che ritorna un
   token mostrato una volta sola in UI (combacia con `UserService.invite` Spring).
3. **Ruoli** — lista ruoli + matrice permessi RWX/scope per risorsa
   (`GET/POST/DELETE /roles`, `PUT /roles/{id}/permissions`) — **vedi disallineamento critico
   sotto**.
4. **API Key** — crea/lista chiavi, mostra la chiave grezza una sola volta alla creazione
   (combacia con `ApiKeyService` Spring).
5. **Workflow** — lista workflow + editor visuale a grafo (`WorkflowEditor.tsx`, drag&drop di
   nodi trigger/action/condition/loop/delay su canvas SVG fatto a mano, non una libreria tipo
   React Flow), pannello inspector laterale per configurare il nodo selezionato, run manuale,
   toggle attivo/inattivo, storico ultime esecuzioni (`wf.runs`). `synthGraph`
   (`Settings.tsx:342-360`) sintetizza un grafo leggibile da un workflow "legacy" definito solo
   con `trigger`+`actions[]` lineari (nessun `graph` salvato) — utile per retro-compatibilità,
   da valutare se serve ancora lato Spring (il motore Spring supporta entrambi i formati, vedi
   `WorkflowEngine.runWorkflow` in `BACKEND_OVERVIEW.md`).

### Notifiche realtime
`AppLayout.tsx:21-25` apre una connessione `socket.io-client` (`src/lib/socket.ts`, auth via
token nel payload di connessione) e ascolta l'evento `notification`, accumulando in uno stato
locale (`notifs`) usato solo per mostrare un puntino rosso sulla campanella — **nessuna pagina o
dropdown lista le notifiche ricevute via socket**, e non c'è alcuna chiamata a
`GET /notifications` nel codice letto: il frontend React non implementa nemmeno il fallback
polling per l'elenco notifiche, solo l'indicatore realtime. Vedi disallineamento sotto.

### Componenti UI di base
`src/components/ui.tsx` — design system minimale "squadrato" (angoli `rounded-sm`/`none`, bordi
marcati): `Button` (varianti primary/accent/ghost/danger), `Card`, `Input`, `Select`, `Textarea`,
`Label`, `Badge` (colore dinamico via inline style), `Spinner`, `Switch`. Tutti componenti di
presentazione puri, nessuna logica. Tema via custom properties CSS (`--surface`, `--border`,
`--text`, ecc., definite altrove in `styles/index.css`, non letto in dettaglio in questo giro ma
referenziato ovunque).

---

## B. Disallineamenti frontend React ↔ backend Spring reale

Punti dove il frontend React si aspetta qualcosa che il backend Spring **non** offre ancora (o
offre diversamente). Vanno tenuti aperti come decisioni esplicite, non risolti per default in
fase di porting:

| Cosa si aspetta il React | Cosa offre Spring oggi | Impatto sul piano Angular |
|---|---|---|
| `socket.io-client` per notifiche realtime (`src/lib/socket.ts`, evento `notification`) | **Nessun endpoint WebSocket/STOMP/SSE.** Notifiche solo `GET /notifications` + `PATCH .../read[-all]`, "nessun realtime (solo polling)" (`BACKEND_OVERVIEW.md` riga 37, 549) | Il modulo notifiche Angular **non può** usare socket.io. Va deciso: polling a intervallo fisso, o rimandare il realtime e fare solo lista+mark-read. Vedi sezione Decisioni. |
| Tab "Ruoli" in Settings: `GET/POST/DELETE /roles`, `PUT /roles/{id}/permissions` (`Settings.tsx:183-266`) | **Nessun `RoleController`/endpoint REST per Role/Permission.** Lato Spring `Role`/`Permission` sono seedati da `RoleSeeder`/`PermissionSeeder` all'avvio, nessun CRUD esposto (verificato per grep su `BACKEND_OVERVIEW.md`: solo riferimenti a seeder e a `RbacService.resolve`) | La tab Ruoli **non è portabile as-is** oggi: o si esclude dall'MVP del frontend (i ruoli restano gestiti solo via seed/DB), o si segnala come dipendenza bloccante verso un futuro `RoleController` lato backend (fuori scope di questo documento, da aprire come task separato sul piano backend). |
| KPI Dashboard: nessuna richiesta di conteggio dedicata, usa `GET /records/{key}?pageSize=1` e legge `total` | Endpoint esiste e supporta la paginazione (`RecordsController` `GET /{key}`), quindi funziona, ma è uno spreco di banda/JSON solo per un numero | Non bloccante, ma occasione di miglioramento (vedi sezione C). |
| `Workflow.graph` con nodi `trigger/action/condition/branch/loop/delay` | Backend supporta lo stesso modello (`GraphWorkflowRunner`, `MAX_STEPS=500`, nodi `trigger/action/condition/branch/loop/delay`) — **combacia**, incluso il fallback al formato lineare `actions[]` se `graph` è vuoto | Nessun disallineamento: `WorkflowEditor` è portabile concettualmente 1:1. |
| `RelationInput`/ricerca record: `GET /records/{key}?q=...&pageSize=N` | Combacia (`RecordsController.GET /{key}`, query string semplice) | Nessun disallineamento. |
| Filtri: operatore `nin` assente in `FilterBuilder.tsx` UI ma presente nel motore | `RecordFilterCompiler` supporta `nin` (`BACKEND_OVERVIEW.md` riga 274) | Piccola occasione: il nuovo `FilterBuilder` Angular può aggiungere `nin` che l'originale aveva dimenticato. |
| Analytics: `GET /analytics/{metric}` | Combacia esattamente (stessi 4 metric, stessa forma dati aggregata) — **nessun** `@RequirePerm`, accessibile a ogni utente autenticato in entrambi i casi | Nessun disallineamento. |
| Ricerca globale `GET /records/search?q=` | Combacia (`RecordsService.globalSearch`, min 2 char, max 30 risultati, filtro RBAC READ) | Nessun disallineamento. |

**Nota generale**: a parte i due punti reali (WebSocket assente, Role/Permission senza
controller), il motore dinamico (records/objects/fields/relations/charts/workflow/webhooks/
apikey/files/audit) combacia abbastanza bene tra ciò che il React si aspetta e ciò che Spring
espone — è la parte di "amministrazione realtime/utenti" quella meno coperta lato backend
attuale.

---

## C. Stack mapping React → Angular

| Layer | Originale (React) | Destinazione (Angular 22) |
|---|---|---|
| Framework | React 19, componenti funzione + hook | Angular 22 standalone components (nessun `NgModule`), già impostato in `app.config.ts` |
| Bundler/dev server | Vite | Angular CLI / `@angular/build` (esbuild-based), già in `package.json` |
| Routing | react-router-dom v7 (`BrowserRouter`, route piatte) | `@angular/router` con `provideRouter`, route lazy via `loadComponent`/`loadChildren` per code-splitting per `ObjectType` area |
| Data fetching/cache | TanStack Query (`useQuery`/`useMutation`, cache key-based, invalidazione manuale) | `HttpClient` + **signals**: per Angular 22 valutare `httpResource()`/`resource()` (le API resource-based sono lo strumento idiomatico per data fetching reattivo in Angular recenti) per le letture; per le mutation, service con metodi che ritornano `Observable`/`Promise` e aggiornano signal locali o un piccolo store. Vedi sezione Decisioni per la scelta definitiva (resource API è ancora developer preview in alcune versioni — da verificare contro la versione 22 esatta installata). |
| Stato globale (auth) | Zustand (`create<AuthState>`, store esterno al component tree) | Service Angular `providedIn: 'root'` con `signal`/`computed` interni (pattern equivalente: stato mutabile centralizzato, niente NgRx per un caso così piccolo — over-engineering non giustificato da un solo store) |
| Stato tema (dark/light) | `useSyncExternalStore` + pub/sub manuale (`src/lib/theme.ts`) | Service con `signal<Theme>`, side-effect su `effect()` per `classList.toggle` + persistenza `localStorage`; applicazione "early" pre-bootstrap va replicata con uno script inline in `index.html` per evitare flash, dato che Angular non renderizza prima del bootstrap |
| Form dinamici | `useState` piatto + funzione `renderInput` a switch (`DynamicForm.tsx`) | `@angular/forms` **Reactive Forms**: `FormGroup` costruito dinamicamente da `FieldDef[]`, con `Validators` derivati da `required`/`min`/`max`/`pattern` — guadagno reale rispetto all'originale (validazione client coerente con `FieldDef`, non solo affidata al 400 del backend). Vedi sezione D. |
| Routing guard auth | Componente wrapper `Protected` con check imperativo | `CanActivateFn` (functional guard, idiomatico Angular 15+) basato sul service auth a signal |
| HTTP client + interceptor JWT | `fetch` wrapper manuale con refresh-on-401 (`src/lib/api.ts`) | `HttpClient` + `HttpInterceptorFn` (functional interceptor) per: header `Authorization: Bearer`, intercettazione 401 → refresh → retry; centralizza ciò che in React era ad-hoc in un solo file |
| Realtime notifiche | `socket.io-client` (richiede backend Socket.IO — **assente lato Spring**) | Da rivalutare: **non c'è equivalente lato Spring oggi**. Opzioni: polling con `setInterval`/`rxjs interval` su `GET /notifications`, oppure proporre SSE lato backend come task separato. Nessuna soluzione client-side sostituisce un endpoint che non esiste. Vedi Decisioni. |
| Grafici | recharts (wrapper React su D3) | Nessun wrapper Angular diretto di recharts. Alternative idiomatiche: **ngx-charts** (Swimlane, basata su D3, supporto Angular nativo) o **Chart.js + ng2-charts** (più leggero, API command-based) o **ECharts + ngx-echarts**. Da scegliere — vedi Decisioni, nessuna è "ovviamente" superiore senza un criterio (bundle size vs feature richieste: qui servono solo area/line/pie/bar, abbastanza semplici). |
| Icone | lucide-react (~1500 icone tree-shakeable) | **lucide-angular** (stesso set di icone, stessa libreria sorgente, porting ufficiale mantenuto dal progetto Lucide) — sostituzione diretta, stesso nome icone, nessuna perdita di asset visivi |
| CSS/styling | Tailwind v4 + CSS custom properties per tema | **Stesso**: Tailwind 4 già configurato nello scheletro Angular (`frontend/package.json:27,31`). Le custom properties di tema (`--surface`, `--border`, ecc.) si portano 1:1, sono CSS puro |
| Drawer laterale resizable | Componente React fatto a mano (`Drawer.tsx`, drag su `mousemove`/`mouseup`) | Component Angular standalone equivalente, stessa logica di resize (eventi nativi, nessuna libreria necessaria — è ~60 righe di logica semplice) |
| Editor workflow a grafo | Canvas SVG fatto a mano (drag nodi, path bezier per gli edge) | Stesso approccio fattibile in Angular puro (nessuna libreria grafo necessaria per la complessità attuale: pochi tipi di nodo, nessun autolayout). Da NON sovra-ingegnerizzare con una libreria graph-editor pesante se il canvas a mano basta, ma rivalutare se il editor cresce in complessità |
| Test | Vitest (già configurato, `frontend/package.json:33`) | **Stesso**: Vitest è già il runner configurato nello scheletro Angular generato — nessun cambio necessario |

---

## D. Miglioramenti possibili rispetto all'originale

Solo punti emersi realmente dal codice letto, non genericità da checklist:

1. **Validazione form lato client assente.** `DynamicForm.tsx` non valida nulla (`onSubmit` passa
   `data` grezzo al backend, si fida del 400). Con Reactive Forms + `Validators` derivati da
   `FieldDef.required/min/max/pattern` si ottiene feedback immediato senza round-trip, e si
   riduce il carico su `RecordValidator` lato Spring per gli errori "banali" (campo vuoto,
   pattern non rispettato) — il backend resta comunque l'ultima linea di difesa.

2. **Gestione errori HTTP generica e poco strutturata.** `src/lib/api.ts:44-47` lancia
   `new Error(body.message ?? ...)`, perso ogni dettaglio strutturato (es. quale campo ha
   fallito la validazione in `RecordValidationException`). Il backend Spring ritorna eccezioni
   tipizzate (400 per validazione, 403 per RBAC con messaggi dedicati per scope `OWN`, 401
   generico per auth). Un interceptor Angular + un modello di errore tipizzato (discriminare per
   status code e per shape del body) permette messaggi UI più precisi (es. evidenziare il campo
   in errore nel form, non solo un toast con testo libero).

3. **Type-safety FE/BE persa con il cambio di linguaggio backend.** L'originale aveva
   `packages/shared` con Zod condiviso tra NestJS e React (anche se i file letti in questo giro
   non mostrano import diretti di quello shared package nei file analizzati — `FieldDef`/
   `ObjectType` in `hooks.ts` sono ridefiniti a mano). Ora che il backend è Java, non c'è più
   alcuna possibilità di condivisione diretta di tipi. Vale la pena generare i tipi TypeScript
   da `springdoc-openapi` (il backend Spring lo monta, vedi stack mapping in `00-OVERVIEW.md`
   riga 27) con un tool tipo `openapi-typescript`, invece di duplicare a mano le interfacce come
   faceva l'originale — elimina una intera classe di bug da disallineamento DTO silenzioso.

4. **`useState` piatto per form complessi con sezioni/larghezze diventa difficile da estendere.**
   Con Reactive Forms + `FormArray`/gruppi annidati per sezione si ottiene una struttura che
   rispecchia meglio il modello (`FieldDef.section`), invece di un singolo oggetto piatto
   `Record<string, any>` senza struttura interna.

5. **Dashboard con fallback a dati finti silenzioso.** `Dashboard.tsx:73-79` sostituisce dati
   reali con valori inventati se `/analytics/{metric}` fallisce, senza alcun indicatore visivo —
   rischio concreto di mostrare metriche false come fossero vere in produzione. Nel porting:
   o si rimuove il fallback silenzioso, o si rende esplicito in UI ("dati non disponibili" /
   placeholder visivamente distinto), mai un numero plausibile ma falso senza etichetta.

6. **KPI card con query sprecata.** Ogni KPI in Dashboard fa un `GET .../{key}?pageSize=1` solo
   per leggere `total` (`Dashboard.tsx:45`) — scarica anche un record completo che viene
   scartato. Se in futuro si aggiunge un endpoint count-only lato Spring è un miglioramento
   naturale; nel frattempo, lato Angular si può almeno centralizzare la chiamata e cacharla più
   aggressivamente (gli oggetti enabled cambiano raramente).

7. **Token JWT in `localStorage`, non httpOnly.** Scelta dell'originale (`src/lib/api.ts:5-13`),
   esposta a XSS. Non è detto vada cambiata (richiede di ripensare CORS/cookie lato Spring, che
   oggi è stateless puro JWT in header), ma va segnalata come trade-off noto, non riprodotta
   senza consapevolezza. Da confermare con l'utente se si vuole mantenere lo stesso schema o
   investigare `httpOnly` cookie + refresh flow lato Spring (cambio non banale, fuori scope MVP).

8. **Operatore filtro `nin` mancante in UI ma supportato dal motore.** Piccola incompletezza
   dell'originale (vedi tabella disallineamenti) — banale da correggere nel nuovo
   `FilterBuilder` Angular.

9. **Nessuna paginazione reale in `RecordList`/`CalendarView`.** L'originale chiama sempre
   `pageSize: 200` fisso (`RecordList.tsx:40`, `CalendarView.tsx:21-24`) invece di una vera
   paginazione incrementale — funziona per dataset piccoli ma non scala. Il backend supporta
   `LIMIT`/`OFFSET` con page size max 200 (`RecordQueryService`, `BACKEND_OVERVIEW.md` riga 264),
   quindi la capacità c'è lato server: vale la pena introdurre paginazione vera (scroll
   infinito o paginazione a pagine) nel nuovo frontend invece di richiedere sempre il tetto
   massimo.

10. **Accessibilità non verificata nell'originale.** Nessun file letto mostra attributi ARIA,
    gestione del focus nei `Drawer`/`Modal`, o navigazione da tastiera esplicita (es. trap del
    focus quando un drawer è aperto). Va trattato come requisito nuovo nel porting Angular, non
    come "stato attuale da preservare" — il CDK di Angular (`@angular/cdk/overlay`,
    `@angular/cdk/a11y` con `FocusTrap`) offre primitive pronte per drawer/modal accessibili che
    l'originale React non aveva (era tutto fatto a mano).

---

## E. Moduli e ordine di lavoro

Ordine per dipendenze reali (non arbitrario): non ha senso costruire liste/dettaglio dinamici
prima che routing+http+auth esistano, e non ha senso costruire dashboard/calendario prima che il
motore dinamico generico (liste, form, filtri) sia solido, perché tutte le aree "di prodotto" lo
riusano.

### Fase 0 — Fondamenta
- [ ] Routing base (`app.routes.ts`): struttura delle route piatte equivalente all'originale
      (`/login`, `/onboarding`, `/`, `/o/:objectKey`, `/o/:objectKey/:id`, `/search`,
      `/settings`), lazy-loaded per area
- [ ] `HttpClient` + `HttpInterceptorFn` per JWT (header `Authorization`, refresh-on-401 con
      singolo retry, equivalente a `src/lib/api.ts`)
- [ ] Service auth a signal (sostituisce lo store Zustand): `user`, `loading`, `login`,
      `logout`, `load` (bootstrap da `GET /auth/me` se c'è un token)
- [ ] `CanActivateFn` guard per le route protette
- [ ] Layout applicativo (`AppLayout` equivalente): topbar, sidebar dinamica da `GET /objects`,
      drawer mobile, bottom-nav — senza ancora il badge notifiche realtime (dipende da una
      decisione presa in Fase 6)
- [ ] Tema dark/light (service a signal + script inline pre-bootstrap in `index.html`)
- [ ] Componenti UI di base (equivalenti a `ui.tsx`): Button/Card/Input/Select/Textarea/Label/
      Badge/Spinner/Switch — design "squadrato" Tailwind, stesso linguaggio visivo

### Fase 1 — Auth completo
- [ ] Pagina Login (form + gestione errori tipizzata, non solo `e.message` grezzo)
- [ ] Pagina Onboarding (form a step, stessa UX dell'originale: workspace+brand color poi
      owner+password)
- [ ] Verifica end-to-end del flusso `GET /auth/status` → onboarding/login → `/`

### Fase 2 — Motore dinamico generico (la parte più importante e complessa)
È la fase che determina la qualità di tutto il resto, perché liste/dettaglio/dashboard/
calendario/ricerca lo riusano tutti.
- [ ] Service/typing per `ObjectType`/`FieldDef` (idealmente generati da OpenAPI, vedi punto D.3)
- [ ] `DynamicForm` equivalente con Reactive Forms: `FormGroup` dinamico da `FieldDef[]`,
      raggruppamento per sezione, validatori derivati, rendering condizionale per ~20 `FieldType`
      (stesso ventaglio di `renderInput` in `DynamicForm.tsx:66-188`)
- [ ] `RelationInput` equivalente (ricerca con debounce — miglioria rispetto all'originale che
      non lo aveva, vedi nota su `RelationInput.tsx:19-26`)
- [ ] `FilterBuilder` equivalente (AND/OR ricorsivo, tutti gli operatori inclusi `nin`)
- [ ] `RecordList`: tabella + vista board/kanban su campo STATUS, ricerca, filtri, multi-sort,
      selezione multipla + bulk update/delete, drawer create/view
- [ ] `RecordDetail`: vista a pagina intera con sezione collegamenti (`outgoing`/`incoming`)
- [ ] Drawer laterale resizable (component equivalente a `Drawer.tsx`)
- [ ] Verifica RBAC lato UI: nascondere azioni write/delete se il record è in scope `OWN` e
      l'utente non è owner (oggi il backend lo rifiuta server-side comunque, ma un buon
      frontend evita di mostrare bottoni che falliranno sempre)

### Fase 3 — Ricerca globale e dashboard
- [ ] Pagina Search (`GET /records/search`, combacia 1:1 col backend)
- [ ] Dashboard: KPI card per `ObjectType`, scelta libreria grafici (vedi Decisioni), 4 grafici
      analytics — **senza** il fallback silenzioso a dati finti (vedi punto D.5)

### Fase 4 — Calendario
- [ ] `CalendarView`: griglia mensile, aggregazione da 4 tipi di record con campi data
      type-specific — mantenere i nomi campo hardcoded per ora (combaciano col seed
      `ObjectTypeSeeder`), ma isolarli in un punto unico così sono facili da aggiornare se lo
      schema cambia

### Fase 5 — Settings (escluse le parti bloccate da disallineamento backend)
- [ ] Tab Oggetti: lista, creazione `ObjectType`, `SchemaEditor`/`FieldEditor` per i `FieldDef`
- [ ] Tab Utenti: lista, invito (`POST /users/invite`, mostra token una sola volta)
- [ ] Tab API Key: crea/lista/revoca chiavi
- [ ] Tab Workflow: editor visuale a grafo (canvas SVG equivalente a `WorkflowEditor.tsx`), CRUD
      workflow, run manuale, storico esecuzioni
- [ ] Tab Ruoli: **bloccata** finché non esiste un `RoleController` lato Spring (vedi
      disallineamento B) — da escludere dall'MVP o implementare come sola-lettura se si espone
      un endpoint minimale di lettura permessi

### Fase 6 — Notifiche
- [ ] Lista notifiche + mark-read/mark-all-read (`GET /notifications`,
      `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` — combaciano col
      backend reale)
- [ ] Strategia di aggiornamento "quasi realtime": dipende dalla decisione in sezione F. Se si
      sceglie polling, implementarlo qui con un intervallo ragionevole (es. 30-60s) invece del
      socket.io dell'originale, che non ha contropartita lato Spring

### Fase 7 — Rifinitura trasversale
- [ ] Accessibilità: focus trap su drawer/modal (CDK), navigazione da tastiera, attributi ARIA
      minimi (assente nell'originale, vedi punto D.10)
- [ ] Paginazione reale in `RecordList`/liste pesanti (vedi punto D.9)
- [ ] Gestione errori HTTP tipizzata end-to-end (vedi punto D.2)

---

## F. Decisioni da prendere / da riconfermare

Punti dove serve una scelta esplicita dell'utente prima o durante l'implementazione, non
deducibile da "cosa faceva l'originale" perché l'originale non ha contropartita valida o perché
sono scelte di stack aperte:

1. **Realtime notifiche.** L'originale usa socket.io-client, ma il backend Spring non ha alcun
   endpoint WebSocket/STOMP/SSE (`BACKEND_OVERVIEW.md`: "nessun realtime, solo polling"). Opzioni:
   (a) polling periodico lato Angular su `GET /notifications` — più semplice, nessuna modifica
   al backend, ma non è vero realtime; (b) proporre un endpoint SSE lato Spring come task
   separato sul piano backend, poi consumarlo con `EventSource` in Angular; (c) rimandare del
   tutto il "live update" e accontentarsi di un refresh on-demand (apertura dropdown notifiche).
   Va deciso prima della Fase 6.

2. **Libreria grafici.** Nessun equivalente diretto di recharts in Angular. Tre candidate
   plausibili (ngx-charts, ng2-charts/Chart.js, ngx-echarts) con trade-off diversi di bundle
   size/flessibilità/manutenzione — nessuna "ovvia" senza un criterio di scelta esplicito
   dall'utente (es. priorità a bundle minimo vs a fedeltà visiva con l'originale).

3. **Strategia di data-fetching/cache lato client.** TanStack Query (originale) ha invalidazione
   per chiave granulare e cache condivisa cross-componente, comportamento maturo. In Angular 22
   esistono le **resource API** (`resource()`/`httpResource()`) ma è da verificare la loro
   stabilità nella versione esatta installata (`frontend/package.json:14-19` fissa `^22.0.0` —
   da controllare se le resource API sono stabili o ancora developer preview a quella versione)
   prima di costruirci sopra l'intero layer dati. Alternativa più conservativa: service con
   `HttpClient` + `signal` gestiti a mano, pattern più verboso ma sicuramente stabile. Va deciso
   in Fase 0, perché impatta la forma di tutti gli altri service.

4. **Token storage.** Mantenere `localStorage` (come l'originale, semplice ma esposto a XSS) o
   investigare un cambio architetturale verso cookie httpOnly (richiede modifiche CORS/CSRF lato
   Spring, oggi configurato stateless puro JWT-in-header — `SecurityConfig` in
   `BACKEND_OVERVIEW.md` riga 53-58). Non bloccante per l'MVP, ma da confermare esplicitamente
   per non riprodurre un trade-off di sicurezza senza consapevolezza.

5. **Tab Ruoli in Settings.** Esclusione totale dall'MVP frontend, o apertura di un task
   backend per un `RoleController` minimale (almeno lettura)? Impatta la pianificazione della
   Fase 5 e potenzialmente richiede coordinamento con il piano di migrazione backend
   (`md/02-RBAC.md`).

6. **Formato `Workflow.graph` "legacy" (`synthGraph`).** L'originale sintetizza un grafo
   leggibile da workflow definiti solo con `trigger`+`actions[]` lineari (nessun `graph`
   salvato) per retro-compatibilità con dati vecchi. Dato che `WorkflowSeeder` lato Spring semina
   12 automazioni di default probabilmente nel formato lineare (da verificare leggendo
   `WorkflowSeeder.java` se serve conferma), questa funzione di sintesi è probabilmente ancora
   necessaria nel nuovo editor — da confermare quando si arriva alla Fase 5/Workflow tab.

7. **Paginazione vs `pageSize` fisso a 200.** Introdurre paginazione reale è un miglioramento
   proposto (punto D.9), ma cambia il contratto UX delle liste (scroll infinito? pagine
   numerate?) — scelta di prodotto da confermare, non solo tecnica.
