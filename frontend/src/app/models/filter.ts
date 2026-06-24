// Modello dei filtri allineato AL NOSTRO backend (RecordFilterCompiler):
// - la condizione foglia ha chiavi { field, op, value, value2 } (NB: `op`, non `operator`)
// - il gruppo ha `combinator` ("and"/"or", il backend lo confronta case-insensitive)
// - operatori supportati = esattamente quelli dello switch in RecordFilterCompiler
export type FilterOp =
  | 'eq' | 'ne'
  | 'contains' | 'startsWith' | 'endsWith'
  | 'gt' | 'gte' | 'lt' | 'lte'
  | 'between' | 'in' | 'nin'
  | 'isEmpty' | 'isNotEmpty';

export interface FilterCondition {
  field: string;
  op: FilterOp;
  value?: unknown;
  value2?: unknown; // solo per `between`
}

export interface FilterGroup {
  combinator: 'and' | 'or';
  conditions: (FilterCondition | FilterGroup)[];
}

// etichette per la UI del filter builder
export const FILTER_OPS: { value: FilterOp; label: string }[] = [
  { value: 'eq', label: '=' },
  { value: 'ne', label: '≠' },
  { value: 'contains', label: 'contiene' },
  { value: 'startsWith', label: 'inizia con' },
  { value: 'endsWith', label: 'finisce con' },
  { value: 'gt', label: '>' },
  { value: 'gte', label: '≥' },
  { value: 'lt', label: '<' },
  { value: 'lte', label: '≤' },
  { value: 'between', label: 'tra' },
  { value: 'in', label: 'in lista' },
  { value: 'nin', label: 'non in lista' },
  { value: 'isEmpty', label: 'è vuoto' },
  { value: 'isNotEmpty', label: 'non vuoto' }
];

// operatori che non richiedono un valore
export const NO_VALUE_OPS: FilterOp[] = ['isEmpty', 'isNotEmpty'];

// "colonne native" filtrabili oltre ai FieldDef dinamici
export const NATIVE_FILTER_FIELDS = [
  { key: 'title', label: 'Titolo' },
  { key: 'status', label: 'Stato' },
  { key: 'createdAt', label: 'Creato il' },
  { key: 'updatedAt', label: 'Aggiornato il' }
];
