# 02 — RBAC (Role-Based Access Control con permessi R/W/X)

## Modello

Tre concetti, stessa struttura dell'originale Prisma:

**Role**: `id`, `key` (univoco, es. "owner", "admin", "manager", "agent", "viewer"), `label`,
`isSystem` (bool — i ruoli di sistema non sono eliminabili dall'utente finale).

**UserRole**: tabella ponte many-to-many tra `User` e `Role`. Nell'originale è una entity
esplicita con chiave composta `(userId, roleId)` invece di una semplice `@ManyToMany`, perché
in futuro potrebbe servire aggiungere metadati alla relazione (es. data di assegnazione). Manteniamo
la stessa scelta: entity `UserRole` esplicita con `@EmbeddedId` o `@IdClass`, non una
`@ManyToMany` diretta.

**Permission**: il cuore del sistema R/W/X. Campi: `roleId` (FK), `resource` (stringa libera:
può essere la `key` di un ObjectType come "contact", oppure una risorsa di sistema come "page",
"workflow", "user", "apikey"), `canRead`/`canWrite`/`canExecute` (booleani), `scope` (enum
`OWN`/`TEAM`/`ALL`). Vincolo unico su `(roleId, resource)` — un ruolo ha al massimo un permesso
per risorsa.

Perché `resource` è una stringa libera e non una FK a `ObjectType`: perché deve poter
referenziare anche risorse che non sono ObjectType (page, workflow, user, apikey). Questo è
intenzionale nell'originale e lo manteniamo.

## Significato di R/W/X

- **R (canRead)**: leggere record di quella risorsa.
- **W (canWrite)**: creare/modificare/eliminare record di quella risorsa.
- **X (canExecute)**: eseguire azioni attive non-CRUD — nello specifico, lanciare manualmente
  un workflow. Non si applica a tutte le risorse, ma il modello è generico.

## Scope record-level

Quando un ruolo ha un permesso con scope `OWN`, l'utente vede/modifica solo i record di cui è
`ownerId`. Con `TEAM` (non ancora implementato a fondo nemmeno nell'originale — è un concetto
predisposto ma il filtro team-based richiederebbe un concetto di "team" che il modello attuale
non ha ancora) e `ALL`, nessuna restrizione aggiuntiva.

Se un utente ha più ruoli con scope diversi sulla stessa risorsa, vince lo scope più ampio
(`ALL` > `TEAM` > `OWN`). La logica di risoluzione (`RbacService.resolve`) prende la lista di
roleId dell'utente, la risorsa, l'azione richiesta, e ritorna `{allowed: bool, scope: Scope}`.

## Traduzione del guard NestJS in Spring

Nell'originale, `RbacGuard` legge un metadato (`@RequirePerm('contact', 'write')`) impostato
sul metodo del controller via `SetMetadata`, e lo intercetta con un `CanActivate` globale
registrato come `APP_GUARD`.

In Spring, l'equivalente più diretto è:

1. Un'annotazione custom `@RequirePerm(resource = "contact", action = PermAction.WRITE)`,
   applicabile a metodi di controller.
2. Un `HandlerInterceptor` (registrato globalmente via `WebMvcConfigurer`) che, in
   `preHandle`, controlla se il metodo invocato ha l'annotazione; se sì, recupera l'`Actor`
   corrente (vedi sotto) e chiama `RbacService.resolve(...)`; se non permesso, lancia
   un'eccezione che un `@RestControllerAdvice` traduce in 403.

Alternativa via Spring AOP con `@Aspect` e un `@Around` advice: funziona altrettanto bene ed è
forse più idiomatico in ambiente Spring puro, ma introduce un concetto in più (AOP) rispetto
all'interceptor, che è meccanicamente più simile al guard NestJS originale. Scegliamo
l'interceptor per minimizzare la distanza concettuale con l'originale e perché è sufficiente
per le esigenze attuali.

## Cos'è l'"Actor"

Nell'originale, `Actor` è un tipo che rappresenta "chi sta facendo la richiesta", e può essere
uno `user` autenticato via JWT oppure una `apikey` autenticata via header `X-Api-Key`. Ha
`id`, `type` (`user`|`apikey`), `email` (opzionale), `roleIds` (lista). Questo perché lo stesso
sistema di permessi deve funzionare sia per richieste umane che per integrazioni automatiche
via API key.

In Spring, replichiamo questo concetto con una classe `Actor` (non un'entity, un DTO interno) e
la popoliamo nel filtro di autenticazione (estensione del nostro `JwtAuthenticationFilter`
attuale, che oggi gestisce solo JWT): se la richiesta ha header `X-Api-Key`, risolviamo l'Actor
da lì invece che dal token; altrimenti dal JWT come già facciamo. L'Actor viene attaccato alla
request (es. come request attribute) e recuperato nei controller con un argument resolver
custom, equivalente al `@CurrentUser()` decorator di NestJS.

## Differenza importante da segnalare

Il nostro `AppUserPrincipal`/`UserDetails` attuale (per Spring Security standard) e questo
`Actor` (per la logica RBAC custom) sono due concetti paralleli con scopi diversi:
`UserDetails` serve a Spring Security per decidere SE una richiesta è autenticata;
`Actor` serve alla nostra logica RBAC custom per decidere COSA quella richiesta può fare. Non
vanno fusi in una sola classe, anche se si sovrappongono parzialmente — tenerli separati rende
più facile aggiungere l'autenticazione via API key senza toccare la parte Spring Security
"standard".

## Seed dei ruoli di sistema

Da fare come dato di inizializzazione (data.sql, o un componente `@PostConstruct`/
`ApplicationRunner` che gira all'avvio se la tabella `Role` è vuota — replicando lo spirito
dello script `seed.ts` Prisma, ma in Java): owner, admin, manager (tutti con R/W/X piene su
tutte le risorse), agent (R/W ma non X), viewer (solo R). Le risorse su cui assegnare permessi
di default dipendono dagli ObjectType che esisteranno (Fase 3) più le risorse di sistema fisse
(page, chart, workflow, user, apikey) — quindi il seed completo dei permessi va fatto DOPO aver
implementato il motore dinamico, anche se le entity Role/Permission si possono creare già ora.
