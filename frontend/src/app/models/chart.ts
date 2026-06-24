// entities/Chart.java + dtos relativi. Un Chart è una definizione persistita; il suo risultato si
// ottiene con GET /api/charts/{id}/run (ChartRunResponse).
export type ChartType = 'BAR' | 'LINE' | 'PIE' | 'FUNNEL' | 'KPI' | 'TABLE';

export const CHART_TYPES: ChartType[] = ['BAR', 'LINE', 'PIE', 'FUNNEL', 'KPI', 'TABLE'];

export interface Chart {
  id: string;
  label: string;
  type: ChartType;
  query: Record<string, unknown>;
  createdAt: string;
}

// dtos/ChartDataPoint.java
export interface ChartDataPoint {
  label: string;
  value: number;
}

// dtos/ChartRunResponse.java
export interface ChartRunResponse {
  chart: { id: string; label: string; type: ChartType };
  data: ChartDataPoint[];
}

// query di un chart: oggetto sorgente, raggruppamento, aggregazione
export interface ChartQuery {
  objectKey: string;
  groupBy: string;
  aggregate: 'count' | 'sum';
  field?: string;
}
