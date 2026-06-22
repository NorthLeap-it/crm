import { Component, input } from '@angular/core';

@Component({
  selector: 'ui-spinner',
  standalone: true,
  template: `<span class="loading loading-spinner" [class]="sizeClass()"></span>`
})
export class UiSpinner {
  readonly size = input<'xs' | 'sm' | 'md' | 'lg'>('md');

  protected sizeClass(): string {
    return `loading-${this.size()}`;
  }
}
