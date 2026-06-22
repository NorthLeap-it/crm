import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideAngularModule,
  Box,
  Home,
  Search,
  Settings,
  LogOut,
  Sun,
  Moon,
  Bell
} from 'lucide-angular';

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
export class AppLayout implements OnInit, OnDestroy {
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly notificationService = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  private pollHandle?: ReturnType<typeof setInterval>;

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
  protected readonly BoxIcon = Box;

  ngOnInit(): void {
    this.objectTypeService.load().subscribe();
    this.notificationService.refresh().subscribe();
    // polling: niente realtime lato backend, si aggiorna a intervalli (vedi NotificationService)
    this.pollHandle = setInterval(() => this.notificationService.refresh().subscribe(), NOTIFICATION_POLL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  protected toggleTheme(): void {
    this.theme.toggle();
  }

  protected markAllRead(): void {
    this.notificationService.markAllRead().subscribe();
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
