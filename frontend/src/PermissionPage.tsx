import { useEffect, useMemo, useState } from "react";
import { permissionApi } from "./api";
import type { AuthResponse, Permission, UserPermission } from "./types";
import { errorMessage } from "./utils";

interface PermissionPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

export default function PermissionPage({ user, onBack, onLogout }: PermissionPageProps) {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [users, setUsers] = useState<UserPermission[]>([]);
  const [drafts, setDrafts] = useState<Record<number, string[]>>({});
  const [keyword, setKeyword] = useState("");
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

  const filteredUsers = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword) return users;
    return users.filter((target) => target.username.toLowerCase().includes(normalizedKeyword));
  }, [keyword, users]);

  async function toggle(target: UserPermission, code: string): Promise<void> {
    const currentCodes = drafts[target.id] ?? [];
    const nextCodes = currentCodes.includes(code)
      ? currentCodes.filter((value) => value !== code)
      : [...currentCodes, code];
    setDrafts((current) => ({ ...current, [target.id]: nextCodes }));
    setSavingId(target.id);
    setError("");
    setNotice("");
    try {
      const updated = await permissionApi.updateUser(target.id, nextCodes);
      setUsers((current) => current.map((item) => item.id === updated.id ? updated : item));
      setDrafts((current) => ({ ...current, [updated.id]: updated.permissions }));
      setNotice(`${updated.username} の権限を更新しました。`);
    } catch (requestError) {
      setDrafts((current) => ({ ...current, [target.id]: target.permissions }));
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
          <span className="eyebrow">PRIVATE ACCESS CONTROL</span>
          <h1>権限設定</h1>
          <p>ユーザーごとに利用できるページを管理します。</p>
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
          <div><span className="section-number">USER PERMISSIONS</span><h2>ユーザー別権限</h2><span className="record-count">{filteredUsers.length} / {users.length} 件</span></div>
          <button className="refresh-button" onClick={() => void load()} disabled={loading}>
            <span aria-hidden="true">↻</span> {loading ? "読込中..." : "最新情報に更新"}
          </button>
        </div>
        <div className="permission-search">
          <label>ユーザー名検索
            <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="例：jingda" />
          </label>
          <button className="refresh-button" onClick={() => setKeyword("")} disabled={!keyword}>検索条件をクリア</button>
        </div>
        {loading ? <div className="empty">権限データを読み込み中...</div> : (
          filteredUsers.length === 0 ? <div className="empty">検索条件に一致するユーザーがありません。</div> : (
            <div className="table-wrap"><table className="permission-table"><thead><tr><th>ユーザー</th><th>権限</th><th>状態</th></tr></thead>
              <tbody>{filteredUsers.map((target) => (
                <tr key={target.id}>
                  <td><span className="section-number">USER #{target.id}</span><strong>{target.username}</strong></td>
                  <td>
                    <div className="permission-options compact">
                      {permissions.map((permission) => (
                        <label className="permission-option" key={permission.code}>
                          <input
                            type="checkbox"
                            checked={(drafts[target.id] ?? []).includes(permission.code)}
                            onChange={() => void toggle(target, permission.code)}
                            disabled={savingId === target.id}
                          />
                          <span>{permission.label}</span>
                          <small>{permission.code}</small>
                        </label>
                      ))}
                    </div>
                  </td>
                  <td>{savingId === target.id ? "保存中..." : "自動保存"}</td>
                </tr>
              ))}</tbody>
            </table></div>
          )
        )}
      </section>
    </main>
  );
}
