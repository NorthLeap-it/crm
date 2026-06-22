import { Component, model } from '@angular/core';

@Component({
  selector: 'ui-switch',
  standalone: true,
  template: `
    <input
      type="checkbox"
      class="toggle toggle-primary"
      [checked]="checked()"
      (change)="checked.set($any($event.target).checked)"
    />
  `
})
export class UiSwitch {
  readonly checked = model(false);
}
