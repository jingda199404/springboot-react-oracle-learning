import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { accountingApi, accountingBalanceApi } from "./api";
import type {
  AccountingBalance,
  AccountingBalanceForm,
  AccountingBalanceRequest,
  AccountingBalanceType,
  AccountingEntry,
  AccountingEntryForm,
  AccountingEntryRequest,
  AccountingEntryType,
  AuthResponse,
} from "./types";

const typeLabels: Record<AccountingEntryType, string> = {
  INCOME: "収入",
  EXPENSE: "支出",
};

const categorySuggestions: Record<AccountingEntryType, string[]> = {
  INCOME: ["給与", "副業", "投資", "その他収入"],
  EXPENSE: ["食費", "交通", "住居", "通信", "学習", "娯楽", "その他支出"],
};

const balanceTypeLabels: Record<AccountingBalanceType, string> = {
  ASSET: "資産",
  LIABILITY: "負債",
};

const currentYear = String(new Date().getFullYear());
const currentMonth = String(new Date().getMonth() + 1).padStart(2, "0");

function createEmptyForm(): AccountingEntryForm {
  return {
    entryDate: new Date().toISOString().slice(0, 10),
    type: "EXPENSE",
    category: "食費",
    amount: "",
    paymentMethod: "",
    memo: "",
  };
}

function createEmptyBalanceForm(): AccountingBalanceForm {
  return {
    type: "ASSET",
    accountName: "銀行預金",
    amount: "",
    repaymentDay: "",
    repaymentAccountName: "",
    memo: "",
  };
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "不明なエラーが発生しました";
}

function formatCurrency(value: number): string {
  return `¥${value.toLocaleString("ja-JP")}`;
}

function nextRepaymentDate(day: number): Date {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();
  const thisMonthLastDay = new Date(year, month + 1, 0).getDate();
  const thisMonthDate = new Date(year, month, Math.min(day, thisMonthLastDay));
  if (thisMonthDate >= new Date(year, month, today.getDate())) {
    return thisMonthDate;
  }
  const nextMonthLastDay = new Date(year, month + 2, 0).getDate();
  return new Date(year, month + 1, Math.min(day, nextMonthLastDay));
}

function formatDate(date: Date): string {
  return date.toLocaleDateString("ja-JP", { year: "numeric", month: "2-digit", day: "2-digit" });
}

export type AccountingView = "home" | "input" | "records" | "assets";
type AccountingTypeFilter = AccountingEntryType | "ALL";

interface AccountingPageProps {
  user: AuthResponse;
  view: AccountingView;
  onBack: () => void;
  onLogout: () => void;
  onNavigate: (path: string) => void;
}

