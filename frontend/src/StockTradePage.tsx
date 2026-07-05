import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { stockTradeApi } from "./api";
import type { AuthResponse, StockTrade, StockTradeImportResponse, StockTradePage as StockTradePageData, StockTradeRequest } from "./types";
import { errorMessage } from "./utils";

interface StockTradePageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

interface ManualForm {
  tradeDate: string;
  stockName: string;
  buyQuantity: string;
  buyPrice: string;
  sellQuantity: string;
  sellPrice: string;
  buyMultiplier: string;
  sellMultiplier: string;
}

const emptyData: StockTradePageData = {
  summary: {
    tradeCount: 0,
    winCount: 0,
    lossCount: 0,
    buyTotalAmount: 0,
    sellNetAmount: 0,
    buyFee: 0,
    sellFee: 0,
    profitLoss: 0,
  },
  trades: [],
};

const today = new Date().toISOString().slice(0, 10);

const initialManualForm: ManualForm = {
  tradeDate: today,
  stockName: "",
  buyQuantity: "",
  buyPrice: "",
  sellQuantity: "",
  sellPrice: "",
  buyMultiplier: "1.00032",
  sellMultiplier: "0.99868",
};

function StockTradePage({ user, onBack, onLogout }: StockTradePageProps) {
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [file, setFile] = useState<File | null>(null);
  const [manualForm, setManualForm] = useState<ManualForm>(initialManualForm);
  const [data, setData] = useState<StockTradePageData>(emptyData);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    void load();
  }, []);

  const years = useMemo(() => {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 8 }, (_, index) => String(currentYear - index));
  }, []);

  async function load(targetYear = year): Promise<void> {
    setLoading(true);
    setError("");
    try {
      setData(await stockTradeApi.list(targetYear));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  function updateYear(event: ChangeEvent<HTMLSelectElement | HTMLInputElement>): void {
    setYear(event.target.value);
  }

  function updateFile(event: ChangeEvent<HTMLInputElement>): void {
    setFile(event.target.files?.[0] ?? null);
  }

  function updateManualForm(event: ChangeEvent<HTMLInputElement>): void {
    const { name, value } = event.target;
    setManualForm((current) => ({ ...current, [name]: value }));
  }

  async function upload(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setError("");
    setMessage("");
    if (!file) {
      setError("Excel ファイルを選択してください");
      return;
    }
    if (!/^\d{4}$/.test(year)) {
      setError("年は 4 桁で入力してください。例：2026");
      return;
    }

    setUploading(true);
    try {
      const result = await stockTradeApi.importExcel(file, year);
      setMessage(importMessage(result));
      setFile(null);
      await load(year);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setUploading(false);
    }
  }

  async function submitManual(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setError("");
    setMessage("");
    const payload = manualPayload(manualForm);
    if (!payload.stockName) {
      setError("銘柄名を入力してください");
      return;
    }
    if (payload.buyQuantity === 0 && payload.sellQuantity === 0) {
      setError("買付数量または売却数量を入力してください");
      return;
    }

    setSaving(true);
    try {
      await stockTradeApi.create(payload);
      setMessage("取引データを登録しました");
      setManualForm({ ...initialManualForm, tradeDate: manualForm.tradeDate });
      await load(String(new Date(payload.tradeDate).getFullYear()));
      setYear(String(new Date(payload.tradeDate).getFullYear()));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function remove(trade: StockTrade): Promise<void> {
    if (!window.confirm(`${trade.stockName}（${trade.tradeDate}）の記録を削除しますか？`)) return;
    setError("");
    setMessage("");
    try {
      await stockTradeApi.remove(trade.id);
      setMessage("記録を削除しました");
      await load(year);
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  return (
    <main className="shell stock-shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← ホームへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>

      <header className="hero stock-hero">
        <div>
          <span className="section-number">STOCK TRADING</span>
          <h1>株式取引記録</h1>
          <p>Excel 取込と手動登録で、買付・売却・手数料・損益を管理します。</p>
        </div>
        <div className="summary stock-summary">
          <span>表示年</span>
          <strong>{year}</strong>
          <small>{loading ? "読込中..." : `${data.trades.length} 件`}</small>
        </div>
      </header>

      {(error || message) && <div className={error ? "alert" : "success"}>{error || message}</div>}

      <section className="panel stock-manual-panel">
        <div className="section-heading">
          <div>
            <span className="section-number">MANUAL ENTRY</span>
            <h2>手動登録</h2>
          </div>
        </div>
        <form className="stock-manual-form" onSubmit={submitManual}>
          <label>取引日<input type="date" name="tradeDate" value={manualForm.tradeDate} onChange={updateManualForm} required /></label>
          <label>銘柄名<input name="stockName" value={manualForm.stockName} onChange={updateManualForm} placeholder="例：東興" maxLength={100} required /></label>
          <label>買付数量<input name="buyQuantity" value={manualForm.buyQuantity} onChange={updateManualForm} inputMode="decimal" placeholder="例：5000" /></label>
          <label>買付単価<input name="buyPrice" value={manualForm.buyPrice} onChange={updateManualForm} inputMode="decimal" placeholder="例：12.73" /></label>
          <label>売却数量<input name="sellQuantity" value={manualForm.sellQuantity} onChange={updateManualForm} inputMode="decimal" placeholder="例：5000" /></label>
          <label>売却単価<input name="sellPrice" value={manualForm.sellPrice} onChange={updateManualForm} inputMode="decimal" placeholder="例：13.03" /></label>
          <label>買付倍率<input name="buyMultiplier" value={manualForm.buyMultiplier} onChange={updateManualForm} inputMode="decimal" required /></label>
          <label>売却倍率<input name="sellMultiplier" value={manualForm.sellMultiplier} onChange={updateManualForm} inputMode="decimal" required /></label>
          <button className="primary-button" disabled={saving}>{saving ? "登録中..." : "登録する"}</button>
        </form>
      </section>

      <section className="panel excel-panel stock-upload-panel">
        <div className="excel-panel-heading">
          <div>
            <span className="section-number">EXCEL IMPORT</span>
            <h2>Excel 取込</h2>
            <p>取込時に年を指定します。H 列は「東興6.15」のように銘柄名＋月日で入力してください。</p>
          </div>
          {uploading && <div className="busy-indicator"><span />取込中</div>}
        </div>
        <form className="stock-upload-form" onSubmit={upload}>
          <label>年
            <input list="stock-years" value={year} onChange={updateYear} inputMode="numeric" maxLength={4} required />
            <datalist id="stock-years">{years.map((option) => <option key={option} value={option} />)}</datalist>
          </label>
          <label>Excel ファイル
            <input type="file" accept=".xlsx,.xls" onChange={updateFile} />
          </label>
          <button className="primary-button" disabled={uploading}>{uploading ? "取込中..." : "取込して解析"}</button>
          <button type="button" className="text-button filter-reset-button" onClick={() => load(year)}>表示年を再読込</button>
        </form>
        <div className="stock-column-help">
          <span>A 買付数量</span><span>B 買付単価</span><span>C 買付総額</span><span>D 売却数量</span>
          <span>E 売却単価</span><span>F 売却受取額</span><span>G 損益</span><span>H 銘柄名＋月日</span>
        </div>
      </section>

      <section className="stock-stats">
        <article><span>取引件数</span><strong>{data.summary.tradeCount}</strong><small>利益 {data.summary.winCount} / 損失 {data.summary.lossCount}</small></article>
        <article><span>買付総額</span><strong>{currency(data.summary.buyTotalAmount)}</strong><small>買付手数料を含む</small></article>
        <article><span>売却受取額</span><strong>{currency(data.summary.sellNetAmount)}</strong><small>売却手数料控除後</small></article>
        <article><span>買付手数料</span><strong>{currency(data.summary.buyFee)}</strong><small>C 列の倍率、または手動倍率から計算</small></article>
        <article><span>売却手数料</span><strong>{currency(data.summary.sellFee)}</strong><small>F 列の倍率、または手動倍率から計算</small></article>
        <article><span>合計損益</span><strong className={data.summary.profitLoss >= 0 ? "income-text" : "expense-text"}>{currency(data.summary.profitLoss)}</strong><small>プラスは利益、マイナスは損失</small></article>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div><span className="section-number">DETAILS</span><h2>取引明細</h2><span className="record-count">{data.trades.length} 件</span></div>
        </div>
        {data.trades.length === 0 ? <div className="empty">株式取引記録はまだありません。Excel 取込、または手動登録を行ってください。</div> : (
          <div className="table-wrap stock-table-wrap">
            <table className="stock-table">
              <thead>
                <tr>
                  <th>取引日</th><th>銘柄名</th><th>買付数量</th><th>買付単価</th><th>買付金額</th><th>買付手数料</th><th>買付総額</th>
                  <th>売却数量</th><th>売却単価</th><th>売却金額</th><th>売却手数料</th><th>売却受取額</th><th>損益</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                {data.trades.map((trade) => (
                  <tr key={trade.id}>
                    <td><strong>{dateText(trade.tradeDate)}</strong><span>{trade.sourceRowNumber > 0 ? `Excel ${trade.sourceRowNumber} 行目` : "手動登録"}</span></td>
                    <td><strong>{trade.stockName}</strong><span>{trade.sourceLabel}</span></td>
                    <td>{numberText(trade.buyQuantity)}</td>
                    <td>{currency(trade.buyPrice)}</td>
                    <td>{currency(trade.buyGrossAmount)}</td>
                    <td><strong>{currency(trade.buyFee)}</strong><span>倍率 {trade.buyMultiplier}</span></td>
                    <td>{currency(trade.buyTotalAmount)}</td>
                    <td>{numberText(trade.sellQuantity)}</td>
                    <td>{currency(trade.sellPrice)}</td>
                    <td>{currency(trade.sellGrossAmount)}</td>
                    <td><strong>{currency(trade.sellFee)}</strong><span>倍率 {trade.sellMultiplier}</span></td>
                    <td>{currency(trade.sellNetAmount)}</td>
                    <td><strong className={trade.profitLoss >= 0 ? "income-text" : "expense-text"}>{currency(trade.profitLoss)}</strong></td>
                    <td className="actions"><button className="danger" onClick={() => remove(trade)}>削除</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}

function manualPayload(form: ManualForm): StockTradeRequest {
  return {
    tradeDate: form.tradeDate,
    stockName: form.stockName.trim(),
    buyQuantity: numericValue(form.buyQuantity),
    buyPrice: numericValue(form.buyPrice),
    sellQuantity: numericValue(form.sellQuantity),
    sellPrice: numericValue(form.sellPrice),
    buyMultiplier: numericValue(form.buyMultiplier || "1.00032"),
    sellMultiplier: numericValue(form.sellMultiplier || "0.99868"),
  };
}

function numericValue(value: string): number {
  const normalized = value.replaceAll(",", "").trim();
  return normalized ? Number(normalized) : 0;
}

function importMessage(result: StockTradeImportResponse): string {
  const lines = [`取込完了：${result.imported} 件、スキップ ${result.skipped} 件。`];
  if (result.errors.length > 0) {
    lines.push(result.errors.join("\n"));
  }
  return lines.join("\n");
}

function currency(value: number): string {
  return new Intl.NumberFormat("ja-JP", { style: "currency", currency: "JPY", maximumFractionDigits: 2 }).format(value ?? 0);
}

function numberText(value: number): string {
  return new Intl.NumberFormat("ja-JP", { maximumFractionDigits: 4 }).format(value ?? 0);
}

function dateText(value: string): string {
  return value?.replaceAll("-", "/") ?? "";
}

export default StockTradePage;
