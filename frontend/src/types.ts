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

export interface JapaneseSummary {
  totalWords: number;
  dueReviews: number;
  quizQuestions: number;
  articles: number;
  answeredQuestions: number;
  correctAnswers: number;
}

export interface JapaneseWord {
  id: number;
  word: string;
  reading: string;
  meaningZh: string;
  partOfSpeech: string;
  category: string;
  level: string;
  exampleJa: string;
  exampleZh: string;
  masteryStatus: "NEW" | "KNOWN" | "UNSURE" | "UNKNOWN";
  reviewCount: number;
  nextReviewDate: string | null;
}

export interface JapaneseProgressResponse {
  wordId: number;
  masteryStatus: "KNOWN" | "UNSURE" | "UNKNOWN";
  reviewCount: number;
  nextReviewDate: string | null;
  lastReviewedAt: string;
}

export interface JapaneseQuizQuestion {
  id: number;
  questionType: string;
  prompt: string;
  options: Record<"A" | "B" | "C" | "D", string>;
  category: string;
  level: string;
}

export interface JapaneseQuizAnswerResponse {
  questionId: number;
  selectedOption: string;
  correctOption: string;
  correct: boolean;
  explanation: string;
}

export interface JapaneseKnowledgeArticle {
  id: number;
  titleJa: string;
  titleZh: string;
  contentJa: string;
  contentZh: string;
  topic: string;
  readingMinutes: number;
}

export interface JapaneseExternalFetchRequest {
  level: string;
  topic: string;
  keyword: string;
  wordCount: number;
  quizCount: number;
  includeArticle: boolean;
}

export interface JapaneseExternalFetchResponse {
  importedWords: number;
  createdQuizzes: number;
  createdArticle: boolean;
  words: JapaneseWord[];
  quizzes: JapaneseQuizQuestion[];
  article: JapaneseKnowledgeArticle | null;
}

export interface JapaneseHome {
  summary: JapaneseSummary;
  todayWords: JapaneseWord[];
  quizPreview: JapaneseQuizQuestion[];
  randomArticle: JapaneseKnowledgeArticle | null;
}

export interface StockTrade {
  id: number;
  tradeYear: number;
  tradeDate: string;
  stockName: string;
  sourceLabel: string;
  buyQuantity: number;
  buyPrice: number;
  buyGrossAmount: number;
  buyFee: number;
  buyTotalAmount: number;
  sellQuantity: number;
  sellPrice: number;
  sellGrossAmount: number;
  sellFee: number;
  sellNetAmount: number;
  profitLoss: number;
  buyMultiplier: number;
  sellMultiplier: number;
  sourceRowNumber: number;
}

export interface StockTradeSummary {
  tradeCount: number;
  winCount: number;
  lossCount: number;
  buyTotalAmount: number;
  sellNetAmount: number;
  buyFee: number;
  sellFee: number;
  profitLoss: number;
}

export interface StockTradePage {
  summary: StockTradeSummary;
  trades: StockTrade[];
}

export interface StockTradeImportResponse {
  imported: number;
  skipped: number;
  errors: string[];
}

export interface StockTradeRequest {
  tradeDate: string;
  stockName: string;
  buyQuantity: number;
  buyPrice: number;
  sellQuantity: number;
  sellPrice: number;
  buyMultiplier: number;
  sellMultiplier: number;
}
