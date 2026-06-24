import type {
  AuthCredentials,
  AuthResponse,
  Employee,
  EmployeeImportResponse,
  EmployeeRequest,
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

interface ApiError {
  message?: string;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_BASE}${path}`, {
    headers: isFormData ? options.headers : { "Content-Type": "application/json", ...options.headers },
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

export const authApi = {
  register: (credentials: AuthCredentials) =>
    request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(credentials) }),
  login: (credentials: AuthCredentials) =>
    request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(credentials) }),
};

async function download(path: string, filename: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`);
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
