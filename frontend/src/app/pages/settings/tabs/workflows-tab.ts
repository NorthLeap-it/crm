import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';

import { UiSpinner } from '../../../components/ui/spinner';
import { Workflow } from '../../../models/admin';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-workflows-tab',
  standalone: true,
  imports: [UiSpinner],
  templateUrl: './workflows-tab.html'
})
export class WorkflowsTab {
  private readonly admin = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly ranId = signal<string | null>(null);

  private readonly reload$ = new Subject<void>();

  protected readonly workflows = toSignal(
    this.reload$.pipe(
      startWith(undefined),
      tap(() => this.loading.set(true)),
      switchMap(() => this.admin.listWorkflows().pipe(catchError(() => of<Workflow[]>([])))),
      tap(() => this.loading.set(false))
    ),
    { initialValue: [] as Workflow[] }
  );

  protected triggerType(w: Workflow): string {
    return (w.trigger?.['type'] as string) ?? '—';
  }

  protected toggle(w: Workflow): void {
    this.admin
      .setWorkflowActive(w.id, !w.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload$.next());
  }

  protected run(w: Workflow): void {
    this.admin
      .runWorkflow(w.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.ranId.set(w.id);
        setTimeout(() => this.ranId.set(null), 2500);
      });
  }

  protected remove(w: Workflow): void {
    if (!confirm(`Eliminare il workflow "${w.name}"?`)) return;
    this.admin
      .removeWorkflow(w.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload$.next());
  }
}
