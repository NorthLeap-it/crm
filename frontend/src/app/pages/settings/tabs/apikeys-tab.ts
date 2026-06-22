import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { UiButton } from '../../../components/ui/button';
import { UiSpinner } from '../../../components/ui/spinner';
import { ROLE_KEYS } from '../../../core/roles';
import { ApiKeySummary } from '../../../models/admin';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-apikeys-tab',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, UiButton, UiSpinner],
  templateUrl: './apikeys-tab.html'
})
export class ApiKeysTab implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);

  protected readonly roleKeys = ROLE_KEYS;
  protected readonly keys = signal<ApiKeySummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  // chiave in chiaro mostrata UNA sola volta dopo la creazione
  protected readonly createdKey = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    roleKey: ['']
  });

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.admin.listApiKeys().subscribe({
      next: (k) => {
        this.keys.set(k);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected create(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { name, roleKey } = this.form.getRawValue();
    this.admin.createApiKey(name, roleKey || undefined).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.createdKey.set(res.apiKey);
        this.form.reset({ roleKey: '' });
        this.reload();
      },
      error: () => this.submitting.set(false)
    });
  }

  protected revoke(key: ApiKeySummary): void {
    if (!confirm(`Revocare la chiave "${key.name}"?`)) return;
    this.admin.revokeApiKey(key.id).subscribe(() => this.reload());
  }
}
