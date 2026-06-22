import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { BottomNav } from './components/bottom-nav/bottom-nav';
import { Sidebar } from './components/sidebar/sidebar';
import { Topbar } from './components/topbar/topbar';
import { AuthService } from './services/auth.service';

// Root: ospita la shell (sidebar/topbar/bottom-nav + contenuto via router-outlet). La shell si
// mostra solo quando l'utente e' autenticato; login/onboarding (non autenticato) rendono solo il
// router-outlet, senza sidebar.
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Topbar, Sidebar, BottomNav],
  templateUrl: './app.html'
})
export class App {
  protected readonly isAuthenticated = inject(AuthService).isAuthenticated;
}
