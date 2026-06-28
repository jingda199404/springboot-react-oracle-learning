export interface AuthCredentials {
  username: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  username: string;
  permissions: string[];
  message: string;
  sessionToken: string | null;
}

export interface Permission {
  code: string;
  label: string;
}

export interface UserPermission {
  id: number;
  username: string;
  permissions: string[];
}

export type ManagedUser = UserPermission;

export interface UserImportResponse {
  imported: number;
  skipped: number;
  errors: string[];
}

export interface BatchParameter {
  name: string;
  label: string;
  type: "date";
  required: boolean;
  defaultValue: string;
}

export interface BatchDefinition {
  code: string;
  label: string;
  description: string;
  parameters: BatchParameter[];
}

export interface BatchRunRequest {
  targetDate: string;
}

export interface BatchRunResponse {
  code: string;
  label: string;
  targetDate: string;
  processedCount: number;
  message: string;
  executedAt: string;
}

export type AccountingEntryType = "INCOME" | "EXPENSE";

export interface AccountingEntry {
  id: number;
  entryDate: string;
  type: AccountingEntryType;
  category: string;
  amount: number;
  paymentMethod: string;
  memo: string;
}

export type AccountingEntryRequest = Omit<AccountingEntry, "id">;

export interface AccountingEntryForm {
  entryDate: string;
  type: AccountingEntryType;
  category: string;
  amount: string | number;
  paymentMethod: string;
  memo: string;
}

export type AccountingBalanceType = "ASSET" | "LIABILITY";

export interface AccountingBalance {
  id: number;
  asOfDate: string;
  type: AccountingBalanceType;
  accountName: string;
  category: string;
  amount: number;
  repaymentDay: number | null;
  repaymentAccountName: string;
  lastRepaymentDate: string | null;
  memo: string;
}

export type AccountingBalanceRequest = Omit<AccountingBalance, "id" | "asOfDate" | "category" | "lastRepaymentDate">;

export interface AccountingBalanceForm {
  type: AccountingBalanceType;
  accountName: string;
  amount: string | number;
  repaymentDay: string | number;
  repaymentAccountName: string;
  memo: string;
}
