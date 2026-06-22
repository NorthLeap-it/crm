import { Component, computed, inject, input, model, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { FieldDef } from '../../models/object-type';
import { RecordItem } from '../../models/record';
import { RecordsService } from '../../services/records.service';

// Picker di record collegati per i campi RELATION/LOOKUP. Cerca nell'ObjectType target
// (config.targetObject) con debounce. config.multiple decide singolo vs multiplo - il valore nel
// form e' un id (string) o una lista di id (string[]), come si aspetta RecordValidator lato backend.
@Component({
  selector: 'app-relation-input',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './relation-input.html'
})
export class RelationInput {
  private readonly recordsService = inject(RecordsService);

  readonly field = input.required<FieldDef>();
  // value: string (singolo) | string[] (multiplo)
  readonly value = model<string | string[] | null>(null);

  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly results = signal<RecordItem[]>([]);
  protected readonly selected = signal<RecordItem[]>([]);
  protected readonly open = signal(false);

  protected readonly targetKey = computed(() => (this.field().config?.['targetObject'] as string) ?? '');
  protected readonly multiple = computed(() => this.field().config?.['multiple'] === true);

  constructor() {
    this.search.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) => {
          const term = q.trim();
          if (term.length < 1 || !this.targetKey()) {
            this.results.set([]);
            return [];
          }
          return this.recordsService.query(this.targetKey(), { q: term, pageSize: 8 });
        })
      )
      .subscribe((res) => {
        this.results.set('items' in res ? res.items : []);
        this.open.set(true);
      });
  }

  protected pick(rec: RecordItem): void {
    if (this.multiple()) {
      const current = this.selected();
      if (current.some((r) => r.id === rec.id)) return;
      const next = [...current, rec];
      this.selected.set(next);
      this.value.set(next.map((r) => r.id));
    } else {
      this.selected.set([rec]);
      this.value.set(rec.id);
    }
    this.search.setValue('');
    this.results.set([]);
    this.open.set(false);
  }

  protected unpick(rec: RecordItem): void {
    const next = this.selected().filter((r) => r.id !== rec.id);
    this.selected.set(next);
    this.value.set(this.multiple() ? next.map((r) => r.id) : null);
  }
}
