// I 5 ruoli di sistema seminati da RoleSeeder lato backend. Non esiste (ancora) un endpoint REST
// per leggerli/gestirli (manca un RoleController - vedi 06-FRONTEND-OVERVIEW.md sez. B): finche'
// non c'e', li teniamo qui come costante per i dropdown di invito utente / creazione API key.
export const ROLE_KEYS = ['owner', 'admin', 'manager', 'agent', 'viewer'] as const;
export type RoleKey = (typeof ROLE_KEYS)[number];
