import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

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
export class ObjectsTab implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly objectTypeService = inject(ObjectTypeService);

  protected readonly objects = signal<ObjectType[]>([]);
  protected readonly loading = signal(true);
  protected readonly expanded = signal<string | null>(null);
  protected readonly creating = signal(false);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    key: ['', [Validators.required, Validators.pattern('^[a-zA-Z0-9_]{1,64}$')]],
    label: ['', Validators.required],
    pluralLabel: ['', Validators.required],
    icon: [''],
    color: ['#2563eb']
  });

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.objectTypeService.load().subscribe({
      next: (objs) => {
        this.objects.set(objs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

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
    this.objectTypeService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.creating.set(false);
        this.form.reset({ color: '#2563eb' });
        this.reload();
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
    this.objectTypeService.remove(obj.key).subscribe(() => this.reload());
  }
}
