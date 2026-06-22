import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

// Placeholder generico per le rotte di prodotto (liste record, dettaglio, ricerca, settings)
// che verranno implementate nelle fasi 2-5. Mostra quale rotta/parametro e' attiva cosi' si
// vede che il routing + guard funzionano.
@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `
    <div>
      <h1 class="text-2xl font-semibold mb-2">{{ title }}</h1>
      @if (objectKey()) {
        <p class="text-base-content/60">Oggetto: <code>{{ objectKey() }}</code></p>
      }
      <p class="text-sm text-base-content/40 mt-4">Questa sezione arriva in una fase successiva.</p>
    </div>
  `
})
export class Placeholder {
  private readonly route = inject(ActivatedRoute);

  title = (this.route.snapshot.data['title'] as string) ?? 'In arrivo';

  protected readonly objectKey = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('objectKey')))
  );
}
