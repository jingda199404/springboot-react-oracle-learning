import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { batchApi } from "./api";
import type { AuthResponse, BatchDefinition, BatchRunResponse } from "./types";
import { errorMessage, todayIsoDate } from "./utils";

interface BatchPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

export default function BatchPage({ user, onBack, onLogout }: BatchPageProps) {
  const [batches, setBatches] = useState<BatchDefinition[]>([]);
  const [selectedCode, setSelectedCode] = useState("");
  const [targetDate, setTargetDate] = useState(todayIsoDate);
  const [result, setResult] = useState<BatchRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState("");

  async function load(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      const nextBatches = await batchApi.list();
      setBatches(nextBatches);
      if (nextBatches.length > 0) {
        setSelectedCode((current) => current || nextBatches[0].code);
        const targetDateParameter = nextBatches[0].parameters.find((parameter) => parameter.name === "targetDate");
        setTargetDate(targetDateParameter?.defaultValue || todayIsoDate());
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, []);

  const selectedBatch = useMemo(
    () => batches.find((batch) => batch.code === selectedCode) ?? null,
    [batches, selectedCode],
  );

  function updateBatch(event: ChangeEvent<HTMLSelectElement>): void {
    const nextCode = event.target.value;
    setSelectedCode(nextCode);
    setResult(null);
    const nextBatch = batches.find((batch) => batch.code === nextCode);
    const targetDateParameter = nextBatch?.parameters.find((parameter) => parameter.name === "targetDate");
    setTargetDate(targetDateParameter?.defaultValue || todayIsoDate());
  }

  async function run(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (!selectedBatch) return;
    setRunning(true);
    setError("");
    setResult(null);
    try {
      setResult(await batchApi.run(selectedBatch.code, { targetDate }));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setRunning(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← ホームへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <header className="hero">
        <div>
          <span className="eyebrow">MANUAL BATCH RUNNER</span>
          <h1>Batch 実行</h1>
          <p>実行したい batch を選び、必要なパラメータを指定して REST API から手動実行します。</p>
        </div>
        <div className="summary">
          <span>登録済み Batch</span>
          <strong>{batches.length}</strong>
          <small>{selectedBatch?.label ?? "未選択"}</small>
        </div>
      </header>

      {error && <div className="alert">{error}</div>}
      {result && <div className="success">{result.message}</div>}

      <section className="panel form-panel">
        <div className="section-heading employee-list-heading">
          <div><span className="section-number">RUN CONFIG</span><h2>実行条件</h2></div>
          <button className="refresh-button" onClick={() => void load()} disabled={loading}>
            <span aria-hidden="true">↻</span> {loading ? "読込中..." : "Batch 一覧を更新"}
          </button>
        </div>
        {loading ? <div className="empty">Batch 情報を読み込み中...</div> : batches.length === 0 ? <div className="empty">実行できる batch がありません。</div> : (
          <form className="batch-form" onSubmit={run}>
            <label>Batch
              <select value={selectedCode} onChange={updateBatch} required>
                {batches.map((batch) => <option key={batch.code} value={batch.code}>{batch.label}</option>)}
              </select>
            </label>
            <label>実行日
              <input type="date" value={targetDate} onChange={(event) => setTargetDate(event.target.value)} required />
            </label>
            <button className="primary-button" disabled={running || !selectedBatch}>{running ? "実行中..." : "Batch を実行"}</button>
          </form>
        )}
        {selectedBatch && <p className="form-hint">{selectedBatch.description}</p>}
      </section>

      {result && (
        <section className="panel">
          <div className="section-heading">
            <div><span className="section-number">RUN RESULT</span><h2>実行結果</h2></div>
          </div>
          <div className="batch-result-grid">
            <article><span>Batch</span><strong>{result.label}</strong><small>{result.code}</small></article>
            <article><span>実行日</span><strong>{result.targetDate}</strong><small>指定パラメータ</small></article>
            <article><span>処理件数</span><strong>{result.processedCount}</strong><small>返済処理済み</small></article>
            <article><span>実行時刻</span><strong>{new Date(result.executedAt).toLocaleString("ja-JP")}</strong><small>API 実行結果</small></article>
          </div>
        </section>
      )}
    </main>
  );
}
