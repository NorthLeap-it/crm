// Sottoinsieme di ObjectType lato backend (entities/ObjectType.java) usato in Fase 0 per la
// sidebar. I FieldDef e il CRUD completo arrivano in Fase 2 (motore dinamico).
export interface ObjectType {
  id: string;
  key: string;
  label: string;
  pluralLabel: string;
  icon: string | null;
  color: string | null;
  isSystem: boolean;
  isEnabled: boolean;
  sortOrder: number;
}
