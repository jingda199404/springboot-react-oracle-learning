package com.example.employee.japanese;

import com.example.employee.error.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@Transactional(readOnly = true)
public class JapaneseLearningService {

    private static final int TODAY_WORD_LIMIT = 6;
    private static final int QUIZ_LIMIT = 8;

    private final JapaneseWordRepository wordRepository;
    private final JapaneseWordProgressRepository progressRepository;
    private final JapaneseQuizQuestionRepository quizQuestionRepository;
    private final JapaneseQuizResultRepository quizResultRepository;
    private final JapaneseKnowledgeArticleRepository articleRepository;
    private final Random random = new Random();
    private final RestClient restClient = RestClient.create();

    public JapaneseLearningService(
            JapaneseWordRepository wordRepository,
            JapaneseWordProgressRepository progressRepository,
            JapaneseQuizQuestionRepository quizQuestionRepository,
            JapaneseQuizResultRepository quizResultRepository,
            JapaneseKnowledgeArticleRepository articleRepository
    ) {
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizResultRepository = quizResultRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public JapaneseHomeResponse home(Long userId) {
        return new JapaneseHomeResponse(summary(userId), todayWords(userId), quizzes(), randomArticle());
    }

    @Transactional
    public JapaneseSummaryResponse summary(Long userId) {
        return new JapaneseSummaryResponse(
                wordRepository.count(),
                progressRepository.countByUserIdAndNextReviewDateLessThanEqual(userId, LocalDate.now()),
                quizQuestionRepository.count(),
                articleRepository.count(),
                quizResultRepository.countByUserId(userId),
                quizResultRepository.countByUserIdAndCorrectTrue(userId)
        );
    }

    @Transactional
    public List<JapaneseWordResponse> words(Long userId, String category) {
        List<JapaneseWord> words = category == null || category.isBlank()
                ? wordRepository.findAll()
                : wordRepository.findByCategory(category);
        return words.stream().map(word -> wordResponse(userId, word)).toList();
    }

    @Transactional
    public List<JapaneseWordResponse> todayWords(Long userId) {
        List<JapaneseWordProgress> due = progressRepository.findByUserIdAndNextReviewDateLessThanEqualOrderByNextReviewDateAscIdAsc(userId, LocalDate.now());
        if (!due.isEmpty()) {
            return due.stream().limit(TODAY_WORD_LIMIT).map(progress -> wordResponse(progress.getWord(), progress)).toList();
        }
        List<JapaneseWord> words = new ArrayList<>(wordRepository.findAllById());
        Collections.shuffle(words, random);
        return words.stream().limit(TODAY_WORD_LIMIT).map(word -> wordResponse(userId, word)).toList();
    }

    @Transactional
    public JapaneseProgressResponse updateProgress(Long userId, Long wordId, JapaneseProgressRequest request) {
        JapaneseWord word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("単語が見つかりません：" + wordId));
        JapaneseWordProgress progress = progressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> new JapaneseWordProgress(userId, word));
        String result = normalizeResult(request.result());
        progress.setMasteryStatus(result);
        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewedAt(Instant.now());
        progress.setNextReviewDate(nextReviewDate(result, progress.getReviewCount()));
        progress = progressRepository.save(progress);
        return new JapaneseProgressResponse(wordId, progress.getMasteryStatus(), progress.getReviewCount(), progress.getNextReviewDate(), progress.getLastReviewedAt());
    }

    public List<JapaneseQuizQuestionResponse> quizzes() {
        List<JapaneseQuizQuestion> questions = new ArrayList<>(quizQuestionRepository.findAll());
        Collections.shuffle(questions, random);
        return questions.stream().limit(QUIZ_LIMIT).map(this::quizResponse).toList();
    }

    @Transactional
    public JapaneseQuizAnswerResponse answer(Long userId, Long questionId, JapaneseQuizAnswerRequest request) {
        JapaneseQuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("問題が見つかりません：" + questionId));
        String selectedOption = normalizeOption(request.selectedOption());
        boolean correct = question.getCorrectOption().equals(selectedOption);
        quizResultRepository.save(new JapaneseQuizResult(userId, question, selectedOption, correct));
        return new JapaneseQuizAnswerResponse(question.getId(), selectedOption, question.getCorrectOption(), correct, question.getExplanation());
    }

    public JapaneseKnowledgeArticleResponse randomArticle() {
        List<JapaneseKnowledgeArticle> articles = articleRepository.findAll();
        if (articles.isEmpty()) {
            return null;
        }
        return articleResponse(articles.get(random.nextInt(articles.size())));
    }

    @Transactional
    public JapaneseExternalFetchResponse fetchExternal(Long userId, JapaneseExternalFetchRequest request) {
        int wordCount = clamp(request.wordCount(), 3, 20, 8);
        int quizCount = clamp(request.quizCount(), 0, 20, 5);
        String level = textOrDefault(request.level(), "N3").toUpperCase();
        String topic = textOrDefault(request.topic(), "会話");
        List<JapaneseWord> imported = importWords(request.keyword(), topic, level, wordCount);
        List<JapaneseQuizQuestion> createdQuizzes = createQuizzes(imported, quizCount);
        JapaneseKnowledgeArticle article = Boolean.FALSE.equals(request.includeArticle()) ? null : createArticle(imported, topic);

        return new JapaneseExternalFetchResponse(
                imported.size(),
                createdQuizzes.size(),
                article != null,
                imported.stream().map(word -> wordResponse(userId, word)).toList(),
                createdQuizzes.stream().map(this::quizResponse).toList(),
                article == null ? null : articleResponse(article)
        );
    }

    @Transactional
    public void deleteUserData(Long userId) {
        progressRepository.deleteByUserId(userId);
        quizResultRepository.deleteByUserId(userId);
    }

    private JapaneseWordResponse wordResponse(Long userId, JapaneseWord word) {
        return wordResponse(word, progressRepository.findByUserIdAndWordId(userId, word.getId()).orElse(null));
    }

    private JapaneseWordResponse wordResponse(JapaneseWord word, JapaneseWordProgress progress) {
        return new JapaneseWordResponse(
                word.getId(),
                word.getWord(),
                word.getReading(),
                word.getMeaningZh(),
                word.getPartOfSpeech(),
                word.getCategory(),
                word.getLevel(),
                word.getExampleJa(),
                word.getExampleZh(),
                progress == null ? "NEW" : progress.getMasteryStatus(),
                progress == null ? 0 : progress.getReviewCount(),
                progress == null ? null : progress.getNextReviewDate()
        );
    }

    private JapaneseQuizQuestionResponse quizResponse(JapaneseQuizQuestion question) {
        return new JapaneseQuizQuestionResponse(
                question.getId(),
                question.getQuestionType(),
                question.getPrompt(),
                Map.of(
                        "A", question.getOptionA(),
                        "B", question.getOptionB(),
                        "C", question.getOptionC(),
                        "D", question.getOptionD()
                ),
                question.getCategory(),
                question.getLevel()
        );
    }

    private JapaneseKnowledgeArticleResponse articleResponse(JapaneseKnowledgeArticle article) {
        return new JapaneseKnowledgeArticleResponse(
                article.getId(),
                article.getTitleJa(),
                article.getTitleZh(),
                article.getContentJa(),
                article.getContentZh(),
                article.getTopic(),
                article.getReadingMinutes()
        );
    }

    private String normalizeResult(String result) {
        String value = result == null ? "" : result.trim().toUpperCase();
        if (!List.of("KNOWN", "UNSURE", "UNKNOWN").contains(value)) {
            throw new IllegalArgumentException("単語の結果は KNOWN, UNSURE, UNKNOWN のいずれかを指定してください");
        }
        return value;
    }

    private String normalizeOption(String selectedOption) {
        String value = selectedOption == null ? "" : selectedOption.trim().toUpperCase();
        if (!List.of("A", "B", "C", "D").contains(value)) {
            throw new IllegalArgumentException("回答は A, B, C, D のいずれかを指定してください");
        }
        return value;
    }

    private LocalDate nextReviewDate(String result, int reviewCount) {
        LocalDate today = LocalDate.now();
        return switch (result) {
            case "KNOWN" -> today.plusDays(Math.min(30, Math.max(2, reviewCount * 3L)));
            case "UNSURE" -> today.plusDays(1);
            default -> today;
        };
    }

    private List<JapaneseWord> importWords(String keyword, String topic, String level, int wordCount) {
        List<JapaneseWord> imported = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String query : queries(keyword, topic, level)) {
            JsonNode data = fetchJisho(query).path("data");
            if (!data.isArray()) continue;
            for (JsonNode entry : data) {
                if (imported.size() >= wordCount) return imported;
                JsonNode japanese = entry.path("japanese").isArray() && !entry.path("japanese").isEmpty() ? entry.path("japanese").get(0) : null;
                JsonNode sense = entry.path("senses").isArray() && !entry.path("senses").isEmpty() ? entry.path("senses").get(0) : null;
                if (japanese == null || sense == null) continue;
                String word = textOrDefault(japanese.path("word").asText(null), japanese.path("reading").asText(""));
                String reading = textOrDefault(japanese.path("reading").asText(null), word);
                String meaning = joinArray(sense.path("english_definitions"), " / ");
                if (word.isBlank() || meaning.isBlank() || !seen.add(word + "|" + reading)) continue;
                String partOfSpeech = textOrDefault(joinArray(sense.path("parts_of_speech"), ", "), "Unknown");
                JapaneseExample example = fetchExample(word);
                JapaneseWord saved = wordRepository.findFirstByWordAndReading(word, reading)
                        .orElseGet(() -> wordRepository.save(new JapaneseWord(
                                word,
                                reading,
                                meaning,
                                partOfSpeech,
                                topic,
                                level,
                                textOrDefault(example.japanese(), word),
                                textOrDefault(example.translation(), meaning)
                        )));
                imported.add(saved);
            }
        }
        return imported;
    }

    private List<JapaneseQuizQuestion> createQuizzes(List<JapaneseWord> words, int quizCount) {
        if (words.size() < 4 || quizCount <= 0) return List.of();
        List<JapaneseQuizQuestion> created = new ArrayList<>();
        List<JapaneseWord> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled, random);
        for (JapaneseWord target : shuffled) {
            if (created.size() >= quizCount) break;
            List<String> options = new ArrayList<>(words.stream()
                    .filter(word -> !word.getId().equals(target.getId()))
                    .map(JapaneseWord::getMeaningZh)
                    .distinct()
                    .limit(3)
                    .toList());
            if (options.size() < 3) continue;
            options.add(target.getMeaningZh());
            Collections.shuffle(options, random);
            int correctIndex = options.indexOf(target.getMeaningZh());
            String correctOption = String.valueOf((char) ('A' + correctIndex));
            created.add(quizQuestionRepository.save(new JapaneseQuizQuestion(
                    "JA_TO_MEANING",
                    "「" + target.getWord() + "」の意味として最も近いものはどれですか？",
                    options.get(0),
                    options.get(1),
                    options.get(2),
                    options.get(3),
                    correctOption,
                    "Jisho/JMdict 系データから取得した語義です。読み方：" + target.getReading(),
                    target.getCategory(),
                    target.getLevel()
            )));
        }
        return created;
    }

    private JapaneseKnowledgeArticle createArticle(List<JapaneseWord> words, String topic) {
        List<JapaneseWord> examples = words.stream()
                .filter(word -> word.getExampleJa() != null && !word.getExampleJa().isBlank())
                .limit(8)
                .toList();
        if (examples.isEmpty()) return null;
        StringBuilder ja = new StringBuilder();
        StringBuilder zh = new StringBuilder();
        int index = 1;
        for (JapaneseWord word : examples) {
            ja.append(index).append(". ").append(word.getExampleJa())
                    .append("\n   語彙: ").append(word.getWord()).append("（").append(word.getReading()).append("）")
                    .append("\n\n");
            zh.append(index).append(". ").append(word.getExampleZh())
                    .append("\n   词汇: ").append(word.getWord()).append(" / ").append(word.getMeaningZh())
                    .append("\n\n");
            index++;
        }
        return articleRepository.save(new JapaneseKnowledgeArticle(
                "外部例文で読む「" + topic + "」",
                "用外部例句阅读「" + topic + "」",
                ja.toString().trim(),
                zh.toString().trim(),
                topic,
                Math.max(3, Math.min(5, examples.size()))
        ));
    }

    private JsonNode fetchJisho(String keyword) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("jisho.org")
                        .path("/api/v1/search/words")
                        .queryParam("keyword", keyword)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private JapaneseExample fetchExample(String word) {
        try {
            JsonNode data = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.tatoeba.org")
                            .path("/v1/sentences")
                            .queryParam("lang", "jpn")
                            .queryParam("q", word)
                            .queryParam("trans:lang", "cmn")
                            .queryParam("sort", "relevance")
                            .queryParam("limit", 1)
                            .queryParam("showtrans", "matching")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode first = data == null ? null : data.path("data").path(0);
            if (first == null || first.isMissingNode()) {
                return new JapaneseExample("", "");
            }
            String japanese = first.path("text").asText("");
            JsonNode translation = first.path("translations").isArray() && !first.path("translations").isEmpty()
                    ? first.path("translations").get(0)
                    : null;
            return new JapaneseExample(japanese, translation == null ? "" : translation.path("text").asText(""));
        } catch (RuntimeException exception) {
            return new JapaneseExample("", "");
        }
    }

    private List<String> queries(String keyword, String topic, String level) {
        List<String> queries = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            queries.add(keyword.trim());
        }
        queries.add("#jlpt-" + level.toLowerCase());
        queries.addAll(topicKeywords(topic));
        return queries.stream().filter(query -> query != null && !query.isBlank()).distinct().toList();
    }

    private List<String> topicKeywords(String topic) {
        return switch (topic) {
            case "仕事" -> List.of("仕事", "確認", "予定", "会議");
            case "IT" -> List.of("開発", "設定", "画面", "保存");
            case "家計簿" -> List.of("お金", "銀行", "支払い", "残高");
            case "生活" -> List.of("生活", "買い物", "料理", "掃除");
            default -> List.of("会話", "お願い", "質問", "説明");
        };
    }

    private String joinArray(JsonNode node, String delimiter) {
        if (node == null || !node.isArray()) return "";
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.asText("").isBlank()) values.add(item.asText());
        });
        return String.join(delimiter, values);
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) return defaultValue;
        return Math.max(min, Math.min(max, value));
    }

    private record JapaneseExample(String japanese, String translation) {
    }
}
