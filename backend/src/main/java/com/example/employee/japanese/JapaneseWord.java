package com.example.employee.japanese;

public class JapaneseWord {

    private Long id;

    private String word;

    private String reading;

    private String meaningZh;

    private String partOfSpeech;

    private String category;

    private String level;

    private String exampleJa;

    private String exampleZh;

    public JapaneseWord() {
    }

    public JapaneseWord(String word, String reading, String meaningZh, String partOfSpeech, String category, String level, String exampleJa, String exampleZh) {
        this.word = word;
        this.reading = reading;
        this.meaningZh = meaningZh;
        this.partOfSpeech = partOfSpeech;
        this.category = category;
        this.level = level;
        this.exampleJa = exampleJa;
        this.exampleZh = exampleZh;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public String getMeaningZh() {
        return meaningZh;
    }

    public void setMeaningZh(String meaningZh) {
        this.meaningZh = meaningZh;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getExampleJa() {
        return exampleJa;
    }

    public void setExampleJa(String exampleJa) {
        this.exampleJa = exampleJa;
    }

    public String getExampleZh() {
        return exampleZh;
    }

    public void setExampleZh(String exampleZh) {
        this.exampleZh = exampleZh;
    }
}
