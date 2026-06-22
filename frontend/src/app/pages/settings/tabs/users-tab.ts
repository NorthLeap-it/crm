import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { UiButton } from '../../../components/ui/button';
import { UiSpinner } from '../../../components/ui/spinner';
import { ROLE_KEYS } from '../../../core/roles';
import { UserSummary } from '../../../models/admin';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-users-tab',
  standalone: true,
  imports: [ReactiveFormsModule, UiButton, UiSpinner],
  templateUrl: './users-tab.html'
})
export class UsersTab implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);

  protected readonly roleKeys = ROLE_KEYS;
  protected readonly users = signal<UserSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  // token mostrato una sola volta dopo l'invito (il backend non manda email in questo porting)
  protected readonly inviteToken = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    roleKey: ['agent', Validators.required]
  });

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.admin.listUsers().subscribe({
      next: (u) => {
        this.users.set(u);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected invite(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { email, roleKey } = this.form.getRawValue();
    this.admin.invite(email, roleKey).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.inviteToken.set(res.inviteToken);
        this.form.reset({ roleKey: 'agent' });
        this.reload();
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.message ?? 'Errore nell\'invito');
      }
    });
  }

  protected deactivate(user: UserSummary): void {
    if (!confirm(`Disattivare ${user.email}?`)) return;
    this.admin.deactivateUser(user.id).subscribe(() => this.reload());
  }
}
