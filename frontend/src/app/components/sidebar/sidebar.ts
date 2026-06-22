import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, Settings } from 'lucide-angular';

import { resolveObjectIcon } from '../../core/object-icons';
import { ObjectType } from '../../models/object-type';
import { ObjectTypeService } from '../../services/object-type.service';

// Sidebar laterale (lista oggetti dinamica). Possiede il caricamento degli ObjectType. Il host
// e' direttamente l'elemento .drawer-side richiesto da daisyUI (host class), cosi' resta figlio
// diretto del .drawer in app.html.
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
  host: { class: 'drawer-side z-30' }
})
export class Sidebar {
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly objects = this.objectTypeService.objects;
  protected readonly SettingsIcon = Settings;

  constructor() {
    this.objectTypeService.load().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  protected iconFor(obj: ObjectType) {
    return resolveObjectIcon(obj.icon);
  }
}
