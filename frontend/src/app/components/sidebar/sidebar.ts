import { Component, DestroyRef, NgZone, Renderer2, WritableSignal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, Settings } from 'lucide-angular';

import { resolveObjectIcon } from '../../core/object-icons';
import { ObjectType } from '../../models/object-type';
import { ObjectTypeService } from '../../services/object-type.service';

const MIN_WIDTH = 64; // rail con sole icone
const MAX_WIDTH = 200;
const LABEL_THRESHOLD = 150; // oltre questa larghezza ricompaiono le etichette
const STORAGE_KEY = 'sidebar-width';

// Sidebar ridimensionabile: di default un rail stretto con sole icone (piu' grandi); trascinando
// il bordo destro la si allarga, e oltre LABEL_THRESHOLD ricompaiono le etichette. La larghezza
// e' persistita in localStorage. Il host e' direttamente l'elemento .drawer-side di daisyUI.
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
  host: { class: 'drawer-side z-30' }
})
export class Sidebar {

  // servizi
  private readonly objectTypeService = inject(ObjectTypeService);

  // gestisco il ciclo di vita
  private readonly destroyRef = inject(DestroyRef);

  // navigazione + zona/renderer per il drag fuori da Angular
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly renderer = inject(Renderer2);

  // mi prendo la lista di object dal service omonimo
  protected readonly objects = this.objectTypeService.objects;
  // icona settings
  protected readonly SettingsIcon = Settings;

  // grandezza, cambiata direttamente tramite il signal
  protected readonly width: WritableSignal<number> = signal(readInitialWidth());
  protected readonly dragging: WritableSignal<boolean> = signal(false);
  protected readonly showLabels = computed(() => this.width() >= LABEL_THRESHOLD);

  // stato interno del drag
  private startX = 0;
  private startWidth = 0;
  private pendingWidth = 0;
  private frame = 0;
  private stopMove?: () => void;
  private stopUp?: () => void;

  protected iconFor(obj: ObjectType) {
    return resolveObjectIcon(obj.icon);
  }

  // appena nasce, carico tutti gli oggetti, gestendo la memoria grazie al destroyRef
  constructor() {
    this.objectTypeService.load().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    // se il componente muore durante un drag, pulisco listener e stili sul body
    this.destroyRef.onDestroy(() => this.teardown());
  }

  // inizio del drag: registro mousemove/mouseup SOLO ora e FUORI dalla zona Angular, cosi' i
  // movimenti del mouse non scatenano change detection a ogni frame. Lavoro a delta da startX
  // (niente getBoundingClientRect per frame -> niente reflow forzato).
  protected startDrag(event: MouseEvent): void {
    event.preventDefault();
    this.startX = event.clientX;
    this.startWidth = this.width();
    this.pendingWidth = this.startWidth;
    this.dragging.set(true);

    this.renderer.setStyle(document.body, 'user-select', 'none');
    this.renderer.setStyle(document.body, 'cursor', 'col-resize');

    this.zone.runOutsideAngular(() => {
      this.stopMove = this.renderer.listen('document', 'mousemove', (e: MouseEvent) => this.onMove(e));
      this.stopUp = this.renderer.listen('document', 'mouseup', () => this.endDrag());
    });
  }

  private onMove(event: MouseEvent): void {
    const delta = event.clientX - this.startX;
    this.pendingWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, this.startWidth + delta));
    // throttle: al massimo un aggiornamento per frame, e rientro in Angular solo nel rAF
    if (this.frame) return;
    this.frame = requestAnimationFrame(() => {
      this.frame = 0;
      this.zone.run(() => this.width.set(this.pendingWidth));
    });
  }

  private endDrag(): void {
    this.zone.run(() => {
      this.dragging.set(false);
      this.width.set(this.pendingWidth);
      localStorage.setItem(STORAGE_KEY, String(this.pendingWidth));
    });
    this.teardown();
  }

  private teardown(): void {
    if (this.frame) {
      cancelAnimationFrame(this.frame);
      this.frame = 0;
    }
    this.stopMove?.();
    this.stopUp?.();
    this.stopMove = undefined;
    this.stopUp = undefined;
    this.renderer.removeStyle(document.body, 'user-select');
    this.renderer.removeStyle(document.body, 'cursor');
  }

  // metodo che torna alla dashboard
  toDashboard(): void {
    this.router.navigateByUrl('');
  }
}

function readInitialWidth(): number {
  const stored = Number(localStorage.getItem(STORAGE_KEY));
  if (stored >= MIN_WIDTH && stored <= MAX_WIDTH) return stored;
  return MIN_WIDTH;
}
