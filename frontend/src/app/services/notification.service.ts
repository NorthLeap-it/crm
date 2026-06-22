import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';
import { Notification } from '../models/notification';

// Niente realtime (il backend non ha WebSocket/SSE, 04-RESTO-MODULI.md sceglie il polling come
// MVP): la campanella nel layout chiama refresh() a intervalli. Endpoint: GET /api/notifications,
// PATCH /read-all, PATCH /{id}/read.
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/api/notifications`;

  private readonly _items = signal<Notification[]>([]);
  readonly items = this._items.asReadonly();
  readonly unreadCount = computed(() => this._items().filter((n) => n.readAt == null).length);

  refresh() {
    return this.http.get<Notification[]>(this.base).pipe(tap((items) => this._items.set(items)));
  }

  markRead(id: string) {
    return this.http.patch<void>(`${this.base}/${id}/read`, {}).pipe(
      tap(() => this._items.update((items) => items.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n))))
    );
  }

  markAllRead() {
    return this.http.patch<void>(`${this.base}/read-all`, {}).pipe(
      tap(() => this._items.update((items) => items.map((n) => ({ ...n, readAt: n.readAt ?? new Date().toISOString() }))))
    );
  }
}
