import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';
import { ObjectType } from '../models/object-type';

// Service minimale per la Fase 0: serve solo la lista (sidebar). Il CRUD completo su
// ObjectType/FieldDef arriva in Fase 2 (motore dinamico).
@Injectable({ providedIn: 'root' })
export class ObjectTypeService {
  private readonly http = inject(HttpClient);

  private readonly _objects = signal<ObjectType[]>([]);
  readonly objects = this._objects.asReadonly();

  load() {
    return this.http
      .get<ObjectType[]>(`${API_BASE_URL}/api/objects`)
      .pipe(tap((objects) => this._objects.set(objects)));
  }

  getByKey(key: string) {
    return this.http.get<ObjectType>(`${API_BASE_URL}/api/objects/${key}`);
  }

  create(dto: { key: string; label: string; pluralLabel: string; icon?: string; color?: string }) {
    return this.http.post<ObjectType>(`${API_BASE_URL}/api/objects`, dto);
  }

  remove(key: string) {
    return this.http.delete<void>(`${API_BASE_URL}/api/objects/${key}`);
  }
}
