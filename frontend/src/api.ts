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
  Employee,
  EmployeeImportResponse,
  EmployeeRequest,
  Permission,
  UserPermission,
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";
const SESSION_KEY = "learning-app-user";

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
    throw new Error(error?.message ?? `リクエストに失敗しました（${response.status}）`);
  }

  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export const employeeApi = {
  list: () => request<Employee[]>("/employees"),
  create: (employee: EmployeeRequest) =>
    request<Employee>("/employees", { method: "POST", body: JSON.stringify(employee) }),
  update: (id: number, employee: EmployeeRequest) =>
    request<Employee>(`/employees/${id}`, { method: "PUT", body: JSON.stringify(employee) }),
  remove: (id: number) => request<void>(`/employees/${id}`, { method: "DELETE" }),
  downloadTemplate: () => download("/employees/template", "employee-upload-template.xlsx"),
  exportExcel: () => download("/employees/export", "employees.xlsx"),
  importExcel: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return request<EmployeeImportResponse>("/employees/import", { method: "POST", body: formData, headers: {} });
  },
};

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
};

export const permissionApi = {
  listPermissions: () => request<Permission[]>("/permissions"),
  listUsers: () => request<UserPermission[]>("/permissions/users"),
  updateUser: (id: number, permissions: string[]) =>
    request<UserPermission>(`/permissions/users/${id}`, { method: "PUT", body: JSON.stringify({ permissions }) }),
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
  const value = localStorage.getItem(SESSION_KEY);
  if (!value) return {};
  try {
    const user = JSON.parse(value) as AuthResponse;
    return user.id ? { "X-User-Id": String(user.id) } : {};
  } catch {
    return {};
  }
}
