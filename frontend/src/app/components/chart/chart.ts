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

// chart creato direttamente con js
// niente wrapper angular 
Chart.register(...registerables);

@Component({
  selector: 'app-chart',
  standalone: true,
  templateUrl: './chart.html'
})
export class ChartComponent implements AfterViewInit, OnDestroy {
  // configurazione iniziale con "tela" di disegno
  readonly config = input.required<ChartConfiguration>();
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  // variabili
  private chart?: Chart;
  private viewReady = false;

  constructor() {
    // quando cambia config, cambia anche il disesgno
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
