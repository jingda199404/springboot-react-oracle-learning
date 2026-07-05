import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { japaneseApi } from "./api";
import type { AuthResponse, JapaneseExternalFetchRequest, JapaneseHome, JapaneseKnowledgeArticle, JapaneseQuizAnswerResponse, JapaneseQuizQuestion, JapaneseWord } from "./types";
import { errorMessage } from "./utils";

export type JapaneseLearningView = "home" | "words" | "memory" | "quiz" | "articles";

interface JapaneseLearningPageProps {
  user: AuthResponse;
  view: JapaneseLearningView;
  onBack: () => void;
  onLogout: () => void;
  onNavigate: (path: string) => void;
}

const statusLabels: Record<JapaneseWord["masteryStatus"], string> = {
  NEW: "未学習",
  KNOWN: "覚えた",
  UNSURE: "迷った",
  UNKNOWN: "もう一度",
};

export default function JapaneseLearningPage({ user, view, onBack, onLogout, onNavigate }: JapaneseLearningPageProps) {
  const [home, setHome] = useState<JapaneseHome | null>(null);
  const [words, setWords] = useState<JapaneseWord[]>([]);
  const [quizzes, setQuizzes] = useState<JapaneseQuizQuestion[]>([]);
  const [article, setArticle] = useState<JapaneseKnowledgeArticle | null>(null);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [fetchForm, setFetchForm] = useState<JapaneseExternalFetchRequest>({
    level: "N3",
    topic: "会話",
    keyword: "",
    wordCount: 8,
    quizCount: 5,
    includeArticle: true,
  });
  const [fetchNotice, setFetchNotice] = useState("");
  const [memoryIndex, setMemoryIndex] = useState(0);
  const [showAnswer, setShowAnswer] = useState(false);
  const [quizIndex, setQuizIndex] = useState(0);
  const [quizAnswer, setQuizAnswer] = useState<JapaneseQuizAnswerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    void load();
  }, [view, selectedCategory]);

  async function load(): Promise<void> {
    setLoading(true);
    setError("");
    try {
      if (view === "home") {
        setHome(await japaneseApi.home());
      } else if (view === "words") {
        setWords(await japaneseApi.words(selectedCategory));
      } else if (view === "memory") {
        setWords(await japaneseApi.todayWords());
        setMemoryIndex(0);
        setShowAnswer(false);
      } else if (view === "quiz") {
        setQuizzes(await japaneseApi.quizzes());
        setQuizIndex(0);
        setQuizAnswer(null);
      } else {
        setArticle(await japaneseApi.randomArticle());
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  function updateFetchField(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>): void {
    const { name, value } = event.target;
    setFetchForm((current) => ({
      ...current,
      [name]: name === "wordCount" || name === "quizCount" ? Number(value) : value,
    }));
  }

  async function fetchExternal(): Promise<void> {
    setBusy(true);
    setError("");
    setFetchNotice("");
    try {
      const result = await japaneseApi.fetchExternal(fetchForm);
      setFetchNotice(`外部から ${result.importedWords} 語、${result.createdQuizzes} 問を取得しました。${result.createdArticle ? "読書用の例文セットも作成しました。" : ""}`);
      setHome(await japaneseApi.home());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  const categories = useMemo(() => Array.from(new Set(words.map((word) => word.category))).sort(), [words]);
  const currentMemoryWord = words[memoryIndex];
  const currentQuiz = quizzes[quizIndex];
  const quizAccuracy = home?.summary.answeredQuestions ? Math.round((home.summary.correctAnswers / home.summary.answeredQuestions) * 100) : 0;

  async function remember(result: "KNOWN" | "UNSURE" | "UNKNOWN"): Promise<void> {
    if (!currentMemoryWord) return;
    setBusy(true);
    setError("");
    try {
      await japaneseApi.updateProgress(currentMemoryWord.id, result);
      setShowAnswer(false);
      setMemoryIndex(0);
      setWords((current) => current.filter((word) => word.id !== currentMemoryWord.id));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function answer(option: string): Promise<void> {
    if (!currentQuiz) return;
    setBusy(true);
    setError("");
    try {
      setQuizAnswer(await japaneseApi.answerQuiz(currentQuiz.id, option));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  function nextQuiz(): void {
    setQuizAnswer(null);
    setQuizIndex((current) => current + 1 >= quizzes.length ? 0 : current + 1);
  }

  async function refreshArticle(): Promise<void> {
    setBusy(true);
    setError("");
    try {
      setArticle(await japaneseApi.randomArticle());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <button className="text-button back-button" onClick={view === "home" ? onBack : () => onNavigate("/japanese")}>← {view === "home" ? "ホームへ戻る" : "日本語学習へ戻る"}</button>
        <div className="user-menu"><span>{user.username}</span><button className="text-button" onClick={onLogout}>ログアウト</button></div>
      </header>
      <header className="hero japanese-hero">
        <div>
          <span className="eyebrow">JINGDA JAPANESE</span>
          <h1>{view === "home" ? "日本語学習" : view === "words" ? "単語用法" : view === "memory" ? "単語暗記" : view === "quiz" ? "問題練習" : "知識点読書"}</h1>
          <p>{view === "articles" ? "入るたびに、5分ほどで読める中日対照の知識点をランダム表示します。" : "毎日少しずつ、単語・例文・暗記・問題練習を積み上げます。"}</p>
        </div>
        <div className="summary">
          <span>今日の学習</span>
          <strong>{home?.summary.dueReviews ?? words.length ?? 0}</strong>
          <small>review items</small>
        </div>
      </header>

      {error && <div className="alert">{error}</div>}
      {fetchNotice && <div className="success">{fetchNotice}</div>}
      {loading ? <section className="panel"><div className="empty">日本語学習データを読み込み中...</div></section> : (
        <>
          {view === "home" && home && (
            <>
              <section className="panel external-fetch-panel">
                <div className="section-heading">
                  <div>
                    <span className="section-number">EXTERNAL SOURCE</span>
                    <h2>外部から学習内容を取得</h2>
                  </div>
                </div>
                <p className="muted-text">Jisho/JMdict 系データから単語を取得し、Tatoeba から例文と翻訳を取得します。取得した内容だけを DB にキャッシュします。</p>
                <div className="external-fetch-form">
                  <label>レベル
                    <select name="level" value={fetchForm.level} onChange={updateFetchField}>
                      {["N5", "N4", "N3", "N2", "N1"].map((level) => <option key={level} value={level}>{level}</option>)}
                    </select>
                  </label>
                  <label>テーマ
                    <select name="topic" value={fetchForm.topic} onChange={updateFetchField}>
                      {["会話", "生活", "仕事", "IT", "家計簿"].map((topic) => <option key={topic} value={topic}>{topic}</option>)}
                    </select>
                  </label>
                  <label>検索語（任意）
                    <input name="keyword" value={fetchForm.keyword} onChange={updateFetchField} placeholder="例：確認、銀行、開発" />
                  </label>
                  <label>単語数
                    <input name="wordCount" type="number" min="3" max="20" value={fetchForm.wordCount} onChange={updateFetchField} />
                  </label>
                  <label>問題数
                    <input name="quizCount" type="number" min="0" max="20" value={fetchForm.quizCount} onChange={updateFetchField} />
                  </label>
                  <button className="primary-button" disabled={busy} onClick={() => void fetchExternal()}>{busy ? "取得中..." : "取得する"}</button>
                </div>
              </section>
              <section className="japanese-stats">
                <article><span>単語</span><strong>{home.summary.totalWords}</strong></article>
                <article><span>復習対象</span><strong>{home.summary.dueReviews}</strong></article>
                <article><span>問題</span><strong>{home.summary.quizQuestions}</strong></article>
                <article><span>正答率</span><strong>{quizAccuracy}%</strong></article>
              </section>
              <section className="module-grid">
                <article className="module-card japanese-card"><span className="module-tag">WORDS</span><h2>単語用法</h2><p>単語、読み方、中国語意味、例文を一覧で確認します。</p><button className="primary-button" onClick={() => onNavigate("/japanese/words")}>単語を見る</button></article>
                <article className="module-card japanese-card"><span className="module-tag">MEMORY</span><h2>単語暗記</h2><p>カードをめくって、覚えた・迷った・もう一度で復習間隔を作ります。</p><button className="primary-button" onClick={() => onNavigate("/japanese/memory")}>暗記を始める</button></article>
                <article className="module-card japanese-card"><span className="module-tag">QUIZ</span><h2>問題練習</h2><p>中日対応、意味理解、自然な使い方を選択問題で確認します。</p><button className="primary-button" onClick={() => onNavigate("/japanese/quiz")}>問題を解く</button></article>
                <article className="module-card japanese-card"><span className="module-tag">5 MIN READ</span><h2>知識点読書</h2><p>毎回ランダムで、中日対照の小文章を表示します。</p><button className="primary-button" onClick={() => onNavigate("/japanese/articles")}>知識点を読む</button></article>
              </section>
              <section className="panel">
                <div className="section-heading"><div><span className="section-number">TODAY</span><h2>今日のおすすめ単語</h2></div></div>
                {home.todayWords.length === 0 ? <div className="empty">まだ単語がありません。上の「外部から学習内容を取得」から取得してください。</div> : <div className="word-grid">{home.todayWords.map((word) => <WordCard key={word.id} word={word} />)}</div>}
              </section>
            </>
          )}

          {view === "words" && (
            <section className="panel">
              <div className="section-heading employee-list-heading">
                <div><span className="section-number">WORD USAGE</span><h2>単語と用法</h2><span className="record-count">{words.length} 件</span></div>
                <label className="compact-select">カテゴリ<select value={selectedCategory} onChange={(event) => setSelectedCategory(event.target.value)}><option value="">すべて</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}</select></label>
              </div>
              <div className="word-grid">{words.map((word) => <WordCard key={word.id} word={word} />)}</div>
            </section>
          )}

          {view === "memory" && (
            <section className="panel memory-panel">
              {!currentMemoryWord ? <div className="empty">今日の暗記対象は終わりました。</div> : (
                <>
                  <div className="section-heading"><div><span className="section-number">FLASH CARD</span><h2>{memoryIndex + 1} / {words.length}</h2></div></div>
                  <article className={`memory-card ${showAnswer ? "revealed" : ""}`} onClick={() => setShowAnswer(true)}>
                    <span>{currentMemoryWord.level} · {currentMemoryWord.category}</span>
                    <h2>{currentMemoryWord.word}</h2>
                    <p>{showAnswer ? `${currentMemoryWord.reading} · ${currentMemoryWord.meaningZh}` : "クリックして答えを見る"}</p>
                    {showAnswer && <small>{currentMemoryWord.exampleJa}<br />{currentMemoryWord.exampleZh}</small>}
                  </article>
                  <div className="memory-actions">
                    <button className="refresh-button" disabled={busy} onClick={() => void remember("UNKNOWN")}>覚えていない</button>
                    <button className="refresh-button" disabled={busy} onClick={() => void remember("UNSURE")}>迷った</button>
                    <button className="primary-button" disabled={busy} onClick={() => void remember("KNOWN")}>覚えた</button>
                  </div>
                </>
              )}
            </section>
          )}

          {view === "quiz" && (
            <section className="panel">
              {!currentQuiz ? <div className="empty">問題がまだ登録されていません。</div> : (
                <>
                  <div className="section-heading"><div><span className="section-number">QUESTION</span><h2>{quizIndex + 1} / {quizzes.length}</h2></div><button className="refresh-button" onClick={() => void load()}>問題を更新</button></div>
                  <article className="quiz-card">
                    <span>{currentQuiz.level} · {currentQuiz.category}</span>
                    <h2>{currentQuiz.prompt}</h2>
                    <div className="quiz-options">{(["A", "B", "C", "D"] as const).map((option) => (
                      <button key={option} disabled={busy || !!quizAnswer} onClick={() => void answer(option)} className={quizAnswer?.correctOption === option ? "correct" : quizAnswer?.selectedOption === option ? "wrong" : ""}>
                        <strong>{option}</strong>{currentQuiz.options[option]}
                      </button>
                    ))}</div>
                    {quizAnswer && <div className={quizAnswer.correct ? "success" : "alert"}>{quizAnswer.correct ? "正解です。" : `不正解です。正解は ${quizAnswer.correctOption} です。`} {quizAnswer.explanation}</div>}
                    {quizAnswer && <button className="primary-button" onClick={nextQuiz}>次の問題へ</button>}
                  </article>
                </>
              )}
            </section>
          )}

          {view === "articles" && article && (
            <section className="panel article-panel">
              <div className="section-heading employee-list-heading">
                <div><span className="section-number">{article.topic} · {article.readingMinutes} MIN</span><h2>{article.titleJa}</h2></div>
                <button className="refresh-button" disabled={busy} onClick={() => void refreshArticle()}>別の知識点を読む</button>
              </div>
              <h3>{article.titleZh}</h3>
              <div className="article-columns">
                <article><span className="section-number">日本語</span>{article.contentJa.split("\n\n").map((paragraph) => <p key={paragraph}>{paragraph}</p>)}</article>
                <article><span className="section-number">中文</span>{article.contentZh.split("\n\n").map((paragraph) => <p key={paragraph}>{paragraph}</p>)}</article>
              </div>
            </section>
          )}
        </>
      )}
    </main>
  );
}

function WordCard({ word }: { word: JapaneseWord }) {
  return (
    <article className="word-card">
      <div><span className="module-tag">{word.level} · {word.category}</span><strong className="word-main">{word.word}</strong><small>{word.reading}</small></div>
      <p>{word.meaningZh}</p>
      <small>{word.exampleJa}<br />{word.exampleZh}</small>
      <span className={`study-status ${word.masteryStatus.toLowerCase()}`}>{statusLabels[word.masteryStatus]}</span>
    </article>
  );
}
