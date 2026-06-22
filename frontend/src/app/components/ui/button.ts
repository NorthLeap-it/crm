import { Component, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'accent' | 'ghost' | 'danger';
export type ButtonType = 'button' | 'submit' | 'reset';

const VARIANT_CLASS: Record<ButtonVariant, string> = {
  primary: 'btn-primary',
  accent: 'btn-accent',
  ghost: 'btn-ghost',
  danger: 'btn-error'
};

@Component({
  selector: 'ui-button',
  standalone: true,
  template: `
    <button [type]="type()" class="btn rounded-sm {{ VARIANT_CLASS[variant()] }}" [disabled]="disabled() || loading()">
      @if (loading()) {
        <span class="loading loading-spinner loading-sm"></span>
      }
      <ng-content />
    </button>
  `
})
export class UiButton {
  readonly variant = input<ButtonVariant>('primary');
  readonly type = input<ButtonType>('button');
  readonly disabled = input(false);
  readonly loading = input(false);

  protected readonly VARIANT_CLASS = VARIANT_CLASS;
}
