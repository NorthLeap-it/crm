import { Component, DestroyRef, OnInit, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { switchMap, timer } from 'rxjs';
import {
  LucideAngularModule,
  Home,
  Search,
  Settings,
  LogOut,
  Sun,
  Moon,
  Bell
} from 'lucide-angular';

import { resolveObjectIcon } from '../core/object-icons';
import { ObjectType } from '../models/object-type';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { ObjectTypeService } from '../services/object-type.service';
import { ThemeService } from '../services/theme.service';

const NOTIFICATION_POLL_MS = 60_000;

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './app-layout.html'
})
export class AppLayout implements OnInit {
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly notificationService = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly objects = this.objectTypeService.objects;
  protected readonly user = this.auth.user;
  protected readonly notifications = this.notificationService.items;
  protected readonly unreadCount = this.notificationService.unreadCount;
  protected readonly isDark = computed(() => this.theme.theme() === 'dark');

  // icone fisse della shell
  protected readonly HomeIcon = Home;
  protected readonly SearchIcon = Search;
  protected readonly SettingsIcon = Settings;
  protected readonly LogOutIcon = LogOut;
  protected readonly SunIcon = Sun;
  protected readonly MoonIcon = Moon;
  protected readonly BellIcon = Bell;

  // icona per ObjectType, risolta dal campo `icon` del backend (vedi core/object-icons.ts)
  protected iconFor(obj: ObjectType) {
    return resolveObjectIcon(obj.icon);
  }

  ngOnInit(): void {
    this.objectTypeService.load().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();

    // polling notifiche: niente realtime lato backend (vedi NotificationService), si rinfresca a
    // intervalli. timer(0, N) parte subito e poi ogni N ms; switchMap annulla la chiamata in volo
    // se ne parte un'altra. Teardown automatico con takeUntilDestroyed.
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
