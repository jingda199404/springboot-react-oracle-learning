package com.example.employee.japanese;

public class JapaneseKnowledgeArticle {

    private Long id;

    private String titleJa;

    private String titleZh;

    private String contentJa;

    private String contentZh;

    private String topic;

    private int readingMinutes;

    public JapaneseKnowledgeArticle() {
    }

    public JapaneseKnowledgeArticle(String titleJa, String titleZh, String contentJa, String contentZh, String topic, int readingMinutes) {
        this.titleJa = titleJa;
        this.titleZh = titleZh;
        this.contentJa = contentJa;
        this.contentZh = contentZh;
        this.topic = topic;
        this.readingMinutes = readingMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitleJa() {
        return titleJa;
    }

    public void setTitleJa(String titleJa) {
        this.titleJa = titleJa;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public void setTitleZh(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getContentJa() {
        return contentJa;
    }

    public void setContentJa(String contentJa) {
        this.contentJa = contentJa;
    }

    public String getContentZh() {
        return contentZh;
    }

    public void setContentZh(String contentZh) {
        this.contentZh = contentZh;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getReadingMinutes() {
        return readingMinutes;
    }

    public void setReadingMinutes(int readingMinutes) {
        this.readingMinutes = readingMinutes;
    }
}
