import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';
import {
  ApiKeyCreatedResponse,
  ApiKeySummary,
  InviteCreatedResponse,
  UserSummary,
  Workflow
} from '../models/admin';

// service per gestione user admin, e gestione impostazioni
@Injectable({ providedIn: 'root' })
export class AdminService {
  // http client con api base
  private readonly http = inject(HttpClient);
  private readonly api = `${API_BASE_URL}/api`;

  // utenti
  /*
    lista di utenti
    invita
    disattiva un utente
  */
  listUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.api}/users`);
  }

  invite(email: string, roleKey: string): Observable<InviteCreatedResponse> {
    return this.http.post<InviteCreatedResponse>(`${this.api}/users/invite`, { email, roleKey });
  }

  deactivateUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/users/${id}`);
  }

  // api keys
  /*
    lista di api-keys
    crea una nuova api-key
    revoca un api-key
  */
  listApiKeys(): Observable<ApiKeySummary[]> {
    return this.http.get<ApiKeySummary[]>(`${this.api}/api-keys`);
  }

  createApiKey(name: string, roleKey?: string): Observable<ApiKeyCreatedResponse> {
    return this.http.post<ApiKeyCreatedResponse>(`${this.api}/api-keys`, { name, roleKey });
  }

  revokeApiKey(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/api-keys/${id}`);
  }

  // workflow
  /*
    lista di workflows
    attivazione determinato workflow
    runna un determinato workflow
    rimuove un determinato workflaw
  */
  listWorkflows(): Observable<Workflow[]> {
    return this.http.get<Workflow[]>(`${this.api}/workflows`);
  }
  
  setWorkflowActive(id: string, active: boolean): Observable<Workflow> {
    return this.http.patch<Workflow>(`${this.api}/workflows/${id}`, { isActive: active });
  }

  runWorkflow(id: string): Observable<unknown> {
    return this.http.post<unknown>(`${this.api}/workflows/${id}/run`, {});
  }

  removeWorkflow(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/workflows/${id}`);
  }
}
