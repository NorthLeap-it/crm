import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';

import { UiButton } from '../../../components/ui/button';
import { UiSpinner } from '../../../components/ui/spinner';
import { ObjectType } from '../../../models/object-type';
import { ObjectTypeService } from '../../../services/object-type.service';

@Component({
  selector: 'app-objects-tab',
  standalone: true,
  imports: [ReactiveFormsModule, UiButton, UiSpinner],
  templateUrl: './objects-tab.html'
})
export class ObjectsTab {
  private readonly fb = inject(FormBuilder);
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly expanded = signal<string | null>(null);
  protected readonly creating = signal(false);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly reload$ = new Subject<void>();

  protected readonly objects = toSignal(
    this.reload$.pipe(
      startWith(undefined),
      tap(() => this.loading.set(true)),
      switchMap(() => this.objectTypeService.load().pipe(catchError(() => of<ObjectType[]>([])))),
      tap(() => this.loading.set(false))
    ),
    { initialValue: [] as ObjectType[] }
  );

  protected readonly form = this.fb.nonNullable.group({
    key: ['', [Validators.required, Validators.pattern('^[a-zA-Z0-9_]{1,64}$')]],
    label: ['', Validators.required],
    pluralLabel: ['', Validators.required],
    icon: [''],
    color: ['#2563eb']
  });

  protected toggle(key: string): void {
    this.expanded.set(this.expanded() === key ? null : key);
  }

  protected create(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.objectTypeService
      .create(this.form.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.creating.set(false);
          this.form.reset({ color: '#2563eb' });
          this.reload$.next();
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err?.error?.message ?? 'Errore nella creazione');
        }
      });
  }

  protected remove(obj: ObjectType): void {
    if (obj.system) return;
    if (!confirm(`Eliminare l'oggetto "${obj.label}" e tutti i suoi record?`)) return;
    this.objectTypeService
      .remove(obj.key)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload$.next());
  }
}
