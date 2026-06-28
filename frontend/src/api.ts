import type {
  AccountingBalance,
  AccountingBalanceRequest,
  AccountingEntry,
  AccountingEntryRequest,
  AuthCredentials,
  AuthResponse,
  BatchDefinition,
  BatchRunRequest,
  BatchRunResponse,
  ManagedUser,
  Permission,
  UserImportResponse,
  UserPermission,
} from "./types";
import { clearStoredSession, readStoredUser } from "./session";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

interface ApiError {
  message?: string;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const isFormData = options.body instanceof FormData;
  const authHeaders = authHeader();
  const response = await fetch(`${API_BASE}${path}`, {
    headers: isFormData ? { ...authHeaders, ...options.headers } : { "Content-Type": "application/json", ...authHeaders, ...options.headers },
    ...options,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiError | null;
    if (response.status === 401 && !path.startsWith("/auth/login") && !path.startsWith("/auth/register")) {
      clearStoredSession();
      window.location.href = "/login";
    }
    throw new Error(error?.message ?? `リクエストに失敗しました（${response.status}）`);
  }

  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export const accountingApi = {
  list: () => request<AccountingEntry[]>("/accounting-entries"),
  create: (entry: AccountingEntryRequest) =>
    request<AccountingEntry>("/accounting-entries", { method: "POST", body: JSON.stringify(entry) }),
  update: (id: number, entry: AccountingEntryRequest) =>
    request<AccountingEntry>(`/accounting-entries/${id}`, { method: "PUT", body: JSON.stringify(entry) }),
  remove: (id: number) => request<void>(`/accounting-entries/${id}`, { method: "DELETE" }),
};

export const accountingBalanceApi = {
  list: () => request<AccountingBalance[]>("/accounting-balances"),
  create: (balance: AccountingBalanceRequest) =>
    request<AccountingBalance>("/accounting-balances", { method: "POST", body: JSON.stringify(balance) }),
  update: (id: number, balance: AccountingBalanceRequest) =>
    request<AccountingBalance>(`/accounting-balances/${id}`, { method: "PUT", body: JSON.stringify(balance) }),
  remove: (id: number) => request<void>(`/accounting-balances/${id}`, { method: "DELETE" }),
};

export const authApi = {
  register: (credentials: AuthCredentials) =>
    request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(credentials) }),
  login: (credentials: AuthCredentials) =>
    request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(credentials) }),
  session: () => request<void>("/auth/session"),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
};

export const permissionApi = {
  listPermissions: () => request<Permission[]>("/permissions"),
  listUsers: () => request<UserPermission[]>("/permissions/users"),
  updateUser: (id: number, permissions: string[]) =>
    request<UserPermission>(`/permissions/users/${id}`, { method: "PUT", body: JSON.stringify({ permissions }) }),
};

export const userApi = {
  list: () => request<ManagedUser[]>("/users"),
  updatePassword: (id: number, password: string) =>
    request<ManagedUser>(`/users/${id}/password`, { method: "PUT", body: JSON.stringify({ password }) }),
  downloadTemplate: () => download("/users/template", "user-upload-template.xlsx"),
  importExcel: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return request<UserImportResponse>("/users/import", { method: "POST", body: formData, headers: {} });
  },
};

export const batchApi = {
  list: () => request<BatchDefinition[]>("/batches"),
  run: (code: string, parameters: BatchRunRequest) =>
    request<BatchRunResponse>(`/batches/${code}/run`, { method: "POST", body: JSON.stringify(parameters) }),
};

async function download(path: string, filename: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, { headers: authHeader() });
  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiError | null;
    if (response.status === 401) {
      clearStoredSession();
      window.location.href = "/login";
    }
    throw new Error(error?.message ?? `ダウンロードに失敗しました（${response.status}）`);
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function authHeader(): Record<string, string> {
  const user = readStoredUser();
  return user?.id && user.sessionToken ? { "X-User-Id": String(user.id), "X-Session-Token": user.sessionToken } : {};
}
