package com.example.employee.japanese;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

record JapaneseSummaryResponse(long totalWords, long dueReviews, long quizQuestions, long articles, long answeredQuestions, long correctAnswers) {
}

record JapaneseWordResponse(
        Long id,
        String word,
        String reading,
        String meaningZh,
        String partOfSpeech,
        String category,
        String level,
        String exampleJa,
        String exampleZh,
        String masteryStatus,
        Integer reviewCount,
        LocalDate nextReviewDate
) {
}

record JapaneseProgressRequest(String result) {
}

record JapaneseProgressResponse(Long wordId, String masteryStatus, int reviewCount, LocalDate nextReviewDate, Instant lastReviewedAt) {
}

record JapaneseQuizQuestionResponse(
        Long id,
        String questionType,
        String prompt,
        Map<String, String> options,
        String category,
        String level
) {
}

record JapaneseQuizAnswerRequest(String selectedOption) {
}

record JapaneseQuizAnswerResponse(Long questionId, String selectedOption, String correctOption, boolean correct, String explanation) {
}

record JapaneseKnowledgeArticleResponse(Long id, String titleJa, String titleZh, String contentJa, String contentZh, String topic, int readingMinutes) {
}

record JapaneseExternalFetchRequest(String level, String topic, String keyword, Integer wordCount, Integer quizCount, Boolean includeArticle) {
}

record JapaneseExternalFetchResponse(
        int importedWords,
        int createdQuizzes,
        boolean createdArticle,
        List<JapaneseWordResponse> words,
        List<JapaneseQuizQuestionResponse> quizzes,
        JapaneseKnowledgeArticleResponse article
) {
}

record JapaneseHomeResponse(
        JapaneseSummaryResponse summary,
        List<JapaneseWordResponse> todayWords,
        List<JapaneseQuizQuestionResponse> quizPreview,
        JapaneseKnowledgeArticleResponse randomArticle
) {
}
