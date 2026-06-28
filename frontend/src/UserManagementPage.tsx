import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { userApi } from "./api";
import type { AuthResponse, ManagedUser } from "./types";
import { errorMessage } from "./utils";

interface UserManagementPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

export default function UserManagementPage({ user, onBack, onLogout }: UserManagementPageProps) {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [passwords, setPasswords] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [fileBusy, setFileBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function load(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      setUsers(await userApi.list());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, []);

  function updatePasswordDraft(userId: number, value: string): void {
    setPasswords((current) => ({ ...current, [userId]: value }));
  }

  async function submitPassword(event: FormEvent<HTMLFormElement>, target: ManagedUser): Promise<void> {
    event.preventDefault();
    const password = passwords[target.id] ?? "";
    if (password.length < 6) {
      setError("パスワードは 6 文字以上で入力してください。");
      return;
    }

    setSavingId(target.id);
    setError("");
    setNotice("");
    try {
      await userApi.updatePassword(target.id, password);
      setPasswords((current) => ({ ...current, [target.id]: "" }));
      setNotice(`${target.username} のパスワードを更新しました。`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSavingId(null);
    }
  }

  async function downloadTemplate(): Promise<void> {
    setFileBusy(true);
    setError("");
    setNotice("");
    try {
      await userApi.downloadTemplate();
      setNotice("ユーザー登録テンプレートをダウンロードしました。");
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setFileBusy(false);
    }
  }

  async function uploadUsers(event: ChangeEvent<HTMLInputElement>): Promise<void> {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    setFileBusy(true);
    setError("");
    setNotice("");
    try {
      const result = await userApi.importExcel(file);
      setNotice(`Excel 取込完了：登録 ${result.imported} 件、スキップ ${result.skipped} 件`);
      if (result.errors.length > 0) {
        setError(result.errors.slice(0, 5).join("\n"));
      }
      await load();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setFileBusy(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← 管理者ページへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <header className="hero">
        <div>
          <span className="eyebrow">USER ADMIN</span>
          <h1>ユーザー管理</h1>
          <p>ユーザー情報の確認、パスワード変更、Excel 一括登録を行います。</p>
        </div>
        <div className="summary">
          <span>ユーザー数</span>
          <strong>{users.length}</strong>
          <small>APP_USERS</small>
        </div>
      </header>
      {notice && <div className="success">{notice}</div>}
      {error && <div className="alert multiline-alert">{error}</div>}
      <section className="panel excel-panel">
        <div className="excel-panel-heading">
          <div>
            <span className="section-number">EXCEL IMPORT</span>
            <h2>Excel 一括登録</h2>
            <p>テンプレートにユーザー名、初期パスワード、権限コードを入力してアップロードします。</p>
          </div>
          {fileBusy && <div className="busy-indicator"><span />処理中</div>}
        </div>
        <div className="excel-actions two-actions">
          <article className="excel-action-card">
            <span className="action-step">STEP 1</span>
            <div className="action-copy"><strong>テンプレートを取得</strong><span>入力形式を確認できる Excel ファイル</span></div>
            <button className="file-button secondary-file-button" onClick={() => void downloadTemplate()} disabled={fileBusy}>ダウンロード</button>
          </article>
          <article className="excel-action-card featured-action-card">
            <span className="action-step">STEP 2</span>
            <div className="action-copy"><strong>ユーザーを取り込む</strong><span>入力済みの .xlsx ファイルを選択</span></div>
            <label className="file-button">
              <span aria-hidden="true">↑</span> Excel を選択
              <input type="file" accept=".xlsx" onChange={(event) => void uploadUsers(event)} disabled={fileBusy} />
            </label>
          </article>
        </div>
        <p className="excel-hint">権限コード例：ACCOUNTING,PERMISSION_SETTING · 登録済みユーザー名はスキップされます</p>
      </section>
      <section className="panel">
        <div className="section-heading employee-list-heading">
          <div><span className="section-number">USER LIST</span><h2>ユーザー一覧</h2><span className="record-count">{users.length} 件</span></div>
          <button className="refresh-button" onClick={() => void load()} disabled={loading}>
            <span aria-hidden="true">↻</span> {loading ? "読込中..." : "最新情報に更新"}
          </button>
        </div>
        {loading ? <div className="empty">ユーザーデータを読み込み中...</div> : (
          users.length === 0 ? <div className="empty">ユーザーが登録されていません。</div> : (
            <div className="table-wrap"><table><thead><tr><th>ユーザー</th><th>権限</th><th>パスワード変更</th></tr></thead>
              <tbody>{users.map((target) => (
                <tr key={target.id}>
                  <td><span className="section-number">USER #{target.id}</span><strong>{target.username}</strong></td>
                  <td>{target.permissions.length === 0 ? <span>権限なし</span> : target.permissions.join(", ")}</td>
                  <td>
                    <form className="password-inline-form" onSubmit={(event) => void submitPassword(event, target)}>
                      <input
                        type="password"
                        placeholder="新しいパスワード"
                        value={passwords[target.id] ?? ""}
                        minLength={6}
                        maxLength={72}
                        onChange={(event) => updatePasswordDraft(target.id, event.target.value)}
                      />
                      <button className="primary-button" disabled={savingId === target.id}>
                        {savingId === target.id ? "更新中..." : "変更"}
                      </button>
                    </form>
                  </td>
                </tr>
              ))}</tbody>
            </table></div>
          )
        )}
      </section>
    </main>
  );
}
