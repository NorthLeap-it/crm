import { Component, input, model } from '@angular/core';

@Component({
  selector: 'ui-input',
  standalone: true,
  template: `
    <input
      class="input rounded-sm w-full"
      [type]="type()"
      [placeholder]="placeholder()"
      [disabled]="disabled()"
      [required]="required()"
      [value]="value()"
      (input)="value.set($any($event.target).value)"
    />
  `
})
export class UiInput {
  readonly type = input<'text' | 'email' | 'password' | 'number' | 'date' | 'datetime-local' | 'time' | 'url' | 'color'>('text');
  readonly placeholder = input('');
  readonly disabled = input(false);
  readonly required = input(false);
  readonly value = model('');
}
