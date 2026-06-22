import { ObjectType } from './object-type';

// entities/Record.java — il motore metadata-driven: i valori dei campi dinamici stanno in `data`.
export interface RecordItem {
  id: string;
  title: string;
  status: string | null;
  ownerId: string | null;
  data: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

// entities/RecordLink.java (forma usata in RecordDetailResponse outgoing/incoming)
export interface RecordLink {
  id: string;
  relationKey: string;
  source: RecordItem;
  target: RecordItem;
  data: Record<string, unknown> | null;
}

// dtos/FilterGroup.java — albero ricorsivo AND/OR. Una condizione foglia ha field/operator/value;
// un gruppo ha combinator + conditions (mix di condizioni e sotto-gruppi, duck-typing su
// `combinator` lato backend).
export interface FilterCondition {
  field: string;
  operator: string;
  value?: unknown;
}

export interface FilterGroup {
  combinator: 'AND' | 'OR';
  conditions: (FilterCondition | FilterGroup)[];
}

export interface SortSpec {
  field: string;
  dir: 'asc' | 'desc';
}

// dtos/QueryRecordsDto.java
export interface QueryRecordsDto {
  q?: string;
  status?: string;
  page?: number;
  pageSize?: number;
  filter?: FilterGroup;
  sort?: SortSpec[];
}

// dtos/RecordQueryResponse.java
export interface RecordQueryResponse {
  items: RecordItem[];
  total: number;
  page: number;
  pageSize: number;
  object: ObjectType;
}

// dtos/RecordDetailResponse.java
export interface RecordDetailResponse {
  record: RecordItem;
  outgoing: RecordLink[];
  incoming: RecordLink[];
}

// dtos/UpsertRecordDto.java
export interface UpsertRecordDto {
  title?: string;
  status?: string;
  ownerId?: string;
  data: Record<string, unknown>;
}
