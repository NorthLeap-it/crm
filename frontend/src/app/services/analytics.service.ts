import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../core/api-config';

// dtos/RevenuePoint.java, EfficiencyPoint.java, PipelinePoint.java, ActivityPoint.java
export interface RevenuePoint {
  month: string;
  fatturato: number;
  costi: number;
}
export interface EfficiencyPoint {
  month: string;
  efficienza: number;
}
export interface PipelinePoint {
  name: string;
  value: number;
}
export interface ActivityPoint {
  month: string;
  // chiave accentata preservata dall'API originale (vedi ActivityPoint.java @JsonProperty)
  attività: number;
  completate: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/api/analytics`;

  revenue(): Observable<RevenuePoint[]> {
    return this.http.get<RevenuePoint[]>(`${this.base}/revenue`);
  }
  efficiency(): Observable<EfficiencyPoint[]> {
    return this.http.get<EfficiencyPoint[]>(`${this.base}/efficiency`);
  }
  pipeline(): Observable<PipelinePoint[]> {
    return this.http.get<PipelinePoint[]>(`${this.base}/pipeline`);
  }
  activity(): Observable<ActivityPoint[]> {
    return this.http.get<ActivityPoint[]>(`${this.base}/activity`);
  }
}
