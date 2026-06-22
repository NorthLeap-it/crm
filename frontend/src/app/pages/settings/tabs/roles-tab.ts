import { Component } from '@angular/core';

import { ROLE_KEYS } from '../../../core/roles';

// La gestione ruoli/permessi non e' esposta via API dal backend (non esiste un RoleController -
// vedi 06-FRONTEND-OVERVIEW.md sez. B). I ruoli sono seminati e gestiti via DB/seed. Mostriamo
// solo l'elenco dei ruoli di sistema in sola lettura, finche' non c'e' un endpoint dedicato.
@Component({
  selector: 'app-roles-tab',
  standalone: true,
  template: `
    <div>
      <div class="alert alert-info rounded-sm py-2 text-sm mb-4">
        La gestione di ruoli e permessi non è ancora disponibile via interfaccia: il backend non
        espone endpoint REST per i ruoli (sono definiti a livello di seed). Qui sotto i ruoli di
        sistema, in sola lettura.
      </div>
      <ul class="border border-base-300 rounded-sm divide-y divide-base-300">
        @for (r of roleKeys; track r) {
          <li class="p-3 font-medium capitalize">{{ r }}</li>
        }
      </ul>
    </div>
  `
})
export class RolesTab {
  protected readonly roleKeys = ROLE_KEYS;
}
