import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { forkJoin } from 'rxjs';

import { ChartComponent } from '../../components/chart/chart';
import { UiCard } from '../../components/ui/card';
import { UiSpinner } from '../../components/ui/spinner';
import { ObjectType } from '../../models/object-type';
import { AnalyticsService } from '../../services/analytics.service';
import { ObjectTypeService } from '../../services/object-type.service';
import { RecordsService } from '../../services/records.service';

interface Kpi {
  object: ObjectType;
  count: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, ChartComponent, UiCard, UiSpinner],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {
  private readonly objectTypeService = inject(ObjectTypeService);
  private readonly recordsService = inject(RecordsService);
  private readonly analytics = inject(AnalyticsService);

  protected readonly kpis = signal<Kpi[]>([]);
  protected readonly loadingKpis = signal(true);
  protected readonly loadingCharts = signal(true);

  protected readonly revenueChart = signal<ChartConfiguration | null>(null);
  protected readonly pipelineChart = signal<ChartConfiguration | null>(null);
  protected readonly activityChart = signal<ChartConfiguration | null>(null);
  protected readonly efficiencyChart = signal<ChartConfiguration | null>(null);

  ngOnInit(): void {
    this.loadKpis();
    this.loadCharts();
  }

  // una KPI card per ObjectType abilitato (max 8): conteggio via query pageSize=1 leggendo total.
  // Spreco noto (scarica anche 1 record), eredita' dall'originale - niente endpoint count dedicato
  // lato backend, da migliorare in futuro.
  private loadKpis(): void {
    this.objectTypeService.load().subscribe((objects) => {
      const enabled = objects.filter((o) => o.enabled).slice(0, 8);
      if (enabled.length === 0) {
        this.loadingKpis.set(false);
        return;
      }
      forkJoin(enabled.map((o) => this.recordsService.query(o.key, { pageSize: 1 }))).subscribe({
        next: (responses) => {
          this.kpis.set(enabled.map((object, i) => ({ object, count: responses[i].total })));
          this.loadingKpis.set(false);
        },
        error: () => this.loadingKpis.set(false)
      });
    });
  }

  private loadCharts(): void {
    forkJoin({
      revenue: this.analytics.revenue(),
      pipeline: this.analytics.pipeline(),
      activity: this.analytics.activity(),
      efficiency: this.analytics.efficiency()
    }).subscribe({
      next: ({ revenue, pipeline, activity, efficiency }) => {
        this.revenueChart.set({
          type: 'line',
          data: {
            labels: revenue.map((p) => p.month),
            datasets: [
              { label: 'Fatturato', data: revenue.map((p) => p.fatturato), borderColor: '#2563eb', backgroundColor: '#2563eb33', fill: true, tension: 0.3 },
              { label: 'Costi', data: revenue.map((p) => p.costi), borderColor: '#ef4444', backgroundColor: '#ef444433', fill: true, tension: 0.3 }
            ]
          },
          options: chartOptions()
        });
        this.pipelineChart.set({
          type: 'doughnut',
          data: {
            labels: pipeline.map((p) => p.name),
            datasets: [{ data: pipeline.map((p) => p.value), backgroundColor: ['#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'] }]
          },
          options: chartOptions()
        });
        this.activityChart.set({
          type: 'bar',
          data: {
            labels: activity.map((p) => p.month),
            datasets: [
              { label: 'Attività', data: activity.map((p) => p['attività']), backgroundColor: '#2563eb' },
              { label: 'Completate', data: activity.map((p) => p.completate), backgroundColor: '#10b981' }
            ]
          },
          options: chartOptions()
        });
        this.efficiencyChart.set({
          type: 'line',
          data: {
            labels: efficiency.map((p) => p.month),
            datasets: [{ label: 'Efficienza %', data: efficiency.map((p) => p.efficienza), borderColor: '#8b5cf6', backgroundColor: '#8b5cf633', fill: true, tension: 0.3 }]
          },
          options: chartOptions()
        });
        this.loadingCharts.set(false);
      },
      error: () => this.loadingCharts.set(false)
    });
  }
}

function chartOptions(): ChartConfiguration['options'] {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } }
  };
}
