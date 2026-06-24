import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { employeeApi } from "./api";
import type { AuthResponse, Employee, EmployeeForm, EmployeeRequest } from "./types";

function createEmptyForm(): EmployeeForm {
  return {
    name: "",
    email: "",
    department: "",
    salary: "",
    hireDate: new Date().toISOString().slice(0, 10),
  };
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "不明なエラーが発生しました";
}

interface EmployeePageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

export default function EmployeePage({ user, onBack, onLogout }: EmployeePageProps) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<EmployeeForm>(createEmptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [fileBusy, setFileBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadEmployees(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      setEmployees(await employeeApi.list());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void loadEmployees(); }, []);

  function updateField(event: ChangeEvent<HTMLInputElement>): void {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function startEdit(employee: Employee): void {
    setEditingId(employee.id);
    setForm({ name: employee.name, email: employee.email, department: employee.department, salary: employee.salary, hireDate: employee.hireDate });
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm(): void {
    setEditingId(null);
    setForm(createEmptyForm());
  }

  async function submit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const payload: EmployeeRequest = { ...form, salary: Number(form.salary) };
      if (editingId !== null) await employeeApi.update(editingId, payload);
      else await employeeApi.create(payload);
      resetForm();
      setNotice(editingId !== null ? "社員情報を更新しました。" : "社員を登録しました。");
      await loadEmployees();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function remove(employee: Employee): Promise<void> {
    if (!window.confirm(`${employee.name} を削除しますか？`)) return;
    setError("");
    setNotice("");
    try {
      await employeeApi.remove(employee.id);
      setNotice("社員を削除しました。");
      await loadEmployees();
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function downloadTemplate(): Promise<void> {
    setFileBusy(true);
    setError("");
    setNotice("");
    try {
      await employeeApi.downloadTemplate();
      setNotice("アップロード用テンプレートをダウンロードしました。");
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setFileBusy(false);
    }
  }

  async function exportEmployees(): Promise<void> {
    setFileBusy(true);
    setError("");
    setNotice("");
    try {
      await employeeApi.exportExcel();
      setNotice("社員一覧を Excel でダウンロードしました。");
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setFileBusy(false);
    }
  }

  async function uploadEmployees(event: ChangeEvent<HTMLInputElement>): Promise<void> {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    setFileBusy(true);
    setError("");
    setNotice("");
    try {
      const result = await employeeApi.importExcel(file);
      setNotice(`Excel 取込完了：登録 ${result.imported} 件、スキップ ${result.skipped} 件`);
      if (result.errors.length > 0) {
        setError(result.errors.slice(0, 5).join("\n"));
      }
      await loadEmployees();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setFileBusy(false);
    }
  }

  const totalSalary = employees.reduce((total, employee) => total + Number(employee.salary), 0);

  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← ホームへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <header className="hero">
        <div><span className="eyebrow">MYSQL WORKFORCE REST API</span><h1>社員管理システム</h1><p>TypeScript React と Spring Boot REST によるフルスタック学習プロジェクト</p></div>
        <div className="summary"><span>社員数</span><strong>{employees.length}</strong><small>月給合計 ¥{totalSalary.toLocaleString("ja-JP")}</small></div>
      </header>
      {notice && <div className="success">{notice}</div>}
      {error && <div className="alert">{error}</div>}
      <section className="panel form-panel">
        <div className="section-heading"><div><span className="section-number">01</span><h2>{editingId !== null ? "社員情報を編集" : "社員を登録"}</h2></div>{editingId !== null && <button className="text-button" onClick={resetForm}>編集をキャンセル</button>}</div>
        <form className="employee-form" onSubmit={submit}>
          <label>氏名<input name="name" value={form.name} onChange={updateField} maxLength={100} required /></label>
          <label>メールアドレス<input name="email" type="email" value={form.email} onChange={updateField} maxLength={150} required /></label>
          <label>部署<input name="department" value={form.department} onChange={updateField} maxLength={100} required /></label>
          <label>月給<input name="salary" type="number" min="0" step="0.01" value={form.salary} onChange={updateField} required /></label>
          <label>入社日<input name="hireDate" type="date" value={form.hireDate} onChange={updateField} required /></label>
          <button className="primary-button" disabled={saving}>{saving ? "保存中..." : editingId !== null ? "変更を保存" : "社員を登録"}</button>
        </form>
      </section>
      <section className="panel excel-panel">
        <div className="excel-panel-heading">
          <div>
            <span className="section-number">02</span>
            <h2>Excel 一括操作</h2>
            <p>テンプレートを使って社員情報をまとめて登録したり、現在の一覧を保存できます。</p>
          </div>
          {fileBusy && <span className="busy-indicator"><span />処理中...</span>}
        </div>
        <div className="excel-actions">
          <article className="excel-action-card">
            <span className="action-step">STEP 1</span>
            <div className="action-copy"><strong>テンプレートを取得</strong><span>入力形式を確認できる Excel ファイル</span></div>
            <button className="file-button secondary-file-button" onClick={() => void downloadTemplate()} disabled={fileBusy}>
              <span aria-hidden="true">↓</span> テンプレート
            </button>
          </article>
          <article className="excel-action-card featured-action-card">
            <span className="action-step">STEP 2</span>
            <div className="action-copy"><strong>社員データを取り込む</strong><span>入力済みの .xlsx ファイルを選択</span></div>
            <label className={`file-button upload-button ${fileBusy ? "disabled" : ""}`}>
              <span aria-hidden="true">↑</span> Excel を選択
              <input type="file" accept=".xlsx" onChange={(event) => void uploadEmployees(event)} disabled={fileBusy} />
            </label>
          </article>
          <article className="excel-action-card">
            <span className="action-step">EXPORT</span>
            <div className="action-copy"><strong>社員一覧を保存</strong><span>現在の登録内容を Excel で出力</span></div>
            <button className="file-button secondary-file-button" onClick={() => void exportEmployees()} disabled={fileBusy || loading}>
              <span aria-hidden="true">↓</span> 一覧を出力
            </button>
          </article>
        </div>
        <p className="excel-hint">対応形式：Excel（.xlsx） · 同じメールアドレスの社員はスキップされます</p>
      </section>
      <section className="panel">
        <div className="section-heading employee-list-heading">
          <div><span className="section-number">03</span><h2>社員一覧</h2><span className="record-count">{employees.length} 件</span></div>
          <button className="refresh-button" onClick={() => void loadEmployees()} disabled={loading}>
            <span aria-hidden="true">↻</span> {loading ? "読込中..." : "最新情報に更新"}
          </button>
        </div>
        {loading ? <div className="empty">MySQL データを読み込み中...</div> : employees.length === 0 ? <div className="empty">社員が登録されていません。最初の社員を登録してください。</div> : (
          <div className="table-wrap"><table><thead><tr><th>社員</th><th>部署</th><th>月給</th><th>入社日</th><th>操作</th></tr></thead>
            <tbody>{employees.map((employee) => <tr key={employee.id}><td><strong>{employee.name}</strong><span>{employee.email}</span></td><td>{employee.department}</td><td>¥{Number(employee.salary).toLocaleString("ja-JP")}</td><td>{employee.hireDate}</td><td className="actions"><button onClick={() => startEdit(employee)}>編集</button><button className="danger" onClick={() => void remove(employee)}>削除</button></td></tr>)}</tbody>
          </table></div>
        )}
      </section>
    </main>
  );
}
