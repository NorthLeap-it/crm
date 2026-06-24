import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FieldDef, FieldOption } from '../../models/object-type';
import { RecordItem } from '../../models/record';

// Vista Kanban: una colonna per ogni opzione del campo STATUS, le card sono i record con quello
// status. Sola lettura (link al dettaglio); lo spostamento drag tra colonne è un'evoluzione futura.
@Component({
  selector: 'app-record-board',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './record-board.html'
})
export class RecordBoard {
  readonly items = input.required<RecordItem[]>();
  readonly statusField = input.required<FieldDef>();
  readonly objectKey = input.required<string>();

  // raggruppa i record per valore di status, seguendo l'ordine delle opzioni del campo
  protected readonly groups = computed(() => {
    const options: FieldOption[] = this.statusField().options ?? [];
    const items = this.items();
    return options.map((o) => ({
      ...o,
      records: items.filter((r) => r.status === o.value)
    }));
  });
}
