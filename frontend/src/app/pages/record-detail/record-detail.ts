import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';

import { Drawer } from '../../components/drawer/drawer';
import { DynamicForm } from '../../components/dynamic-form/dynamic-form';
import { UiButton } from '../../components/ui/button';
import { UiSpinner } from '../../components/ui/spinner';
import { FieldDef } from '../../models/object-type';
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
  private readonly destroyRef = inject(DestroyRef);

  protected readonly objectKey = this.route.snapshot.paramMap.get('objectKey') ?? '';
  protected readonly recordId = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly loading = signal(true);
  protected readonly editOpen = signal(false);
  protected readonly submitting = signal(false);

  // l'ObjectType (per i campi) si carica una volta sola: stream -> signal
  protected readonly object = toSignal(this.objectTypeService.getByKey(this.objectKey), {
    initialValue: null
  });

  // trigger di ricarica del record dopo una modifica
  private readonly reload$ = new Subject<void>();

  private readonly detail = toSignal(
    this.reload$.pipe(
      startWith(undefined),
      tap(() => this.loading.set(true)),
      switchMap(() =>
        this.recordsService.findOne(this.objectKey, this.recordId).pipe(catchError(() => of(null)))
      ),
      tap(() => this.loading.set(false))
    ),
    { initialValue: null }
  );

  protected readonly record = computed(() => this.detail()?.record ?? null);
  protected readonly outgoing = computed(() => this.detail()?.outgoing ?? []);
  protected readonly incoming = computed(() => this.detail()?.incoming ?? []);

  protected readonly visibleFields = computed<FieldDef[]>(() =>
    (this.object()?.fields ?? []).filter((f) => !f.hidden).sort((a, b) => a.sortOrder - b.sortOrder)
  );

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
    this.recordsService
      .update(this.objectKey, this.recordId, { data })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.editOpen.set(false);
          this.reload$.next();
        },
        error: () => this.submitting.set(false)
      });
  }

  protected remove(): void {
    if (!confirm('Eliminare questo record?')) return;
    this.recordsService
      .remove(this.objectKey, this.recordId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.router.navigate(['/o', this.objectKey]));
  }
}
