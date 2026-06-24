# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
directory.

## What this is

Angular frontend for NorthLeap CRM, consuming the Spring Boot backend in `../backend`. It is a
**from-scratch port** of a React 19 + Vite reference frontend (`crm_geck/apps/frontend`, not in
this repo) — not a 1:1 translation: the target backend (Spring) is not the same backend the React
app was built against, and the UI framework/stack also changes deliberately (see Stack below).

The full analysis of what the React reference does, where it diverges from what the Spring
backend actually exposes, concrete improvement opportunities, and a phase-by-phase build plan all
live in **`../md/06-FRONTEND-OVERVIEW.md`** (Italian) — **read it before starting any module**,
the same way `../backend/CLAUDE.md` points to `../md/01-AUTH.md` etc. for backend phases. For the
actual current shape of the backend API (endpoints, DTOs, RBAC, what's implemented vs. not), the
source of truth is `../BACKEND_OVERVIEW.md` (gitignored, present on disk).

The CRM is **metadata-driven**: there are no hand-built "Contacts"/"Companies" pages. A generic
`ObjectType`/`FieldDef` schema drives list and detail views at runtime (`RecordList`/`RecordDetail`
equivalents), so the dynamic-engine module (Phase 2 below) is the architectural core almost
everything else in this app builds on.

## Stack (decided)

| Layer | Choice |
|---|---|
| Framework | **Angular, latest stable** (currently 22.x, already scaffolded in `package.json`) — standalone components only, no `NgModule`. Keep it on latest stable as the project evolves (`ng update`). |
| Reactivity / state | **Signals-first.** Use `signal`/`computed`/`effect` for component and service state instead of manual RxJS subjects where avoidable. Evaluate Angular 22's `resource()`/`httpResource()` for data fetching (verify their stability in the installed version before committing the whole data layer to them — see `06-FRONTEND-OVERVIEW.md` section F.3); fall back to `HttpClient` + signals in a service if they're not stable enough yet. |
| Forms | `@angular/forms` Reactive Forms, especially for the dynamic record form (`FormGroup` built at runtime from `FieldDef[]`, validators derived from `required`/`min`/`max`/`pattern`) — a real improvement over the React original, which had no client-side validation at all. |
| Routing | `@angular/router`, flat routes (mirroring the original's flat `react-router-dom` structure), lazy-loaded per area via `loadComponent`. Auth guard as a functional `CanActivateFn`. HTTP auth as a functional `HttpInterceptorFn` (JWT header + refresh-on-401). |
| Styling | **Tailwind CSS v4 + daisyUI.** Tailwind is already wired (`@import 'tailwindcss'` in `src/styles.css`, `@tailwindcss/postcss` in `.postcssrc.json`). daisyUI is **not yet installed** — add it as a dependency and enable it with `@plugin "daisyui";` in `src/styles.css` (daisyUI 5 is the Tailwind-v4-compatible line, no separate `tailwind.config.js` needed since Tailwind v4 is CSS-first). Prefer daisyUI's component classes (`btn`, `card`, `input`, `select`, `modal`, `drawer`, `badge`, etc.) over hand-rolling a bespoke UI kit like the React original's `ui.tsx` — daisyUI already covers most of that surface, including built-in dark/light theming, which can replace the original's manual `useSyncExternalStore` theme pub/sub. |
| Icons | `lucide-angular` (official Angular port of the icon set the original uses via `lucide-react`) — direct swap, same icon names. |
| Charts | Not yet decided — see `06-FRONTEND-OVERVIEW.md` section F.2 (ngx-charts vs ng2-charts/Chart.js vs ngx-echarts). |
| Realtime notifications | Not yet decided — the original used socket.io-client, but the Spring backend has no WebSocket/SSE endpoint today. See `06-FRONTEND-OVERVIEW.md` section F.1. |
| Test | Vitest (already configured by `ng new`, `package.json`'s `test` script). |

## Current state

Freshly scaffolded via `ng new` — no application code yet. `app.routes.ts` is an empty `Routes`
array, `app.config.ts` only has the router and global error listener providers, no app-specific
components, services, or guards exist. Tailwind 4 and Vitest are configured; daisyUI is not yet
added. Build the app following the phase order in `06-FRONTEND-OVERVIEW.md` section E (Phase 0:
routing/HTTP/auth/layout foundations, before anything product-specific).

## Build, run, test

From the `frontend/` directory:

```
npm install
npm start          # ng serve, dev server
npm run build       # ng build, production bundle
npm test            # ng test (Vitest)
```

The backend (`../backend`) must be running separately for the app to do anything useful past the
login screen — see `../backend/CLAUDE.md` for how to run it.

## Conventions

- Standalone components only; no `NgModule`.
- Signals over RxJS subjects for component/service state; reserve RxJS for genuinely
  stream-shaped problems (e.g. debounced search input, interceptor chains).
- One component/service per file, grouped by feature folder (`pages/`, `components/`,
  `services/`, matching the original's `pages/`/`components/`/`lib/` split closely enough to stay
  easy to cross-reference against `06-FRONTEND-OVERVIEW.md`).
- Prefer daisyUI component classes over custom CSS; only write bespoke component logic for things
  daisyUI doesn't cover (e.g. the resizable side drawer, the workflow graph canvas).
- Don't silently fabricate fallback data on a failed request (the original's Dashboard does this —
  flagged as an anti-pattern in `06-FRONTEND-OVERVIEW.md` section D.5, do not repeat it here).
