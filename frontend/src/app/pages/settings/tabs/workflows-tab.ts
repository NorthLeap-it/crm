import { Component, OnInit, inject, signal } from '@angular/core';

import { UiSpinner } from '../../../components/ui/spinner';
import { Workflow } from '../../../models/admin';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-workflows-tab',
  standalone: true,
  imports: [UiSpinner],
  templateUrl: './workflows-tab.html'
})
export class WorkflowsTab implements OnInit {
  private readonly admin = inject(AdminService);

  protected readonly workflows = signal<Workflow[]>([]);
  protected readonly loading = signal(true);
  protected readonly ranId = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.admin.listWorkflows().subscribe({
      next: (w) => {
        this.workflows.set(w);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected triggerType(w: Workflow): string {
    return (w.trigger?.['type'] as string) ?? '—';
  }

  protected toggle(w: Workflow): void {
    this.admin.setWorkflowActive(w.id, !w.active).subscribe(() => {
      this.workflows.update((list) => list.map((x) => (x.id === w.id ? { ...x, active: !x.active } : x)));
    });
  }

  protected run(w: Workflow): void {
    this.admin.runWorkflow(w.id).subscribe(() => {
      this.ranId.set(w.id);
      setTimeout(() => this.ranId.set(null), 2500);
    });
  }

  protected remove(w: Workflow): void {
    if (!confirm(`Eliminare il workflow "${w.name}"?`)) return;
    this.admin.removeWorkflow(w.id).subscribe(() => this.reload());
  }
}
