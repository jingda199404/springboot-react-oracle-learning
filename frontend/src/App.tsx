import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { authApi } from "./api";
import AccountingPage, { type AccountingView } from "./AccountingPage";
import BatchPage from "./BatchPage";
import PermissionPage from "./PermissionPage";
import UserManagementPage from "./UserManagementPage";
import type { AuthResponse } from "./types";
import { clearStoredSession, isSessionExpired, readStoredUser, touchSession, writeStoredUser } from "./session";
import { errorMessage } from "./utils";

const ACCOUNTING_PERMISSION = "ACCOUNTING";
const PERMISSION_SETTING_PERMISSION = "PERMISSION_SETTING";

function navigate(path: string): void {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

interface AuthPageProps {
  mode: "login" | "register";
  onLogin: (user: AuthResponse) => void;
}

interface AuthForm {
  username: string;
  password: string;
  confirmPassword: string;
}

function AuthPage({ mode, onLogin }: AuthPageProps) {
  const isLogin = mode === "login";
  const [form, setForm] = useState<AuthForm>({ username: "", password: "", confirmPassword: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  function updateField(event: ChangeEvent<HTMLInputElement>): void {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setError("");
    if (!isLogin && form.password !== form.confirmPassword) {
      setError("パスワードが一致しません");
      return;
    }

    setSaving(true);
    try {
      const credentials = { username: form.username, password: form.password };
      if (isLogin) {
        onLogin(await authApi.login(credentials));
      } else {
        await authApi.register(credentials);
        navigate("/login");
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-intro">
        <span className="eyebrow">JAVA FULL-STACK LAB</span>
        <h1>学んだ技術を、<br />動く形に。</h1>
        <p>TypeScript React が画面を、Spring Boot REST API が処理を、MySQL がデータを担当します。</p>
        <div className="tech-row"><span>TypeScript</span><span>React</span><span>REST</span><span>MySQL</span></div>
      </section>
      <section className="auth-card">
        <span className="section-number">{isLogin ? "WELCOME BACK" : "CREATE ACCOUNT"}</span>
        <h2>{isLogin ? "学習プロジェクトにログイン" : "新規アカウント登録"}</h2>
        <p>{isLogin ? "ユーザー名とパスワードを入力してください。" : "登録後、各学習モジュールを利用できます。"}</p>
        {error && <div className="alert">{error}</div>}
        <form className="auth-form" onSubmit={submit}>
          <label>ユーザー名<input name="username" value={form.username} onChange={updateField} minLength={3} maxLength={50} required autoFocus /></label>
          <label>パスワード<input name="password" type="password" value={form.password} onChange={updateField} minLength={6} maxLength={72} required /></label>
          {!isLogin && <label>パスワード確認<input name="confirmPassword" type="password" value={form.confirmPassword} onChange={updateField} minLength={6} maxLength={72} required /></label>}
          <button className="primary-button auth-submit" disabled={saving}>{saving ? "処理中..." : isLogin ? "ログイン" : "登録"}</button>
        </form>
        <div className="auth-switch">
          {isLogin ? "アカウントをお持ちでない方" : "すでにアカウントをお持ちの方"}
          <button className="text-button" onClick={() => navigate(isLogin ? "/register" : "/login")}>{isLogin ? "新規登録" : "ログインへ戻る"}</button>
        </div>
      </section>
    </main>
  );
}

interface HomePageProps {
  user: AuthResponse;
  onLogout: () => void;
}

function HomePage({ user, onLogout }: HomePageProps) {
  const canUseAccounting = hasPermission(user, ACCOUNTING_PERMISSION);
  const canUsePermissionSetting = hasPermission(user, PERMISSION_SETTING_PERMISSION);
  const canUseAdmin = canUsePermissionSetting || canUseAccounting;

  return (
    <main className="shell">
      <header className="topbar">
        <div><span className="eyebrow">JAVA FULL-STACK LAB</span><strong>個人開発学習プロジェクト</strong></div>
        <div className="user-menu"><span>こんにちは、{user.username} さん</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <section className="home-hero">
        <div><span className="section-number">PROJECT HOME</span><h1>今日は何を<br />学びますか？</h1><p>実際に動くモジュールから、Java Web プロジェクト全体のデータフローを学びましょう。</p></div>
        <div className="home-orb"><span>01</span><strong>利用可能なモジュール</strong></div>
      </section>
      <section className="module-grid">
        {canUseAccounting && <article className="module-card">
          <span className="module-tag">TYPESCRIPT · REST · JPA · MYSQL</span>
          <h2>家計簿</h2>
          <p>収入と支出を登録し、カテゴリ・日付・メモ付きで MySQL に保存します。日々の記録から REST API と集計表示を学習できます。</p>
          <button className="primary-button" onClick={() => navigate("/accounting")}>家計簿を開く</button>
        </article>}
        {canUseAdmin && <article className="module-card">
          <span className="module-tag">ADMIN · PERMISSION · BATCH</span>
          <h2>管理者ページ</h2>
          <p>権限設定と Batch 実行など、システム管理用の機能をまとめて確認・操作できます。</p>
          <button className="primary-button" onClick={() => navigate("/admin")}>管理者ページを開く</button>
        </article>}
        <article className="module-card coming-soon"><span className="module-tag">NEXT MODULE</span><h2>次のアイデアを形に</h2><p>認証・認可、ファイルアップロード、メッセージキューなどのモジュールを追加できます。</p></article>
      </section>
    </main>
  );
}

interface AdminPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

function AdminPage({ user, onBack, onLogout }: AdminPageProps) {
  const canUseAccounting = hasPermission(user, ACCOUNTING_PERMISSION);
  const canUsePermissionSetting = hasPermission(user, PERMISSION_SETTING_PERMISSION);

  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← ホームへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <section className="home-hero">
        <div>
          <span className="section-number">ADMIN CENTER</span>
          <h1>管理者<br />ページ</h1>
          <p>システム管理に関わる画面をここにまとめています。</p>
        </div>
        <div className="home-orb"><span>AD</span><strong>管理メニュー</strong></div>
      </section>
      <section className="module-grid">
        {canUsePermissionSetting && <article className="module-card">
          <span className="module-tag">USER · PASSWORD · EXCEL</span>
          <h2>ユーザー管理</h2>
          <p>ユーザー一覧の確認、パスワード変更、Excel によるユーザー一括登録を行います。</p>
          <button className="primary-button" onClick={() => navigate("/users")}>ユーザー管理を開く</button>
        </article>}
        {canUsePermissionSetting && <article className="module-card">
          <span className="module-tag">USER · PERMISSION · TABLE</span>
          <h2>権限設定</h2>
          <p>権限マスタとユーザー権限の関連テーブルを使って、各ユーザーが利用できるページを管理します。</p>
          <button className="primary-button" onClick={() => navigate("/permissions")}>権限設定を開く</button>
        </article>}
        {canUseAccounting && <article className="module-card">
          <span className="module-tag">BATCH · SCHEDULE · PARAMETER</span>
          <h2>Batch 実行</h2>
          <p>登録済みの batch を選択し、実行日などのパラメータを指定して手動実行します。定時処理の学習にも使えます。</p>
          <button className="primary-button" onClick={() => navigate("/batches")}>Batch を実行する</button>
        </article>}
      </section>
    </main>
  );
}

interface AccessDeniedPageProps {
  user: AuthResponse;
  onBack: () => void;
  onLogout: () => void;
}

function AccessDeniedPage({ user, onBack, onLogout }: AccessDeniedPageProps) {
  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={onBack}>← ホームへ戻る</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <section className="panel">
        <span className="section-number">ACCESS CONTROL</span>
        <h2>このページを利用する権限がありません</h2>
        <p className="muted-text">ログイン中のユーザーに付与された権限のページだけを表示します。</p>
      </section>
    </main>
  );
}

function hasPermission(user: AuthResponse, permission: string): boolean {
  if (user.permissions?.includes(permission)) return true;
  return !user.permissions && permission === ACCOUNTING_PERMISSION;
}

function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [user, setUser] = useState<AuthResponse | null>(readStoredUser);

  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname);
    window.addEventListener("popstate", updatePath);
    return () => window.removeEventListener("popstate", updatePath);
  }, []);

  useEffect(() => {
    if (!user) return undefined;

    const logoutForTimeout = () => {
      if (!isSessionExpired()) return;
      clearStoredSession();
      setUser(null);
      navigate("/login");
    };
    const validateCurrentSession = () => {
      void authApi.session().catch(() => undefined);
    };
    const recordActivity = () => touchSession();
    const events = ["mousemove", "mousedown", "keydown", "scroll", "touchstart"];
    events.forEach((eventName) => window.addEventListener(eventName, recordActivity, { passive: true }));
    const timer = window.setInterval(() => {
      logoutForTimeout();
      validateCurrentSession();
    }, 30 * 1000);

    return () => {
      events.forEach((eventName) => window.removeEventListener(eventName, recordActivity));
      window.clearInterval(timer);
    };
  }, [user]);

  function login(nextUser: AuthResponse): void {
    writeStoredUser(nextUser);
    setUser(nextUser);
    navigate("/");
  }

  async function logout(): Promise<void> {
    await authApi.logout().catch(() => undefined);
    clearStoredSession();
    setUser(null);
    navigate("/login");
  }

  if (path === "/register") return <AuthPage mode="register" onLogin={login} />;
  if (!user) return <AuthPage mode="login" onLogin={login} />;
  if (path.startsWith("/accounting")) {
    if (!hasPermission(user, ACCOUNTING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/")} onLogout={logout} />;
    const accountingView: AccountingView = path === "/accounting/input" ? "input" : path === "/accounting/records" ? "records" : path === "/accounting/assets" ? "assets" : "home";
    return <AccountingPage user={user} view={accountingView} onBack={() => navigate("/")} onLogout={logout} onNavigate={navigate} />;
  }
  if (path === "/admin") {
    if (!hasPermission(user, PERMISSION_SETTING_PERMISSION) && !hasPermission(user, ACCOUNTING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/")} onLogout={logout} />;
    return <AdminPage user={user} onBack={() => navigate("/")} onLogout={logout} />;
  }
  if (path === "/permissions") {
    if (!hasPermission(user, PERMISSION_SETTING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
    return <PermissionPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
  }
  if (path === "/users") {
    if (!hasPermission(user, PERMISSION_SETTING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
    return <UserManagementPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
  }
  if (path === "/batches") {
    if (!hasPermission(user, ACCOUNTING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
    return <BatchPage user={user} onBack={() => navigate("/admin")} onLogout={logout} />;
  }
  return <HomePage user={user} onLogout={logout} />;
}

export default App;
