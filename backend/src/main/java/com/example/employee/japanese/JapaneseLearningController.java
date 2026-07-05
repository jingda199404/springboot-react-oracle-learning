package com.example.employee.japanese;

import com.example.employee.auth.AuthService;
import com.example.employee.auth.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/japanese")
public class JapaneseLearningController {

    private final JapaneseLearningService service;
    private final CurrentUser currentUser;

    public JapaneseLearningController(JapaneseLearningService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/home")
    public JapaneseHomeResponse home(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.home(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION));
    }

    @PostMapping("/external-fetch")
    public JapaneseExternalFetchResponse fetchExternal(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody JapaneseExternalFetchRequest request
    ) {
        return service.fetchExternal(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION), request);
    }

    @GetMapping("/summary")
    public JapaneseSummaryResponse summary(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.summary(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION));
    }

    @GetMapping("/words")
    public List<JapaneseWordResponse> words(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "category", required = false) String category
    ) {
        return service.words(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION), category);
    }

    @GetMapping("/words/today")
    public List<JapaneseWordResponse> todayWords(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.todayWords(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION));
    }

    @PostMapping("/words/{id}/progress")
    public JapaneseProgressResponse updateProgress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody JapaneseProgressRequest request
    ) {
        return service.updateProgress(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION), id, request);
    }

    @GetMapping("/quizzes")
    public List<JapaneseQuizQuestionResponse> quizzes(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION);
        return service.quizzes();
    }

    @PostMapping("/quizzes/{id}/answer")
    public JapaneseQuizAnswerResponse answer(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody JapaneseQuizAnswerRequest request
    ) {
        return service.answer(currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION), id, request);
    }

    @GetMapping("/articles/random")
    public JapaneseKnowledgeArticleResponse randomArticle(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        currentUser.requirePermission(userId, AuthService.JAPANESE_LEARNING_PERMISSION);
        return service.randomArticle();
    }
}
