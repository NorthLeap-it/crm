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

// Service unico per le aree di amministrazione dei Settings (utenti, API key, workflow).
// Tenute insieme perche' usate solo dalle tab dei Settings e ciascuna ha poche operazioni.
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = `${API_BASE_URL}/api`;

  // --- utenti ---
  listUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.api}/users`);
  }
  invite(email: string, roleKey: string): Observable<InviteCreatedResponse> {
    return this.http.post<InviteCreatedResponse>(`${this.api}/users/invite`, { email, roleKey });
  }
  deactivateUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/users/${id}`);
  }

  // --- API key ---
  listApiKeys(): Observable<ApiKeySummary[]> {
    return this.http.get<ApiKeySummary[]>(`${this.api}/api-keys`);
  }
  createApiKey(name: string, roleKey?: string): Observable<ApiKeyCreatedResponse> {
    return this.http.post<ApiKeyCreatedResponse>(`${this.api}/api-keys`, { name, roleKey });
  }
  revokeApiKey(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/api-keys/${id}`);
  }

  // --- workflow ---
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
