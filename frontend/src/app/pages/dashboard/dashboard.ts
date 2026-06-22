import { Component, inject } from '@angular/core';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div>
      <h1 class="text-2xl font-semibold mb-2">Dashboard</h1>
      <p class="text-base-content/60">Benvenuto, {{ user()?.name }}.</p>
      <p class="text-sm text-base-content/40 mt-4">
        KPI e grafici analytics arrivano in Fase 3.
      </p>
    </div>
  `
})
export class Dashboard {
  protected readonly user = inject(AuthService).user;
}
