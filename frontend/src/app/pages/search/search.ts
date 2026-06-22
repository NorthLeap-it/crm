import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { UiSpinner } from '../../components/ui/spinner';
import { SearchResult } from '../../models/record';
import { RecordsService } from '../../services/records.service';

// Ricerca globale: GET /api/records/search?q= (min 2 char lato backend, ILIKE sul title, filtrata
// per permesso READ). Debounce per non bombardare il backend a ogni tasto - miglioria rispetto
// all'originale React, che faceva una query a ogni keystroke.
@Component({
  selector: 'app-search',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, UiSpinner],
  templateUrl: './search.html'
})
export class Search {
  private readonly recordsService = inject(RecordsService);

  protected readonly query = new FormControl('', { nonNullable: true });
  protected readonly results = signal<SearchResult[]>([]);
  protected readonly loading = signal(false);
  protected readonly searched = signal(false);

  constructor() {
    this.query.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          const term = q.trim();
          if (term.length < 2) {
            this.results.set([]);
            this.searched.set(false);
            return [];
          }
          this.loading.set(true);
          return this.recordsService.search(term);
        })
      )
      .subscribe({
        next: (res) => {
          this.results.set(res);
          this.loading.set(false);
          this.searched.set(true);
        },
        error: () => this.loading.set(false)
      });
  }
}
