package com.example.employee.japanese;

import java.time.Instant;
import java.time.LocalDate;

public class JapaneseWordProgress {

    private Long id;

    private Long userId;

    private JapaneseWord word;

    private String masteryStatus = "NEW";

    private int reviewCount;

    private LocalDate nextReviewDate;

    private Instant lastReviewedAt;

    public JapaneseWordProgress() {
    }

    public JapaneseWordProgress(Long userId, JapaneseWord word) {
        this.userId = userId;
        this.word = word;
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

    public JapaneseWord getWord() {
        return word;
    }

    public void setWord(JapaneseWord word) {
        this.word = word;
    }

    public String getMasteryStatus() {
        return masteryStatus;
    }

    public void setMasteryStatus(String masteryStatus) {
        this.masteryStatus = masteryStatus;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}
