import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';
import { Notification } from '../models/notification';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  // importo http client
  private readonly http = inject(HttpClient);
  // endpoint base
  private readonly base = `${API_BASE_URL}/api/notifications`;

  // array che contiene le notifiche
  private readonly _items: WritableSignal<Notification[]> = signal([]);
  readonly items = this._items.asReadonly();
  // array filtrato, con numero di notifiche non lette
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
