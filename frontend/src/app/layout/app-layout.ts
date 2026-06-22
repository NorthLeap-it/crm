import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideAngularModule,
  Box,
  Home,
  Search,
  Settings,
  LogOut,
  Sun,
  Moon
} from 'lucide-angular';

import { AuthService } from '../services/auth.service';
import { ObjectTypeService } from '../services/object-type.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './app-layout.html'
})
export class AppLayout implements OnInit {
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  protected readonly objects = this.objectTypeService.objects;
  protected readonly user = this.auth.user;
  protected readonly isDark = computed(() => this.theme.theme() === 'dark');

  // icone fisse della shell
  protected readonly HomeIcon = Home;
  protected readonly SearchIcon = Search;
  protected readonly SettingsIcon = Settings;
  protected readonly LogOutIcon = LogOut;
  protected readonly SunIcon = Sun;
  protected readonly MoonIcon = Moon;
  protected readonly BoxIcon = Box;

  ngOnInit(): void {
    this.objectTypeService.load().subscribe();
  }

  protected toggleTheme(): void {
    this.theme.toggle();
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
