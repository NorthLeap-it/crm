# 01 — Auth completo (estensione)

Quanto fatto finora (login + JWT access token semplice) copre solo una parte di quello che fa
`auth.module.ts`/`auth.service.ts` nell'originale. Manca: refresh token con sessioni revocabili,
onboarding del primo workspace, inviti utente, logout.

## Entity da aggiungere

### Workspace
Un solo record logico nel sistema (anche se la tabella supporta più righe in teoria, l'originale
ne usa una sola con id fisso `"default"`). Campi: `name`, `brandColor`, `logoUrl`, `onboarded`
(bool), `createdAt`/`updatedAt`.

### Session
Rappresenta un refresh token attivo. Campi: `userId` (FK), `refreshHash` (SHA-256 del refresh
token, mai il token in chiaro), `userAgent`, `ip`, `expiresAt`, `revokedAt` (nullable — null =
ancora valida), `createdAt`.

Perché si salva l'hash e non il token: stesso principio delle password, se il DB viene
compromesso un attaccante non deve poter riusare le sessioni.

### Invite
Token di invito per nuovi utenti (creato da un admin, non self-service). Campi: `email`,
`roleId` (FK), `tokenHash`, `invitedBy` (FK User, nullable), `acceptedAt` (nullable),
`expiresAt`, `createdAt`.

## Modifiche a `User` esistente

Aggiungere: `avatarUrl` (String, nullable), `lastLoginAt` (Instant, nullable). La relazione con
`Role` arriva nel modulo RBAC (Fase 2), non qui — ma teniamola a mente: `User` avrà una
relazione `@ManyToMany` con `Role` tramite tabella ponte `UserRole` (o un'entity `UserRole`
esplicita se serve aggiungere metadati alla relazione, come fa l'originale).

## Flusso JWT: access + refresh

L'originale usa due secret diversi (`JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`) e due scadenze
diverse (15 minuti l'access, 30 giorni il refresh). Manteniamo lo stesso schema:

- **Access token**: contiene `sub` (userId) ed `email`, scade in 15 minuti, è quello che il
  client manda in `Authorization: Bearer`. Non viene salvato lato server (è stateless).
- **Refresh token**: contiene `sub` e un `jti` (random, identificatore univoco del token),
  scade in 30 giorni. Alla generazione, il suo hash SHA-256 viene salvato in `Session`. Per
  rinnovare l'access token, il client manda il refresh token a `/auth/refresh`; il server
  verifica la firma JWT, poi controlla che esista una `Session` non revocata con quell'hash e
  non scaduta. Se tutto ok, **revoca la sessione vecchia** e ne crea una nuova (rotation:
  ogni refresh consuma il token precedente, mitigando replay).

Questo significa che il nostro `JwtService` attuale va arricchito con due secret invece di uno,
e con un metodo `generateRefreshToken` parallelo a `generateToken` (da rinominare in
`generateAccessToken` per chiarezza).

## Endpoint da implementare

Tutti sotto `/api/auth`, tranne `logout`/`me` che richiedono autenticazione:

- `GET /auth/status` (pubblico) — dice se il workspace è già stato configurato (per decidere
  se il frontend mostra onboarding o login)
- `POST /auth/onboarding` (pubblico, ma si blocca da solo se già esiste un workspace
  onboardato) — crea il primo utente con ruolo `owner` e configura il `Workspace`
- `POST /auth/login` (pubblico)
- `POST /auth/refresh` (pubblico, ma valido solo con un refresh token valido)
- `POST /auth/logout` (autenticato) — revoca la sessione associata al refresh token passato
- `GET /auth/me` (autenticato) — utente corrente, coi suoi ruoli risolti come array di stringhe
  (chiavi dei ruoli, es. `["owner"]`), MAI con `passwordHash` nella risposta

## Differenza rispetto a quanto già fatto

Il nostro `AuthController`/`AuthService` attuali fanno solo login con access token "semplice"
(nessun refresh, nessuna sessione salvata). Quando arriviamo a implementare questa fase, il
flusso di login cambia: non ritorna solo `accessToken`, ma `{accessToken, refreshToken}`, e crea
una riga in `Session`. Il `LoginResponse`/`AuthResponse` DTO che hai già nel progetto va
aggiornato di conseguenza (probabilmente sostituendo i due DTO sovrapposti con uno solo
ben definito, come discusso prima).

## Sicurezza: cosa NON cambiare rispetto all'originale

- Niente messaggi diversi per "utente non trovato" vs "password sbagliata" (già impostato così
  nel nostro `AuthExceptionHandler`, da mantenere anche per il flusso esteso).
- L'endpoint di onboarding deve restare bloccato dopo il primo uso: se il workspace esiste già
  ed è `onboarded=true` con almeno un utente, l'endpoint deve rispondere 400, mai permettere
  un secondo "primo utente" che bypassa l'invito.
