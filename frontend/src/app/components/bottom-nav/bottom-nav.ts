import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Home, LucideAngularModule, Search, Settings } from 'lucide-angular';

// Bottom-nav mobile (Home/Cerca/Impostazioni). Nessuno stato proprio. display:contents -> il
// <nav> diventa figlio diretto del flex .drawer-content.
@Component({
  selector: 'app-bottom-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './bottom-nav.html',
  styles: ':host { display: contents; }'
})
export class BottomNav {
  protected readonly HomeIcon = Home;
  protected readonly SearchIcon = Search;
  protected readonly SettingsIcon = Settings;
}
