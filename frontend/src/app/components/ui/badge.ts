import { Component, input } from '@angular/core';

// colore opzionale via inline style (alcuni ObjectType/option portano un colore arbitrario dal
// backend) - se assente, ricade sul badge daisyUI neutro
@Component({
  selector: 'ui-badge',
  standalone: true,
  template: `
    <span class="badge rounded-sm" [class.badge-neutral]="!color()" [style.backgroundColor]="color()" [style.borderColor]="color()" [style.color]="color() ? '#fff' : null">
      <ng-content />
    </span>
  `
})
export class UiBadge {
  readonly color = input<string | null>(null);
}
