import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Drawer } from '../../components/drawer/drawer';
import { DynamicForm } from '../../components/dynamic-form/dynamic-form';
import { UiButton } from '../../components/ui/button';
import { UiSpinner } from '../../components/ui/spinner';
import { FieldDef, ObjectType } from '../../models/object-type';
import { RecordItem, RecordLink } from '../../models/record';
import { ObjectTypeService } from '../../services/object-type.service';
import { RecordsService } from '../../services/records.service';

@Component({
  selector: 'app-record-detail',
  standalone: true,
  imports: [RouterLink, Drawer, DynamicForm, UiButton, UiSpinner],
  templateUrl: './record-detail.html'
})
export class RecordDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recordsService = inject(RecordsService);
  private readonly objectTypeService = inject(ObjectTypeService);

  protected readonly objectKey = this.route.snapshot.paramMap.get('objectKey') ?? '';
  protected readonly recordId = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly loading = signal(true);
  protected readonly object = signal<ObjectType | null>(null);
  protected readonly record = signal<RecordItem | null>(null);
  protected readonly outgoing = signal<RecordLink[]>([]);
  protected readonly incoming = signal<RecordLink[]>([]);

  protected readonly editOpen = signal(false);
  protected readonly submitting = signal(false);

  protected readonly visibleFields = computed<FieldDef[]>(() =>
    (this.object()?.fields ?? []).filter((f) => !f.hidden).sort((a, b) => a.sortOrder - b.sortOrder)
  );

  constructor() {
    this.objectTypeService.getByKey(this.objectKey).subscribe((obj) => this.object.set(obj));
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.recordsService.findOne(this.objectKey, this.recordId).subscribe({
      next: (res) => {
        this.record.set(res.record);
        this.outgoing.set(res.outgoing);
        this.incoming.set(res.incoming);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected displayValue(field: FieldDef): string {
    const rec = this.record();
    if (!rec) return '—';
    const raw = field.key === 'status' ? rec.status : rec.data[field.key];
    if (raw == null || raw === '') return '—';
    if (Array.isArray(raw)) return raw.join(', ');
    if (typeof raw === 'boolean') return raw ? 'Sì' : 'No';
    return String(raw);
  }

  protected saveEdit(data: Record<string, unknown>): void {
    this.submitting.set(true);
    this.recordsService.update(this.objectKey, this.recordId, { data }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.editOpen.set(false);
        this.load();
      },
      error: () => this.submitting.set(false)
    });
  }

  protected remove(): void {
    if (!confirm('Eliminare questo record?')) return;
    this.recordsService.remove(this.objectKey, this.recordId).subscribe(() => {
      this.router.navigate(['/o', this.objectKey]);
    });
  }
}
