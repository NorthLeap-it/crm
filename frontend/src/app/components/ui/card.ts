import { Component } from '@angular/core';

@Component({
  selector: 'ui-card',
  standalone: true,
  template: `
    <div class="card bg-base-100 rounded-sm border border-base-300 shadow-sm">
      <div class="card-body">
        <ng-content />
      </div>
    </div>
  `
})
export class UiCard {}
