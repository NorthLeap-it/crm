import { Component, signal } from '@angular/core';

import { ApiKeysTab } from './tabs/apikeys-tab';
import { ObjectsTab } from './tabs/objects-tab';
import { RolesTab } from './tabs/roles-tab';
import { UsersTab } from './tabs/users-tab';
import { WorkflowsTab } from './tabs/workflows-tab';

type Tab = 'objects' | 'users' | 'apikeys' | 'workflows' | 'roles';

// Settings a tab con stato locale (niente routing per-tab, stesso approccio dell'originale React).
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ObjectsTab, UsersTab, ApiKeysTab, WorkflowsTab, RolesTab],
  templateUrl: './settings.html'
})
export class Settings {
  protected readonly tab = signal<Tab>('objects');

  protected readonly tabs: { id: Tab; label: string }[] = [
    { id: 'objects', label: 'Oggetti' },
    { id: 'users', label: 'Utenti' },
    { id: 'apikeys', label: 'API Key' },
    { id: 'workflows', label: 'Workflow' },
    { id: 'roles', label: 'Ruoli' }
  ];
}