export default function AccountingPage({ user, view, onBack, onLogout, onNavigate }: AccountingPageProps) {
  const [entries, setEntries] = useState<AccountingEntry[]>([]);
  const [balances, setBalances] = useState<AccountingBalance[]>([]);
  const [form, setForm] = useState<AccountingEntryForm>(createEmptyForm);
  const [balanceForm, setBalanceForm] = useState<AccountingBalanceForm>(createEmptyBalanceForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingBalanceId, setEditingBalanceId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [balancesLoading, setBalancesLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [balanceSaving, setBalanceSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [filterYear, setFilterYear] = useState(currentYear);
  const [filterMonth, setFilterMonth] = useState(currentMonth);
  const [filterType, setFilterType] = useState<AccountingTypeFilter>("ALL");

  async function loadEntries(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      setEntries(await accountingApi.list());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function loadBalances(): Promise<void> {
    setBalancesLoading(true);
    setError("");
    try {
      setBalances(await accountingBalanceApi.list());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBalancesLoading(false);
    }
  }

  useEffect(() => { void loadEntries(); }, []);
  useEffect(() => { void loadBalances(); }, []);

  const summary = useMemo(() => {
    const income = entries
      .filter((entry) => entry.type === "INCOME")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    const expense = entries
      .filter((entry) => entry.type === "EXPENSE")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    return { income, expense, balance: income - expense };
  }, [entries]);

  const monthlySummary = useMemo(() => {
    const monthEntries = entries.filter((entry) => {
      const [year, month] = entry.entryDate.split("-");
      return year === currentYear && month === currentMonth;
    });
    const income = monthEntries
      .filter((entry) => entry.type === "INCOME")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    const expense = monthEntries
      .filter((entry) => entry.type === "EXPENSE")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    return { income, expense, balance: income - expense };
  }, [entries]);

  const availableYears = useMemo(() => {
    const years = new Set(entries.map((entry) => entry.entryDate.slice(0, 4)));
    years.add(currentYear);
    return [...years].sort((a, b) => Number(b) - Number(a));
  }, [entries]);

  const filteredEntries = useMemo(() => entries.filter((entry) => {
    const [year, month] = entry.entryDate.split("-");
    if (filterYear !== "ALL" && year !== filterYear) return false;
    if (filterMonth !== "ALL" && month !== filterMonth) return false;
    return filterType === "ALL" || entry.type === filterType;
  }), [entries, filterMonth, filterType, filterYear]);

  const filteredSummary = useMemo(() => {
    const income = filteredEntries
      .filter((entry) => entry.type === "INCOME")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    const expense = filteredEntries
      .filter((entry) => entry.type === "EXPENSE")
      .reduce((total, entry) => total + Number(entry.amount), 0);
    return { income, expense, balance: income - expense };
  }, [filteredEntries]);

  const balanceSummary = useMemo(() => {
    const assets = balances
      .filter((balance) => balance.type === "ASSET")
      .reduce((total, balance) => total + Number(balance.amount), 0);
    const liabilities = balances
      .filter((balance) => balance.type === "LIABILITY")
      .reduce((total, balance) => total + Number(balance.amount), 0);
    return { assets, liabilities, netWorth: assets - liabilities };
  }, [balances]);

  const repaymentSummary = useMemo(() => {
    const cardBalances = balances.filter((balance) => balance.type === "LIABILITY" && balance.repaymentDay);
    const repaymentTotal = cardBalances.reduce((total, balance) => total + Number(balance.amount), 0);
    const nextItems = cardBalances
      .map((balance) => ({
        balance,
        date: nextRepaymentDate(Number(balance.repaymentDay)),
      }))
      .sort((a, b) => a.date.getTime() - b.date.getTime());
    const nearest = nextItems[0];
    const today = new Date();
    const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const daysLeft = nearest ? Math.ceil((nearest.date.getTime() - todayStart.getTime()) / 86400000) : null;
    return { repaymentTotal, nearest, daysLeft };
  }, [balances]);

  const assetAccountOptions = useMemo(() => {
    const accounts = new Set<string>();
    balances
      .filter((balance) => balance.type === "ASSET")
      .forEach((balance) => accounts.add(balance.accountName));
    return [...accounts];
  }, [balances]);

  const paymentMethodSuggestions = useMemo(() => {
    const methods = new Set<string>();
    balances.forEach((balance) => methods.add(balance.accountName));
    return [...methods];
  }, [balances]);

  useEffect(() => {
    if (paymentMethodSuggestions.length === 0) return;
    setForm((current) => current.paymentMethod ? current : { ...current, paymentMethod: paymentMethodSuggestions[0] });
  }, [paymentMethodSuggestions]);

  function updateField(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>): void {
    const { name, value } = event.target;
    setForm((current) => {
      if (name === "type") {
        const nextType = value as AccountingEntryType;
        return { ...current, type: nextType, category: categorySuggestions[nextType][0] };
      }
      return { ...current, [name]: value };
    });
  }

  function updateBalanceField(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>): void {
    const { name, value } = event.target;
    setBalanceForm((current) => {
      if (name === "type") {
        const nextType = value as AccountingBalanceType;
        return {
          ...current,
          type: nextType,
          accountName: nextType === "ASSET" ? "銀行預金" : "クレジットカード",
          repaymentDay: nextType === "ASSET" ? "" : current.repaymentDay,
          repaymentAccountName: nextType === "ASSET" ? "" : current.repaymentAccountName,
        };
      }
      return { ...current, [name]: value };
    });
  }

  function startEdit(entry: AccountingEntry): void {
    setEditingId(entry.id);
    setForm({
      entryDate: entry.entryDate,
      type: entry.type,
      category: entry.category,
      amount: entry.amount,
      paymentMethod: paymentMethodSuggestions.includes(entry.paymentMethod) ? entry.paymentMethod : "",
      memo: entry.memo ?? "",
    });
    setError("");
    setNotice("");
    onNavigate("/accounting/input");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm(): void {
    setEditingId(null);
    setForm(createEmptyForm());
  }

  function startBalanceEdit(balance: AccountingBalance): void {
    setEditingBalanceId(balance.id);
    setBalanceForm({
      type: balance.type,
      accountName: balance.accountName,
      amount: balance.amount,
      repaymentDay: balance.repaymentDay ?? "",
      repaymentAccountName: balance.repaymentAccountName ?? "",
      memo: balance.memo ?? "",
    });
    setError("");
    setNotice("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetBalanceForm(): void {
    setEditingBalanceId(null);
    setBalanceForm(createEmptyBalanceForm());
  }

  function resetFilters(): void {
    setFilterYear("ALL");
    setFilterMonth("ALL");
    setFilterType("ALL");
  }

  async function submit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const payload: AccountingEntryRequest = {
        ...form,
        amount: Number(form.amount),
      };
      if (editingId !== null) await accountingApi.update(editingId, payload);
      else await accountingApi.create(payload);
      resetForm();
      setNotice(editingId !== null ? "記帳データを更新しました。" : "記帳データを登録しました。");
      await loadEntries();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function submitBalance(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setBalanceSaving(true);
    setError("");
    setNotice("");
    try {
      const payload: AccountingBalanceRequest = {
        ...balanceForm,
        amount: Number(balanceForm.amount),
        repaymentDay: balanceForm.type === "LIABILITY" && balanceForm.repaymentDay !== "" ? Number(balanceForm.repaymentDay) : null,
        repaymentAccountName: balanceForm.type === "LIABILITY" ? balanceForm.repaymentAccountName : "",
      };
      if (editingBalanceId !== null) await accountingBalanceApi.update(editingBalanceId, payload);
      else await accountingBalanceApi.create(payload);
      resetBalanceForm();
      setNotice(editingBalanceId !== null ? "資産・負債データを更新しました。" : "資産・負債データを登録しました。");
      await loadBalances();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBalanceSaving(false);
    }
  }

  async function remove(entry: AccountingEntry): Promise<void> {
    if (!window.confirm(`${entry.entryDate} の ${typeLabels[entry.type]}「${entry.category}」を削除しますか？`)) return;
    setError("");
    setNotice("");
    try {
      await accountingApi.remove(entry.id);
      setNotice("記帳データを削除しました。");
      await loadEntries();
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function removeBalance(balance: AccountingBalance): Promise<void> {
    if (!window.confirm(`${balance.accountName} を削除しますか？`)) return;
    setError("");
    setNotice("");
    try {
      await accountingBalanceApi.remove(balance.id);
      setNotice("資産・負債データを削除しました。");
      await loadBalances();
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  return (
    <main className="shell accounting-shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={view === "home" ? onBack : () => onNavigate("/accounting")}>← {view === "home" ? "ホームへ戻る" : "家計簿トップへ戻る"}</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <header className="hero accounting-hero">
        <div>
          <span className="eyebrow">MYSQL ACCOUNTING REST API</span>
          <h1>{view === "home" ? "家計簿" : view === "input" ? "記帳入力" : view === "records" ? "記帳記録" : "初期資産・負債"}</h1>
          <p>{view === "home" ? "記帳入力、記録確認、初期資産・負債を分けて、お金の全体像を管理します。" : view === "input" ? "収入と支出を入力して MySQL に保存します。" : view === "records" ? "保存済みの記帳データを一覧で確認できます。" : "銀行預金、証券口座、クレジットカード未払金など、開始時点の資産と負債を登録します。"}</p>
        </div>
        <div className="summary accounting-summary">
          <span>現在の残高</span>
          <strong>{formatCurrency(summary.balance)}</strong>
          <small>収入 {formatCurrency(summary.income)} / 支出 {formatCurrency(summary.expense)}</small>
        </div>
      </header>

      <section className="accounting-stats">
        <article><span>当月収入合計</span><strong className="income-text">{formatCurrency(monthlySummary.income)}</strong></article>
        <article><span>当月支出合計</span><strong className="expense-text">{formatCurrency(monthlySummary.expense)}</strong></article>
        <article><span>当月差額</span><strong className={monthlySummary.balance < 0 ? "expense-text" : "income-text"}>{formatCurrency(monthlySummary.balance)}</strong></article>
      </section>

      {notice && <div className="success">{notice}</div>}
      {error && <div className="alert">{error}</div>}

      {view === "home" && (
        <section className="accounting-menu-grid">
          <article className="module-card accounting-menu-card">
            <span className="module-tag">PAGE 1</span>
            <h2>記帳入力</h2>
            <p>日付、収入・支出、カテゴリ、金額、メモを入力します。毎日の記録をここから追加します。</p>
            <button className="primary-button" onClick={() => onNavigate("/accounting/input")}>記帳入力へ</button>
          </article>
          <article className="module-card accounting-menu-card">
            <span className="module-tag">PAGE 2</span>
            <h2>記帳記録</h2>
            <p>保存済みの記帳データを一覧で確認します。編集・削除・最新情報への更新もここから行えます。</p>
            <button className="primary-button" onClick={() => onNavigate("/accounting/records")}>記帳記録へ</button>
          </article>
          <article className="module-card accounting-menu-card">
            <span className="module-tag">PAGE 3</span>
            <h2>初期資産・負債</h2>
            <p>銀行預金、証券口座、信用カード未払金などを登録し、純資産の出発点を確認します。</p>
            <button className="primary-button" onClick={() => onNavigate("/accounting/assets")}>資産・負債入力へ</button>
          </article>
        </section>
      )}

      {view === "input" && (
        <section className="panel form-panel">
          <div className="section-heading">
            <div><span className="section-number">PAGE 1</span><h2>{editingId !== null ? "記帳データを編集" : "記帳データを登録"}</h2></div>
            <div className="inline-actions">
              {editingId !== null && <button className="text-button" onClick={resetForm}>編集をキャンセル</button>}
              <button className="text-button" onClick={() => onNavigate("/accounting/records")}>記録を見る</button>
            </div>
          </div>
          <form className="accounting-form" onSubmit={submit}>
            <label>日付<input name="entryDate" type="date" value={form.entryDate} onChange={updateField} required /></label>
            <label>種別<select name="type" value={form.type} onChange={updateField} required><option value="EXPENSE">支出</option><option value="INCOME">収入</option></select></label>
            <label>カテゴリ<input name="category" value={form.category} onChange={updateField} list="accounting-categories" maxLength={80} required /></label>
            <datalist id="accounting-categories">{categorySuggestions[form.type].map((category) => <option key={category} value={category} />)}</datalist>
            <label>金額<input name="amount" type="number" min="0.01" step="0.01" value={form.amount} onChange={updateField} required /></label>
            <label>入出金方法
              <select name="paymentMethod" value={form.paymentMethod} onChange={updateField} required disabled={paymentMethodSuggestions.length === 0}>
                <option value="">口座・項目を選択</option>
                {paymentMethodSuggestions.map((method) => <option key={method} value={method}>{method}</option>)}
              </select>
            </label>
            <label className="memo-field">メモ<input name="memo" value={form.memo} onChange={updateField} maxLength={255} placeholder="例：昼食、教材、交通費など" /></label>
            <button className="primary-button" disabled={saving || paymentMethodSuggestions.length === 0}>{saving ? "保存中..." : editingId !== null ? "変更を保存" : "記帳する"}</button>
          </form>
          {paymentMethodSuggestions.length === 0 && <p className="form-hint">入出金方法を選ぶには、先に「初期資産・負債」ページで現金・銀行口座・カードなどの口座項目を登録してください。</p>}
        </section>
      )}

      {view === "records" && (
        <section className="panel">
          <div className="section-heading employee-list-heading">
            <div><span className="section-number">PAGE 2</span><h2>記帳一覧</h2><span className="record-count">{filteredEntries.length} / {entries.length} 件</span></div>
            <div className="inline-actions">
              <button className="text-button" onClick={() => onNavigate("/accounting/input")}>記帳する</button>
              <button className="refresh-button" onClick={() => void loadEntries()} disabled={loading}>
                <span aria-hidden="true">↻</span> {loading ? "読込中..." : "最新情報に更新"}
              </button>
            </div>
          </div>
          <div className="accounting-filter-panel">
            <div className="filter-grid">
              <label>年
                <select value={filterYear} onChange={(event) => setFilterYear(event.target.value)}>
                  <option value="ALL">すべて</option>
                  {availableYears.map((year) => <option key={year} value={year}>{year}年</option>)}
                </select>
              </label>
              <label>月
                <select value={filterMonth} onChange={(event) => setFilterMonth(event.target.value)}>
                  <option value="ALL">すべて</option>
                  {Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, "0")).map((month) => <option key={month} value={month}>{Number(month)}月</option>)}
                </select>
              </label>
              <label>収支区分
                <select value={filterType} onChange={(event) => setFilterType(event.target.value as AccountingTypeFilter)}>
                  <option value="ALL">すべて</option>
                  <option value="EXPENSE">支出</option>
                  <option value="INCOME">収入</option>
                </select>
              </label>
              <button className="refresh-button filter-reset-button" onClick={resetFilters}>条件をリセット</button>
            </div>
            <div className="filtered-summary">
              <span>抽出結果</span>
              <strong>{filteredEntries.length} 件</strong>
              <small>収入 {formatCurrency(filteredSummary.income)} / 支出 {formatCurrency(filteredSummary.expense)} / 差額 {formatCurrency(filteredSummary.balance)}</small>
            </div>
          </div>
          {loading ? <div className="empty">MySQL データを読み込み中...</div> : entries.length === 0 ? <div className="empty">まだ記帳データがありません。記帳入力ページから最初の収入または支出を登録してください。</div> : filteredEntries.length === 0 ? <div className="empty">指定した条件に一致する記帳データはありません。</div> : (
            <div className="table-wrap"><table><thead><tr><th>日付</th><th>種別</th><th>カテゴリ</th><th>入出金方法</th><th>金額</th><th>メモ</th><th>操作</th></tr></thead>
              <tbody>{filteredEntries.map((entry) => <tr key={entry.id}><td>{entry.entryDate}</td><td><span className={`entry-badge ${entry.type.toLowerCase()}`}>{typeLabels[entry.type]}</span></td><td><strong>{entry.category}</strong></td><td>{entry.paymentMethod || "-"}</td><td className={entry.type === "INCOME" ? "income-text" : "expense-text"}>{formatCurrency(Number(entry.amount))}</td><td>{entry.memo || "-"}</td><td className="actions"><button onClick={() => startEdit(entry)}>編集</button><button className="danger" onClick={() => void remove(entry)}>削除</button></td></tr>)}</tbody>
            </table></div>
          )}
        </section>
      )}

      {view === "assets" && (
        <>
          <section className="accounting-stats">
            <article><span>資産合計</span><strong className="income-text">{formatCurrency(balanceSummary.assets)}</strong></article>
            <article><span>負債合計</span><strong className="expense-text">{formatCurrency(balanceSummary.liabilities)}</strong></article>
            <article><span>純資産</span><strong>{formatCurrency(balanceSummary.netWorth)}</strong></article>
          </section>
          <section className="repayment-panel">
            <div>
              <span className="section-number">AUTO REPAYMENT</span>
              <h2>クレジットカード返済予定</h2>
              <p>負債として登録したカード請求額、返済日、返済口座から、直近の返済予定と自動返済対象を計算します。</p>
            </div>
            <div className="repayment-summary">
              <span>返済予定総額</span>
              <strong>{formatCurrency(repaymentSummary.repaymentTotal)}</strong>
              <small>{repaymentSummary.nearest ? `直近：${repaymentSummary.nearest.balance.accountName} / ${repaymentSummary.nearest.balance.repaymentAccountName || "返済口座未設定"} / ${formatDate(repaymentSummary.nearest.date)} / あと ${repaymentSummary.daysLeft} 日` : "返済日付きの負債がありません"}</small>
            </div>
          </section>
          <section className="panel form-panel">
            <div className="section-heading">
              <div><span className="section-number">PAGE 3</span><h2>{editingBalanceId !== null ? "資産・負債を編集" : "資産・負債を登録"}</h2></div>
              <div className="inline-actions">
                {editingBalanceId !== null && <button className="text-button" onClick={resetBalanceForm}>編集をキャンセル</button>}
                <button className="refresh-button" onClick={() => void loadBalances()} disabled={balancesLoading}>
                  <span aria-hidden="true">↻</span> {balancesLoading ? "読込中..." : "最新情報に更新"}
                </button>
              </div>
            </div>
            <form className="balance-form" onSubmit={submitBalance}>
              <label>区分<select name="type" value={balanceForm.type} onChange={updateBalanceField} required><option value="ASSET">資産</option><option value="LIABILITY">負債</option></select></label>
              <label>口座・項目名<input name="accountName" value={balanceForm.accountName} onChange={updateBalanceField} maxLength={100} placeholder="例：三菱UFJ銀行、楽天証券、楽天カード" required /></label>
              <label>金額<input name="amount" type="number" min="0" step="0.01" value={balanceForm.amount} onChange={updateBalanceField} required /></label>
              {balanceForm.type === "LIABILITY" && <label>返済日<input name="repaymentDay" type="number" min="1" max="31" step="1" value={balanceForm.repaymentDay} onChange={updateBalanceField} placeholder="例：27" /></label>}
              {balanceForm.type === "LIABILITY" && (
                <label>返済口座
                  <select name="repaymentAccountName" value={balanceForm.repaymentAccountName} onChange={updateBalanceField} required={balanceForm.repaymentDay !== ""} disabled={assetAccountOptions.length === 0}>
                    <option value="">資産口座を選択</option>
                    {assetAccountOptions.map((accountName) => <option key={accountName} value={accountName}>{accountName}</option>)}
                  </select>
                </label>
              )}
              <label className="memo-field">メモ<input name="memo" value={balanceForm.memo} onChange={updateBalanceField} maxLength={255} placeholder="例：普通預金、NISA評価額、未確定請求など" /></label>
              <button className="primary-button" disabled={balanceSaving}>{balanceSaving ? "保存中..." : editingBalanceId !== null ? "変更を保存" : "登録する"}</button>
            </form>
            {balanceForm.type === "LIABILITY" && assetAccountOptions.length === 0 && <p className="form-hint">返済口座を選ぶには、先に区分「資産」で銀行口座などを登録してください。</p>}
          </section>
          <section className="panel">
            <div className="section-heading employee-list-heading">
              <div><span className="section-number">BALANCE LIST</span><h2>資産・負債一覧</h2><span className="record-count">{balances.length} 件</span></div>
            </div>
            {balancesLoading ? <div className="empty">MySQL データを読み込み中...</div> : balances.length === 0 ? <div className="empty">まだ資産・負債データがありません。銀行預金やカード未払金などを登録してください。</div> : (
              <div className="table-wrap"><table><thead><tr><th>区分</th><th>口座・項目</th><th>金額</th><th>返済日</th><th>返済口座</th><th>次回返済</th><th>最終返済処理日</th><th>メモ</th><th>操作</th></tr></thead>
                <tbody>{balances.map((balance) => {
                  const repaymentDate = balance.repaymentDay ? nextRepaymentDate(Number(balance.repaymentDay)) : null;
                  return <tr key={balance.id}><td><span className={`entry-badge ${balance.type === "ASSET" ? "income" : "expense"}`}>{balanceTypeLabels[balance.type]}</span></td><td><strong>{balance.accountName}</strong></td><td className={balance.type === "ASSET" ? "income-text" : "expense-text"}>{formatCurrency(Number(balance.amount))}</td><td>{balance.repaymentDay ? `毎月 ${balance.repaymentDay}日` : "-"}</td><td>{balance.repaymentAccountName || "-"}</td><td>{repaymentDate ? formatDate(repaymentDate) : "-"}</td><td>{balance.lastRepaymentDate || "-"}</td><td>{balance.memo || "-"}</td><td className="actions"><button onClick={() => startBalanceEdit(balance)}>編集</button><button className="danger" onClick={() => void removeBalance(balance)}>削除</button></td></tr>;
                })}</tbody>
              </table></div>
            )}
          </section>
        </>
      )}
    </main>
  );
}
