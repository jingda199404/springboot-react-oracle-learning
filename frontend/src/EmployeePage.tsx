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
  const [error, setError] = useState("");

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
    try {
      const payload: EmployeeRequest = { ...form, salary: Number(form.salary) };
      if (editingId !== null) await employeeApi.update(editingId, payload);
      else await employeeApi.create(payload);
      resetForm();
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
    try {
      await employeeApi.remove(employee.id);
      await loadEmployees();
    } catch (requestError) {
      setError(errorMessage(requestError));
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
        <div><span className="eyebrow">ORACLE WORKFORCE REST API</span><h1>社員管理システム</h1><p>TypeScript React と Spring Boot REST によるフルスタック学習プロジェクト</p></div>
        <div className="summary"><span>社員数</span><strong>{employees.length}</strong><small>月給合計 ¥{totalSalary.toLocaleString("ja-JP")}</small></div>
      </header>
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
      <section className="panel">
        <div className="section-heading"><div><span className="section-number">02</span><h2>社員一覧</h2></div><button className="text-button" onClick={() => void loadEmployees()} disabled={loading}>更新</button></div>
        {loading ? <div className="empty">Oracle データを読み込み中...</div> : employees.length === 0 ? <div className="empty">社員が登録されていません。最初の社員を登録してください。</div> : (
          <div className="table-wrap"><table><thead><tr><th>社員</th><th>部署</th><th>月給</th><th>入社日</th><th>操作</th></tr></thead>
            <tbody>{employees.map((employee) => <tr key={employee.id}><td><strong>{employee.name}</strong><span>{employee.email}</span></td><td>{employee.department}</td><td>¥{Number(employee.salary).toLocaleString("ja-JP")}</td><td>{employee.hireDate}</td><td className="actions"><button onClick={() => startEdit(employee)}>編集</button><button className="danger" onClick={() => void remove(employee)}>削除</button></td></tr>)}</tbody>
          </table></div>
        )}
      </section>
    </main>
  );
}
