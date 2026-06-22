// dtos/UserSummary.java — NB: e' un record Java, quindi Jackson mantiene il nome del componente
// `isActive` (a differenza delle entity con Lombok @Getter, dove `isXxx` -> `xxx`). Verificato live.
export interface UserSummary {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  isActive: boolean;
  roles: string[];
  createdAt: string;
}

// dtos/InviteCreatedResponse.java — token in chiaro, mostrato una sola volta
export interface InviteCreatedResponse {
  inviteToken: string;
  email: string;
}

// dtos/ApiKeySummary.java
export interface ApiKeySummary {
  id: string;
  name: string;
  prefix: string;
  lastUsedAt: string | null;
  createdAt: string;
}

// dtos/ApiKeyCreatedResponse.java — chiave in chiaro, UNA sola volta
export interface ApiKeyCreatedResponse {
  apiKey: string;
}

// entities/Workflow.java — entity Lombok: il boolean isActive serializza come `active`
export interface Workflow {
  id: string;
  name: string;
  description: string | null;
  trigger: Record<string, unknown>;
  conditions: Record<string, unknown> | null;
  actions: Record<string, unknown>[] | null;
  graph: Record<string, unknown> | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
