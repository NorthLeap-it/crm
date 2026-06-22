import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';
import { ObjectType } from '../models/object-type';

// service che prende solo la lista di oggetti
@Injectable({ providedIn: 'root' })
export class ObjectTypeService {
  // inject client http
  private readonly http = inject(HttpClient);

  // storro gli object che mi passa
  private _objects: WritableSignal<ObjectType[] | null> = signal(null);
  readonly objects = this._objects.asReadonly();

  // metodo che fa una richiesta http all'endpoint /api/objects
  // poi lo passo alla variabile objects, come readonly
  load() {
    return this.http
      .get<ObjectType[]>(`${API_BASE_URL}/api/objects`)
      .pipe(tap((objects) => this._objects.set(objects)));
  }

  // metodo che prende un determinato object, in base alla chiave passata
  getByKey(key: string) {
    return this.http.get<ObjectType>(`${API_BASE_URL}/api/objects/${key}`);
  }

  // metodo che crea un oggetto, validato tramite il dto
  create(dto: { key: string; label: string; pluralLabel: string; icon?: string; color?: string }) {
    return this.http.post<ObjectType>(`${API_BASE_URL}/api/objects`, dto);
  }

  // metodo che toglie un oggetto
  remove(key: string) {
    return this.http.delete<void>(`${API_BASE_URL}/api/objects/${key}`);
  }
}
