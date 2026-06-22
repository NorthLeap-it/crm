import { Component, input } from '@angular/core';

@Component({
  selector: 'ui-label',
  standalone: true,
  template: `
    <label class="block text-sm font-medium mb-1 text-base-content/80">
      <ng-content />
      @if (required()) {
        <span class="text-error">*</span>
      }
    </label>
  `
})
export class UiLabel {
  readonly required = input(false);
}
