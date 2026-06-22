import { Component, input, model } from '@angular/core';

export interface SelectOption {
  value: string;
  label: string;
}

@Component({
  selector: 'ui-select',
  standalone: true,
  template: `
    <select
      class="select rounded-sm w-full"
      [disabled]="disabled()"
      [value]="value()"
      (change)="value.set($any($event.target).value)"
    >
      @if (placeholder()) {
        <option value="" disabled>{{ placeholder() }}</option>
      }
      @for (opt of options(); track opt.value) {
        <option [value]="opt.value">{{ opt.label }}</option>
      }
    </select>
  `
})
export class UiSelect {
  readonly options = input<SelectOption[]>([]);
  readonly placeholder = input('');
  readonly disabled = input(false);
  readonly value = model('');
}
