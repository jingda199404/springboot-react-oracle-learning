export interface AuthCredentials {
  username: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  username: string;
  message: string;
}

export interface Employee {
  id: number;
  name: string;
  email: string;
  department: string;
  salary: number;
  hireDate: string;
}

export type EmployeeRequest = Omit<Employee, "id">;

export interface EmployeeForm {
  name: string;
  email: string;
  department: string;
  salary: string | number;
  hireDate: string;
}
