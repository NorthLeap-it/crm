import { Component, input, model } from '@angular/core';

@Component({
  selector: 'ui-textarea',
  standalone: true,
  template: `
    <textarea
      class="textarea rounded-sm w-full"
      [placeholder]="placeholder()"
      [disabled]="disabled()"
      [rows]="rows()"
      [value]="value()"
      (input)="value.set($any($event.target).value)"
    ></textarea>
  `
})
export class UiTextarea {
  readonly placeholder = input('');
  readonly disabled = input(false);
  readonly rows = input(3);
  readonly value = model('');
}
