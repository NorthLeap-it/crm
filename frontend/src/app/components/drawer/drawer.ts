import { Component, input, output } from '@angular/core';

// pannello laterale
@Component({
  selector: 'app-drawer',
  standalone: true,
  templateUrl: './drawer.html'
})
export class Drawer {
  readonly open = input(false);
  readonly title = input('');
  readonly close = output<void>();
}
