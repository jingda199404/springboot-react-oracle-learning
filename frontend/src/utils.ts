export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "不明なエラーが発生しました";
}

export function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

export function formatCurrency(value: number): string {
  return `¥${value.toLocaleString("ja-JP")}`;
}

export function formatDate(date: Date): string {
  return date.toLocaleDateString("ja-JP", { year: "numeric", month: "2-digit", day: "2-digit" });
}
