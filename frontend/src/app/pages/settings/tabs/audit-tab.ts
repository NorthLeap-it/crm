import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';

import { UiButton } from '../../../components/ui/button';
import { UiSpinner } from '../../../components/ui/spinner';
import { AuditLog } from '../../../models/admin';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-audit-tab',
  standalone: true,
  imports: [ReactiveFormsModule, UiButton, UiSpinner, DatePipe],
  templateUrl: './audit-tab.html'
})
export class AuditTab {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);

  // i filtri correnti applicati (li leggo qui dentro lo switchMap, non dal form, così la
  // ricarica usa sempre i valori confermati con "Filtra")
  private readonly filters = signal<{ resource?: string; resourceId?: string }>({});
  private readonly reload$ = new Subject<void>();

  protected readonly logs = toSignal(
    this.reload$.pipe(
      startWith(undefined),
      tap(() => this.loading.set(true)),
      switchMap(() => {
        const { resource, resourceId } = this.filters();
        return this.admin.listLogs(resource, resourceId).pipe(catchError(() => of<AuditLog[]>([])));
      }),
      tap(() => this.loading.set(false))
    ),
    { initialValue: [] as AuditLog[] }
  );

  protected readonly form = this.fb.nonNullable.group({
    resource: [''],
    resourceId: ['']
  });

  protected applyFilters(): void {
    const { resource, resourceId } = this.form.getRawValue();
    this.filters.set({ resource: resource || undefined, resourceId: resourceId || undefined });
    this.reload$.next();
  }

  protected reset(): void {
    this.form.reset({ resource: '', resourceId: '' });
    this.filters.set({});
    this.reload$.next();
  }
}
