import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

// Routing piatto, equivalente all'originale React (react-router-dom). Le rotte di prodotto
// sono lazy via loadComponent. Tutto sotto AppLayout e' protetto da authGuard.
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login)
  },
  {
    path: 'onboarding',
    loadComponent: () => import('./pages/onboarding/onboarding').then((m) => m.Onboarding)
  },
  {
    path: '',
    loadComponent: () => import('./layout/app-layout').then((m) => m.AppLayout),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard)
      },
      {
        path: 'o/:objectKey',
        loadComponent: () => import('./pages/placeholder/placeholder').then((m) => m.Placeholder),
        data: { title: 'Lista record' }
      },
      {
        path: 'o/:objectKey/:id',
        loadComponent: () => import('./pages/placeholder/placeholder').then((m) => m.Placeholder),
        data: { title: 'Dettaglio record' }
      },
      {
        path: 'search',
        loadComponent: () => import('./pages/placeholder/placeholder').then((m) => m.Placeholder),
        data: { title: 'Ricerca' }
      },
      {
        path: 'settings',
        loadComponent: () => import('./pages/placeholder/placeholder').then((m) => m.Placeholder),
        data: { title: 'Impostazioni' }
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
