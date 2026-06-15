import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { authApi } from "./api";
import EmployeePage from "./EmployeePage";
import type { AuthResponse } from "./types";

const SESSION_KEY = "learning-app-user";

function navigate(path: string): void {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "不明なエラーが発生しました";
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
        <p>TypeScript React が画面を、Spring Boot REST API が処理を、Oracle がデータを担当します。</p>
        <div className="tech-row"><span>TypeScript</span><span>React</span><span>REST</span><span>Oracle</span></div>
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
        <article className="module-card">
          <span className="module-tag">TYPESCRIPT · REST · JPA · ORACLE</span>
          <h2>社員管理システム</h2>
          <p>社員データの登録・取得・編集・削除を通して、TypeScript React から Spring Boot REST API、Oracle までの流れを学習します。</p>
          <button className="primary-button" onClick={() => navigate("/employees")}>社員管理を開く</button>
        </article>
        <article className="module-card coming-soon"><span className="module-tag">NEXT MODULE</span><h2>次のアイデアを形に</h2><p>認証・認可、ファイルアップロード、メッセージキューなどのモジュールを追加できます。</p></article>
      </section>
    </main>
  );
}

function readStoredUser(): AuthResponse | null {
  const value = localStorage.getItem(SESSION_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value) as AuthResponse;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [user, setUser] = useState<AuthResponse | null>(readStoredUser);

  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname);
    window.addEventListener("popstate", updatePath);
    return () => window.removeEventListener("popstate", updatePath);
  }, []);

  function login(nextUser: AuthResponse): void {
    localStorage.setItem(SESSION_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
    navigate("/");
  }

  function logout(): void {
    localStorage.removeItem(SESSION_KEY);
    setUser(null);
    navigate("/login");
  }

  if (path === "/register") return <AuthPage mode="register" onLogin={login} />;
  if (!user) return <AuthPage mode="login" onLogin={login} />;
  if (path === "/employees") return <EmployeePage user={user} onBack={() => navigate("/")} onLogout={logout} />;
  return <HomePage user={user} onLogout={logout} />;
}

export default App;
