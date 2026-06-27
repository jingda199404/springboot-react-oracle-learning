import { useEffect, useState } from "react";
import { permissionApi } from "./api";
import type { AuthResponse, Permission, UserPermission } from "./types";

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "不明なエラーが発生しました";
}

interface PermissionPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

export default function PermissionPage({ user, onBack, onLogout }: PermissionPageProps) {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [users, setUsers] = useState<UserPermission[]>([]);
  const [drafts, setDrafts] = useState<Record<number, string[]>>({});
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function load(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      const [nextPermissions, nextUsers] = await Promise.all([
        permissionApi.listPermissions(),
        permissionApi.listUsers(),
      ]);
      setPermissions(nextPermissions);
      setUsers(nextUsers);
      setDrafts(Object.fromEntries(nextUsers.map((target) => [target.id, target.permissions])));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, []);

  function toggle(userId: number, code: string): void {
    setDrafts((current) => {
      const currentCodes = current[userId] ?? [];
      const nextCodes = currentCodes.includes(code)
        ? currentCodes.filter((value) => value !== code)
        : [...currentCodes, code];
      return { ...current, [userId]: nextCodes };
    });
  }

  async function save(target: UserPermission): Promise<void> {
    setSavingId(target.id);
    setError("");
    setNotice("");
    try {
      const updated = await permissionApi.updateUser(target.id, drafts[target.id] ?? []);
      setUsers((current) => current.map((item) => item.id === updated.id ? updated : item));
      setDrafts((current) => ({ ...current, [updated.id]: updated.permissions }));
      setNotice(`${updated.username} の権限を更新しました。`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSavingId(null);
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
          <span className="eyebrow">PERMISSION TABLE ADMIN</span>
          <h1>権限設定</h1>
          <p>権限マスタとユーザー権限の関連テーブルで、利用できるページを管理します。</p>
        </div>
        <div className="summary">
          <span>ユーザー数</span>
          <strong>{users.length}</strong>
          <small>権限 {permissions.length} 種類</small>
        </div>
      </header>
      {notice && <div className="success">{notice}</div>}
      {error && <div className="alert">{error}</div>}
      <section className="panel">
        <div className="section-heading employee-list-heading">
          <div><span className="section-number">USER PERMISSIONS</span><h2>ユーザー別権限</h2></div>
          <button className="refresh-button" onClick={() => void load()} disabled={loading}>
            <span aria-hidden="true">↻</span> {loading ? "読込中..." : "最新情報に更新"}
          </button>
        </div>
        {loading ? <div className="empty">権限データを読み込み中...</div> : (
          <div className="permission-list">
            {users.map((target) => (
              <article className="permission-card" key={target.id}>
                <div>
                  <span className="section-number">USER #{target.id}</span>
                  <h2>{target.username}</h2>
                </div>
                <div className="permission-options">
                  {permissions.map((permission) => (
                    <label className="permission-option" key={permission.code}>
                      <input
                        type="checkbox"
                        checked={(drafts[target.id] ?? []).includes(permission.code)}
                        onChange={() => toggle(target.id, permission.code)}
                      />
                      <span>{permission.label}</span>
                      <small>{permission.code}</small>
                    </label>
                  ))}
                </div>
                <button className="primary-button" onClick={() => void save(target)} disabled={savingId === target.id}>
                  {savingId === target.id ? "保存中..." : "権限を保存"}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
