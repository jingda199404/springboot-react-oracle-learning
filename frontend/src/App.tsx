import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { authApi } from "./api";
import AccountingPage, { type AccountingView } from "./AccountingPage";
import BatchPage from "./BatchPage";
import JapaneseLearningPage, { type JapaneseLearningView } from "./JapaneseLearningPage";
import PermissionPage from "./PermissionPage";
import StockTradePage from "./StockTradePage";
import UserManagementPage from "./UserManagementPage";
import type { AuthResponse } from "./types";
import { clearStoredSession, isSessionExpired, readStoredUser, touchSession, writeStoredUser } from "./session";
import { errorMessage } from "./utils";

const ACCOUNTING_PERMISSION = "ACCOUNTING";
const PERMISSION_SETTING_PERMISSION = "PERMISSION_SETTING";
const JAPANESE_LEARNING_PERMISSION = "JAPANESE_LEARNING";
const STOCK_TRADING_PERMISSION = "STOCK_TRADING";

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
        <span className="eyebrow">JINGDA PRIVATE HUB</span>
        <h1>毎日のことを、<br />静かに整える。</h1>
        <p>家計、資産、ユーザー管理、定時処理をひとつにまとめた Jingda 専用のプライベートシステムです。</p>
        <div className="tech-row"><span>Finance</span><span>Assets</span><span>Admin</span><span>Batch</span></div>
      </section>
      <section className="auth-card">
        <span className="section-number">{isLogin ? "PRIVATE LOGIN" : "CREATE ACCOUNT"}</span>
        <h2>{isLogin ? "Jingda Hub にログイン" : "新規アカウント登録"}</h2>
        <p>{isLogin ? "ユーザー名とパスワードで入室してください。" : "利用者を登録し、必要な権限を設定します。"}</p>
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
  const canUseJapaneseLearning = hasPermission(user, JAPANESE_LEARNING_PERMISSION);
  const canUseStockTrading = hasPermission(user, STOCK_TRADING_PERMISSION);
  const canUsePermissionSetting = hasPermission(user, PERMISSION_SETTING_PERMISSION);
  const canUseAdmin = canUsePermissionSetting || canUseAccounting;

  return (
    <main className="shell">
      <header className="topbar">
        <div><span className="eyebrow">JINGDA PRIVATE HUB</span><strong>Personal Operations Console</strong></div>
        <div className="user-menu"><span>こんにちは、{user.username} さん</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <section className="home-hero">
        <div><span className="section-number">PRIVATE HOME</span><h1>今日は何を<br />整えますか？</h1><p>家計簿、資産・負債、ユーザー権限、日次処理をここから管理します。</p></div>
        <div className="home-orb"><span>JD</span><strong>Private Console</strong></div>
      </section>
      <section className="module-grid">
        {canUseAccounting && <article className="module-card">
          <span className="module-tag">FINANCE</span>
          <h2>家計簿</h2>
          <p>収入・支出・口座別の動きを記録し、毎月のお金の流れを確認します。</p>
          <button className="primary-button" onClick={() => navigate("/accounting")}>家計簿を開く</button>
        </article>}
        {canUseStockTrading && <article className="module-card">
          <span className="module-tag">STOCKS</span>
          <h2>株式取引記録</h2>
          <p>Excel 取込または手動登録で、買付・売却・手数料・損益を管理します。</p>
          <button className="primary-button" onClick={() => navigate("/stocks")}>株式取引を開く</button>
        </article>}
        {canUseAdmin && <article className="module-card">
          <span className="module-tag">ADMIN</span>
          <h2>管理者ページ</h2>
          <p>ユーザー、権限、手動 batch 実行など、システム管理用の機能をまとめています。</p>
          <button className="primary-button" onClick={() => navigate("/admin")}>管理者ページを開く</button>
        </article>}
        {canUseJapaneseLearning && <article className="module-card">
          <span className="module-tag">JAPANESE</span>
          <h2>日本語学習</h2>
          <p>単語用法、暗記カード、問題練習、5分読書で毎日少しずつ日本語を積み上げます。</p>
          <button className="primary-button" onClick={() => navigate("/japanese")}>日本語を学ぶ</button>
        </article>}
        <article className="module-card coming-soon"><span className="module-tag">PRIVATE ROADMAP</span><h2>次に追加するもの</h2><p>健康管理、メモ、投資メモなど、生活に合わせて機能を増やしていけます。</p></article>
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
          <span className="module-tag">USERS</span>
          <h2>ユーザー管理</h2>
          <p>利用者の確認、パスワード変更、Excel 一括登録、不要なユーザーの削除を行います。</p>
          <button className="primary-button" onClick={() => navigate("/users")}>ユーザー管理を開く</button>
        </article>}
        {canUsePermissionSetting && <article className="module-card">
          <span className="module-tag">ACCESS CONTROL</span>
          <h2>権限設定</h2>
          <p>ユーザーごとに利用できるページを設定し、個人データの見える範囲を管理します。</p>
          <button className="primary-button" onClick={() => navigate("/permissions")}>権限設定を開く</button>
        </article>}
        {canUseAccounting && <article className="module-card">
          <span className="module-tag">OPERATIONS</span>
          <h2>Batch 実行</h2>
          <p>登録済みの batch を選択し、実行日などのパラメータを指定して手動実行します。</p>
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
      void authApi.session()
        .then((nextUser) => {
          writeStoredUser(nextUser);
          setUser(nextUser);
        })
        .catch(() => undefined);
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
  if (path.startsWith("/japanese")) {
    if (!hasPermission(user, JAPANESE_LEARNING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/")} onLogout={logout} />;
    const japaneseView: JapaneseLearningView = path === "/japanese/words" ? "words" : path === "/japanese/memory" ? "memory" : path === "/japanese/quiz" ? "quiz" : path === "/japanese/articles" ? "articles" : "home";
    return <JapaneseLearningPage user={user} view={japaneseView} onBack={() => navigate("/")} onLogout={logout} onNavigate={navigate} />;
  }
  if (path === "/stocks") {
    if (!hasPermission(user, STOCK_TRADING_PERMISSION)) return <AccessDeniedPage user={user} onBack={() => navigate("/")} onLogout={logout} />;
    return <StockTradePage user={user} onBack={() => navigate("/")} onLogout={logout} />;
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
