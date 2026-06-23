import { Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Bell, Languages, LogOut, LucideAngularModule, Moon, Settings, Sun } from 'lucide-angular';
import { switchMap, timer } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { ThemeService } from '../../services/theme.service';
import { I18nService } from '../../services/i18n';

const NOTIFICATION_POLL_MS = 60_000;

// Barra superiore: logo, hamburger (toggle del drawer in app.html via id), tema, campanella
// notifiche, menu utente. Possiede il polling delle notifiche (niente realtime lato backend).
// :host display:contents -> il <header> diventa figlio diretto del flex .drawer-content, layout
// identico a prima della scomposizione.
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './topbar.html',
  styles: ':host { display: contents; }'
})
export class Topbar {
  private readonly notificationService = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  // servizio per la lingua
  readonly i18n = inject(I18nService);

  protected readonly user = this.auth.user;
  protected readonly notifications = this.notificationService.items;
  protected readonly unreadCount = this.notificationService.unreadCount;
  protected readonly isDark = computed(() => this.theme.theme() === 'dark');

  protected readonly BellIcon = Bell;
  protected readonly SunIcon = Sun;
  protected readonly MoonIcon = Moon;
  protected readonly SettingsIcon = Settings;
  protected readonly LogOutIcon = LogOut;
  protected readonly Languages = Languages;

  constructor() {
    timer(0, NOTIFICATION_POLL_MS)
      .pipe(
        switchMap(() => this.notificationService.refresh()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  protected toggleTheme(): void {
    this.theme.toggle();
  }

  protected markAllRead(): void {
    this.notificationService.markAllRead().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  protected logout(): void {
    this.auth
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.router.navigate(['/login']));
  }
}
