import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

// Chart.js diretto (canvas), niente wrapper Angular di terze parti per evitare conflitti di
// peerDependency su Angular 22. Registrazione una tantum di tutti i componenti Chart.js.
Chart.register(...registerables);

@Component({
  selector: 'app-chart',
  standalone: true,
  template: `<div class="relative h-64"><canvas #canvas></canvas></div>`
})
export class ChartComponent implements AfterViewInit, OnDestroy {
  readonly config = input.required<ChartConfiguration>();
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  private chart?: Chart;
  private viewReady = false;

  constructor() {
    // ridisegna quando cambia la config (dopo che la view e' pronta)
    effect(() => {
      const cfg = this.config();
      if (this.viewReady) this.render(cfg);
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.render(this.config());
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render(config: ChartConfiguration): void {
    this.chart?.destroy();
    this.chart = new Chart(this.canvas().nativeElement, config);
  }
}
