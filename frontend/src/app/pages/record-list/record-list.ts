import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { Drawer } from '../../components/drawer/drawer';
import { DynamicForm } from '../../components/dynamic-form/dynamic-form';
import { UiButton } from '../../components/ui/button';
import { UiSpinner } from '../../components/ui/spinner';
import { FieldDef, ObjectType } from '../../models/object-type';
import { RecordItem } from '../../models/record';
import { RecordsService } from '../../services/records.service';

@Component({
  selector: 'app-record-list',
  standalone: true,
  imports: [Drawer, DynamicForm, UiButton, UiSpinner],
  templateUrl: './record-list.html'
})
export class RecordList {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recordsService = inject(RecordsService);

  // la rotta /o/:objectKey puo' cambiare key senza distruggere il componente: reagiamo al param
  protected readonly objectKey = toSignal(this.route.paramMap.pipe(map((p) => p.get('objectKey') ?? '')), {
    initialValue: ''
  });

  protected readonly loading = signal(true);
  protected readonly object = signal<ObjectType | null>(null);
  protected readonly records = signal<RecordItem[]>([]);
  protected readonly total = signal(0);

  protected readonly drawerOpen = signal(false);
  protected readonly submitting = signal(false);

  // colonne: i field con showInList, fallback ai primi 6 (stesso criterio dell'originale)
  protected readonly columns = computed<FieldDef[]>(() => {
    const obj = this.object();
    if (!obj) return [];
    const shown = obj.fields.filter((f) => f.showInList && !f.hidden);
    return (shown.length > 0 ? shown : obj.fields.filter((f) => !f.hidden).slice(0, 6)).sort(
      (a, b) => a.sortOrder - b.sortOrder
    );
  });

  protected readonly formFields = computed<FieldDef[]>(() => this.object()?.fields ?? []);

  constructor() {
    // ricarica quando cambia la key
    this.route.paramMap.subscribe((params) => {
      const key = params.get('objectKey');
      if (key) this.reload(key);
    });
  }

  private reload(key: string): void {
    this.loading.set(true);
    this.recordsService.query(key, { pageSize: 200 }).subscribe({
      next: (res) => {
        this.object.set(res.object);
        this.records.set(res.items);
        this.total.set(res.total);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected cellValue(rec: RecordItem, field: FieldDef): string {
    const raw = field.key === 'status' ? rec.status : rec.data[field.key];
    if (raw == null || raw === '') return '—';
    if (Array.isArray(raw)) return raw.join(', ');
    if (typeof raw === 'boolean') return raw ? 'Sì' : 'No';
    return String(raw);
  }

  protected openRecord(rec: RecordItem): void {
    this.router.navigate(['/o', this.objectKey(), rec.id]);
  }

  protected createRecord(data: Record<string, unknown>): void {
    this.submitting.set(true);
    this.recordsService.create(this.objectKey(), { data }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.drawerOpen.set(false);
        this.reload(this.objectKey());
      },
      error: () => this.submitting.set(false)
    });
  }
}
