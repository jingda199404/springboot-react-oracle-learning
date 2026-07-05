package com.example.employee.japanese;

import java.time.Instant;

public class JapaneseQuizResult {

    private Long id;

    private Long userId;

    private JapaneseQuizQuestion question;

    private String selectedOption;

    private boolean correct;

    private Instant answeredAt;

    public JapaneseQuizResult() {
    }

    public JapaneseQuizResult(Long userId, JapaneseQuizQuestion question, String selectedOption, boolean correct) {
        this.userId = userId;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.answeredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public JapaneseQuizQuestion getQuestion() {
        return question;
    }

    public void setQuestion(JapaneseQuizQuestion question) {
        this.question = question;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
