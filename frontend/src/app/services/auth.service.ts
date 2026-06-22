import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, switchMap, tap } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';

export interface MeResponse {
  userId: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  roles: string[];
}

interface AuthResponse {
  userId: string;
  email: string;
  name: string;
}

interface OnboardingPayload {
  workspaceName: string;
  email: string;
  password: string;
  name: string;
}

// I token JWT vivono solo nei cookie httpOnly impostati dal backend (mai letti/scritti qui via
// JS) - questo service tiene solo lo stato "chi e' loggato", popolato da GET /auth/me.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _user = signal<MeResponse | null>(null);
  private readonly _loading = signal(true);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  // chiamata una volta al bootstrap (vedi provideAppInitializer in app.config.ts) e dopo ogni
  // login/onboarding riuscito, per popolare/aggiornare user+roles da /me
  load(): Observable<MeResponse | null> {
    this._loading.set(true);
    return this.http.get<MeResponse>(`${API_BASE_URL}/api/auth/me`).pipe(
      tap((user) => {
        this._user.set(user);
        this._loading.set(false);
      }),
      catchError(() => {
        this._user.set(null);
        this._loading.set(false);
        return of(null);
      })
    );
  }

  login(email: string, password: string): Observable<MeResponse | null> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/login`, { email, password })
      .pipe(switchMap(() => this.load()));
  }

  onboarding(payload: OnboardingPayload): Observable<MeResponse | null> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/onboarding`, payload)
      .pipe(switchMap(() => this.load()));
  }

  // usata internamente dall'interceptor su 401/403; nessun refresh token nel body, viaggia
  // come cookie automaticamente
  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/api/auth/refresh`, {});
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/logout`, {}).pipe(
      tap(() => this.clearSession()),
      catchError(() => {
        this.clearSession();
        return of(void 0);
      })
    );
  }

  clearSession(): void {
    this._user.set(null);
  }
}
