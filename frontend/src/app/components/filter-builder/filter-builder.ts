import { Component, computed, input, model } from '@angular/core';

import {
  FILTER_OPS,
  FilterCondition,
  FilterGroup,
  FilterOp,
  NATIVE_FILTER_FIELDS,
  NO_VALUE_OPS
} from '../../models/filter';
import { FieldDef } from '../../models/object-type';

// Costruttore di filtri AND/OR annidati. Ricorsivo: un sotto-gruppo è un altro <app-filter-builder>
// (il componente è importato in sé stesso, pattern standard per gli alberi standalone in AOT).
// Emette un FilterGroup nella forma che il backend (RecordFilterCompiler) si aspetta: condizione
// { field, op, value, value2 }, gruppo { combinator, conditions }.
@Component({
  selector: 'app-filter-builder',
  standalone: true,
  imports: [FilterBuilder],
  templateUrl: './filter-builder.html'
})
export class FilterBuilder {
  readonly fields = input<FieldDef[]>([]);
  readonly value = model.required<FilterGroup>();

  protected readonly ops = FILTER_OPS;
  protected readonly noValueOps = NO_VALUE_OPS;

  // colonne selezionabili: native (title/status/createdAt/updatedAt) + i FieldDef filtrabili
  protected readonly columns = computed(() => [
    ...NATIVE_FILTER_FIELDS,
    ...this.fields()
      .filter((f) => f.filterable !== false)
      .map((f) => ({ key: f.key, label: f.label }))
  ]);

  protected isGroup(c: FilterCondition | FilterGroup): c is FilterGroup {
    return (c as FilterGroup).combinator !== undefined;
  }

  protected asCondition(c: FilterCondition | FilterGroup): FilterCondition {
    return c as FilterCondition;
  }

  protected asGroup(c: FilterCondition | FilterGroup): FilterGroup {
    return c as FilterGroup;
  }

  protected setCombinator(combinator: string): void {
    this.value.update((v) => ({ ...v, combinator: combinator === 'or' ? 'or' : 'and' }));
  }

  protected addCondition(): void {
    const field = this.columns()[0]?.key ?? 'title';
    this.value.update((v) => ({ ...v, conditions: [...v.conditions, { field, op: 'contains' as FilterOp }] }));
  }

  protected addGroup(): void {
    this.value.update((v) => ({ ...v, conditions: [...v.conditions, { combinator: 'and', conditions: [] }] }));
  }

  protected removeAt(i: number): void {
    this.value.update((v) => ({ ...v, conditions: v.conditions.filter((_, j) => j !== i) }));
  }

  protected patchCondition(i: number, patch: Partial<FilterCondition>): void {
    this.value.update((v) => {
      const next = [...v.conditions];
      next[i] = { ...(next[i] as FilterCondition), ...patch };
      return { ...v, conditions: next };
    });
  }

  protected patchGroup(i: number, group: FilterGroup): void {
    this.value.update((v) => {
      const next = [...v.conditions];
      next[i] = group;
      return { ...v, conditions: next };
    });
  }
}
